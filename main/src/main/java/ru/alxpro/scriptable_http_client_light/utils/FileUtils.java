package ru.alxpro.scriptable_http_client_light.utils;

import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import ru.alxpro.scriptable_http_client_light.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

  private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

  public static String getNameWithoutExt(String name) {
    int idx = name.lastIndexOf('.');
    return idx == -1 ? name : name.substring(0, idx);
  }

  public static String saveJsonFile(String parent, String name, Object obj) {
    return saveJsonFile(parent, name, obj, false);
  }

  public static String saveJsonFile(
      String parent, String name, Object obj, Boolean pretty
  ) {
    try {
      GsonBuilder builder = new GsonBuilder();
      if (pretty) {
        builder.setPrettyPrinting();
      }
      return saveFileDataDir(parent, name, builder.create().toJson(obj), ".json");
    } catch (Exception e) {
      log.error("Error to serialize Object to JSON: {}", e.getMessage());
      throw new RuntimeException("Error: " + e.getMessage(), e);
    }
  }

  public static String saveFileDataDir(
      String parent, String name, String content, String ext
  ) {
    try {
      if (!name.toLowerCase().endsWith(ext)) {
        name += ext;
      }
      File dataDir = Main.getDataDir();
      File target;
      if (parent != null && !parent.isBlank()) {
        target = new File(new File(dataDir, parent), name);
      } else {
        target = new File(dataDir, name);
      }
      String dataPath = dataDir.getCanonicalPath();
      String targetPath = target.getCanonicalPath();
      if (!targetPath.startsWith(dataPath)) {
        throw new SecurityException("Cannot save file outside of the 'data' dir.");
      }
      File targetDir = target.getParentFile();
      if (targetDir != null && !targetDir.exists() && !targetDir.mkdirs()) {
        throw new IOException("Failed to make parents dirs");
      }
      Files.writeString(
          target.toPath(), content, StandardCharsets.UTF_8,
          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
      );
      Path path = Paths.get(targetPath);
      return dataDir.toPath().relativize(path).toString();
    } catch (IOException | SecurityException e) {
      log.error("Failed to save file '{}': {}", name, e.getMessage());
      throw new RuntimeException("Error: " + e.getMessage(), e);
    }
  }

  public static String readFileDataDir(String name) {
    try {
      File dataDir = Main.getDataDir();
      File target = new File(dataDir, name);
      String dataPath = dataDir.getCanonicalPath();
      String targetPath = target.getCanonicalPath();
      if (!targetPath.startsWith(dataPath)) {
        throw new SecurityException("Cannot read outside 'data' dir.");
      }
      if (!target.exists() || !target.isFile()) {
        throw new IOException("File does not exist or is not a file: " + name);
      }
      return Files.readString(target.toPath(), StandardCharsets.UTF_8);
    } catch (IOException | SecurityException e) {
      log.error("Failed to read file '{}': {}", name, e.getMessage());
      throw new RuntimeException("Error: " + e.getMessage(), e);
    }
  }
}
