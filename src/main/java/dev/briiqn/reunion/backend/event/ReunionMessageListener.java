package dev.briiqn.reunion.backend.event;

import dev.briiqn.reunion.backend.ReunionBackendPlugin;
import dev.briiqn.reunion.backend.skin.MineSkinUploader;
import dev.briiqn.reunion.backend.skin.SkinApplier;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class ReunionMessageListener implements PluginMessageListener {

  private final ReunionBackendPlugin plugin;
  private final Map<String, String[]> skinCache = new ConcurrentHashMap<>();

  public ReunionMessageListener(ReunionBackendPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void onPluginMessageReceived(String channel, Player player, byte[] message) {
    if (!"reunion:".equals(channel)) return;

    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
      String playerName = readString(in);
      long xuid = in.readLong();

      int skinLen = in.readInt();
      byte[] skinBytes = null;
      if (skinLen > 0) {
        skinBytes = new byte[skinLen];
        in.readFully(skinBytes);
      }

      int capeLen = in.readInt();
      if (capeLen > 0) in.skipBytes(capeLen);

      if (skinBytes != null && skinBytes.length > 0) {
        processSkin(player, skinBytes);
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to parse reunion message for " + player.getName() + ": " + e.getMessage());
    }
  }

  private void processSkin(Player player, byte[] skinBytes) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String hash = Base64.getEncoder().encodeToString(digest.digest(skinBytes));

        String[] textures = skinCache.computeIfAbsent(hash, k -> {
          try {
            return MineSkinUploader.upload(skinBytes);
          } catch (Exception e) {
            plugin.getLogger().warning("MineSkin upload failed for " + player.getName() + ": " + e.getMessage());
            return null;
          }
        });

        if (textures != null) {
          String value = textures[0];
          String signature = textures[1];
          Bukkit.getScheduler().runTask(plugin, () -> SkinApplier.applySkin(plugin, player, value, signature));
        }
      } catch (Exception e) {
        plugin.getLogger().warning("Failed to process skin for " + player.getName() + ": " + e.getMessage());
      }
    });
  }

  private int readVarInt(DataInputStream in) throws IOException {
    int numRead = 0;
    int result = 0;
    byte read;
    do {
      read = in.readByte();
      result |= ((read & 0x7F) << (7 * numRead));
      if (++numRead > 5) throw new RuntimeException("VarInt too big");
    } while ((read & 0x80) != 0);
    return result;
  }

  private String readString(DataInputStream in) throws IOException {
    byte[] bytes = new byte[readVarInt(in)];
    in.readFully(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}