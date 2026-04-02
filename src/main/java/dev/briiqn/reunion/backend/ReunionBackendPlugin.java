package dev.briiqn.reunion.backend;

import dev.briiqn.reunion.backend.event.ReunionMessageListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineskin.MineSkinClient;
import org.mineskin.JsoupRequestHandler;

public final class ReunionBackendPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    MineSkinClient skinClient = MineSkinClient.builder()
        .requestHandler(JsoupRequestHandler::new)
        .userAgent("ReunionBackend/1.0")
        .build();

    getServer().getMessenger().registerIncomingPluginChannel(this, "reunion:", new ReunionMessageListener(this,
        skinClient));
  }

  @Override
  public void onDisable() {
    getServer().getMessenger().unregisterIncomingPluginChannel(this);
  }
}