package org.camunda.shell;

import jline.internal.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by hawky4s on 15.10.15.
 */
public class IoUtil {

  public static Path createFolder(String outputFolder) {
    Path folder = Paths.get(outputFolder.replaceAll("[^a-zA-Z0-9-_\\./]", "_"));

    File outputDir = new File(outputFolder);
    if (outputDir.exists() && outputDir.isDirectory()) {
      Log.info("Output folder '" + outputDir + "' already exists.");
      folder = Paths.get(folder.toString(), String.valueOf(System.currentTimeMillis()));
    }

    Log.info("Creating output folder '" + folder + "'.");
    try {
      folder = Files.createDirectory(folder);
    } catch (IOException e) {
      Log.error("Unable to create output folder '" + folder + "'");
    }

    return folder;
  }
}
