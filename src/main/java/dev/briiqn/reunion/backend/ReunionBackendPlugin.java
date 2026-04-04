package dev.briiqn.reunion.backend;

import dev.briiqn.reunion.backend.event.ReunionMessageListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReunionBackendPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    getServer().getMessenger().registerIncomingPluginChannel(this, "reunion:", new ReunionMessageListener(this));
  }

  @Override
  public void onDisable() {
    getServer().getMessenger().unregisterIncomingPluginChannel(this);
  }
}