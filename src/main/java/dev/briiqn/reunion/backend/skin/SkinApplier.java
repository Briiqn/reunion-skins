package dev.briiqn.reunion.backend.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SkinApplier {

  private SkinApplier() {}

  public static void applySkin(Plugin plugin, Player player, String value, String signature) {
    try {
      Object serverPlayer = player.getClass().getMethod("getHandle").invoke(player);
      GameProfile profile;
      try {
        Method getProfile = serverPlayer.getClass().getMethod("getGameProfile");
        profile = (GameProfile) getProfile.invoke(serverPlayer);
      } catch (NoSuchMethodException e) {
        Method getProfile = serverPlayer.getClass().getMethod("getProfile");
        profile = (GameProfile) getProfile.invoke(serverPlayer);
      }

      profile.getProperties().removeAll("textures");
      profile.getProperties().put("textures", new Property("textures", value, signature));

      for (Player target : Bukkit.getOnlinePlayers()) {
        if (!target.equals(player) && target.canSee(player)) {
          target.hidePlayer(plugin, player);
          target.showPlayer(plugin, player);
        }
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to apply skin to " + player.getName() + ": " + e.getMessage());
    }
  }
}