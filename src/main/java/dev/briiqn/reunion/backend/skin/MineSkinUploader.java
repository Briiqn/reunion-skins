package dev.briiqn.reunion.backend.skin;

import java.io.File;
import java.nio.file.Files;
import org.mineskin.MineSkinClient;
import org.mineskin.data.Visibility;
import org.mineskin.request.GenerateRequest;

public final class MineSkinUploader {

  private MineSkinUploader() {}

  public static String[] upload(MineSkinClient client, byte[] skinBytes) throws Exception {
    File tmp = File.createTempFile("reunion_skin_", ".png");
    try {
      Files.write(tmp.toPath(), skinBytes);

      GenerateRequest request = GenerateRequest.upload(tmp)
          .name("Reunion Skin")
          .visibility(Visibility.UNLISTED);

      String[] textures = new String[2];

      client.queue().submit(request)
          .thenCompose(q -> q.getJob().waitForCompletion(client))
          .thenCompose(j -> j.getOrLoadSkin(client))
          .thenAccept(skin -> {
            textures[0] = skin.texture().data().value();
            textures[1] = skin.texture().data().signature();
          })
          .get();

      return textures[0] != null ? textures : null;
    } finally {
      tmp.delete();
    }
  }
}