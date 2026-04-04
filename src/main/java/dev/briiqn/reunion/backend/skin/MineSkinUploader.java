package dev.briiqn.reunion.backend.skin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class MineSkinUploader {

  private static final String API_URL = "https://api.mineskin.org/generate/upload";
  private static final String BOUNDARY = "----ReunionSkinBoundary";

  private MineSkinUploader() {}

  public static String[] upload(byte[] skinBytes) throws Exception {
    URL url = new URL(API_URL);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    conn.setRequestMethod("POST");
    conn.setDoOutput(true);
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
    conn.setRequestProperty("User-Agent", "ReunionBackend/1.0");
    conn.setConnectTimeout(15_000);
    conn.setReadTimeout(30_000);

    try (OutputStream out = conn.getOutputStream()) {
      writePart(out, "name", "Reunion Skin");
      writePart(out, "visibility", "1");
      writeFilePart(out, skinBytes);
      out.write(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
    }

    int status = conn.getResponseCode();
    String body = readStream(status < 400 ? conn.getInputStream() : conn.getErrorStream());

    if (status != 200) {
      throw new IOException("MineSkin API returned " + status + ": " + body);
    }

    String value = extractJson(body, "value");
    String signature = extractJson(body, "signature");

    if (value == null || signature == null) {
      throw new IOException("Missing texture data in response: " + body);
    }

    return new String[]{value, signature};
  }

  private static void writePart(OutputStream out, String name, String value) throws IOException {
    String header = "--" + BOUNDARY + "\r\n"
        + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
        + value + "\r\n";
    out.write(header.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeFilePart(OutputStream out, byte[] skinBytes) throws IOException {
    String header = "--" + BOUNDARY + "\r\n"
        + "Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n"
        + "Content-Type: image/png\r\n\r\n";
    out.write(header.getBytes(StandardCharsets.UTF_8));
    out.write(skinBytes);
    out.write("\r\n".getBytes(StandardCharsets.UTF_8));
  }

  private static String readStream(InputStream in) throws IOException {
    if (in == null) return "";
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) sb.append(line);
      return sb.toString();
    }
  }

  private static String extractJson(String json, String key) {
    String search = "\"" + key + "\":\"";
    int start = json.indexOf(search);
    if (start == -1) return null;
    start += search.length();
    int end = start;
    while (end < json.length()) {
      if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
      end++;
    }
    return json.substring(start, end).replace("\\\"", "\"");
  }
}