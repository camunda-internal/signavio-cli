package org.camunda.shell.commands;

import jline.internal.Log;
import org.camunda.bpm.cycle.connector.ConnectorLoginMode;
import org.camunda.bpm.cycle.connector.ConnectorNode;
import org.camunda.bpm.cycle.connector.ConnectorNodeType;
import org.camunda.bpm.cycle.connector.signavio.SignavioConnector;
import org.camunda.bpm.cycle.entity.ConnectorConfiguration;
import org.camunda.shell.IoUtil;
import org.camunda.shell.ShellConfiguration;
import org.fusesource.jansi.Ansi;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Signavio implements CommandMarker {

  private static final String DEFAULT_SIGNAVIO_ENDPOINT = "https://editor.signavio.com";
  private ConnectorConfiguration signavioConnectorConfiguration = null;
  @Inject
  private SignavioConnector signavioConnector;
  @Inject
  private ShellConfiguration shellConfiguration;
  private HashMap<String, ConnectorNode> nodeLabelToConnectorNode = new HashMap<>();

  @CliAvailabilityIndicator({"login"})
  public boolean isLoginAvailable() {
    return true;
  }

  @CliAvailabilityIndicator({"ls", "export"})
  public boolean isListAvailable() {
    return signavioConnectorConfiguration != null;
  }

  @CliCommand(value = "login", help = "Configure Signavio connector and login")
  public void login(
      @CliOption(
          key = {"user"},
          mandatory = true,
          help = "Your Signavio login user, e.g. me@work.com")
      final String user,

      @CliOption(
          key = {"password"},
          mandatory = true,
          help = "Your Signavio password")
      final String password,

      @CliOption(
          key = {"url"},
          unspecifiedDefaultValue = DEFAULT_SIGNAVIO_ENDPOINT,
          help = "The url of your Signavio endpoint, e.g. " + DEFAULT_SIGNAVIO_ENDPOINT)
      final String endpoint
  ) {
    shellConfiguration.setEndpoint(endpoint);
    signavioConnectorConfiguration = configureSignavioConnector(user, password, endpoint);
    signavioConnector.setConfiguration(signavioConnectorConfiguration);
    signavioConnector.login(user, password);
  }

  @CliCommand(value = "logout", help = "Logout from Signavio")
  public void logout() {
    signavioConnector.dispose();
    signavioConnector.setConfiguration(null);
    signavioConnectorConfiguration = null;
  }

  @CliCommand(value = "ls", help = "List files from given folder")
  public String listFiles(
      @CliOption(
          key = {""},
          unspecifiedDefaultValue = "root",
          specifiedDefaultValue = "root",
          help = "Your Signavio folder name or id")
      final String folder
  ) {
    List<ConnectorNode> nodes = getNodesFromFolder(folder);
    return logConnectorNodes(nodes, true);
  }

  @CliCommand(value = "export", help = "Export files from given folder")
  public void exportFilesFromFolder(
      @CliOption(
          key = {"folder"},
          mandatory = true,
          help = "Your Signavio folder label or id to export")
      final String exportFolder,

      @CliOption(
          key = {"output"},
          unspecifiedDefaultValue = "signavio-export",
          help = "The output directory for the files and folders. Defaults to 'signavio-export'")
      final String outputFolder,

      @CliOption(
          key = {"recursive"},
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Recursively include subfolders")
          final String includeSubfolders
  ) {

    Path outputDir = IoUtil.createFolder(outputFolder);
    if (outputDir != null) {
      AtomicInteger exportedSubDirectories = new AtomicInteger(0);
      AtomicInteger exportedFiles = new AtomicInteger(0);
      long startTime = System.currentTimeMillis();
      exportFilesFromFolder(exportFolder, outputDir, Boolean.parseBoolean(includeSubfolders), exportedSubDirectories, exportedFiles);
      Log.info("Exported total of '" + exportedSubDirectories + "' directories with '" + exportedFiles + "' files in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime) + " seconds.");
    } else {
      Log.error("Aborting export operation.");
    }
  }

  protected void exportFilesFromFolder(String parentFolder, Path outputFolder, boolean includeSubfolders, AtomicInteger exportedSubDirectories, AtomicInteger exportedFiles) {
    outputFolder = IoUtil.createFolder(Paths.get(outputFolder.toString(), parentFolder).toString());
    List<ConnectorNode> nodes = getNodesFromFolder(parentFolder);

    for (ConnectorNode node : nodes) {
      if (includeSubfolders && node.isDirectory()) {
        // invoke recursively
        Log.info("Traversing into subfolder '" + node.getLabel() + "'.");
        exportFilesFromFolder(node.getLabel(), outputFolder, includeSubfolders, exportedSubDirectories, exportedFiles);
        exportedSubDirectories.getAndIncrement();
      }

      if (node.getType() == ConnectorNodeType.BPMN_FILE) {
        Path targetFile = sanitizePathForBpmnFile(outputFolder, node);
        try (InputStream content = signavioConnector.getContent(node)) {
          Files.copy(content, targetFile);
          exportedFiles.incrementAndGet();
          Log.info("Exported '" + node.getLabel() + "' to '" + targetFile + "'.");
        } catch (IOException e) {
          Log.error("Error while copying file '" + node.getLabel() + "' to " + targetFile + ". Exception: " + e);
        }
      }
    }
  }

  protected Path sanitizePathForBpmnFile(Path outputFolder, ConnectorNode node) {
    String label = node.getLabel().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    return Paths.get(outputFolder.toString(), label + "_" + node.getId().replaceFirst("/", "") + ".bpmn");
  }

  protected ConnectorConfiguration configureSignavioConnector(String login, String password, String endpoint) {
    ConnectorConfiguration signavioConnectorConfiguration = new ConnectorConfiguration();
    signavioConnectorConfiguration.setConnectorName("SignavioConnector");
    signavioConnectorConfiguration.setName("SignavioConnector");
    signavioConnectorConfiguration.setLoginMode(ConnectorLoginMode.GLOBAL);
    signavioConnectorConfiguration.setGlobalUser(login);
    signavioConnectorConfiguration.setGlobalPassword(password);
    signavioConnectorConfiguration.getProperties().put(SignavioConnector.CONFIG_KEY_SIGNAVIO_BASE_URL, endpoint);
    signavioConnectorConfiguration.setConnectorClass(SignavioConnector.class.getName());

    return signavioConnectorConfiguration;
  }

  protected List<ConnectorNode> getNodesFromFolder(String folder) {
    ConnectorNode cachedNode = getNodeFromCacheByLabel(folder);
    String id = cachedNode != null ? cachedNode.getId() : folder;
    List<ConnectorNode> nodes = null;

    if (folder.equals("root")) {
      nodes = signavioConnector.getChildren(signavioConnector.getRoot());
    } else if (folder.equals("private")) {
      nodes = signavioConnector.getChildren(signavioConnector.getPrivateFolder());
    } else {
      nodes = signavioConnector.getChildren(new ConnectorNode(id));
    }

    cacheConnectorNodes(nodes);

    return nodes;
  }

  protected void cacheConnectorNodes(List<ConnectorNode> nodes) {
    for (ConnectorNode node : nodes) {
      nodeLabelToConnectorNode.put(node.getLabel(), node);
    }
  }

  protected void cacheConnectorNode(ConnectorNode node) {
    nodeLabelToConnectorNode.put(node.getLabel(), node);
  }

  protected ConnectorNode getNodeFromCacheByLabel(String label) {
    ConnectorNode cachedNode = nodeLabelToConnectorNode.get(label);
    Log.debug("cachedNode: " + cachedNode);
    return cachedNode;
  }

  protected String logConnectorNodes(List<ConnectorNode> nodes, boolean showDirectories) {
    StringBuilder sb = new StringBuilder();

    int numOfDirectories = 0,
        numOfFiles = 0;

    nodes = sortNodes(nodes);

    for (ConnectorNode node : nodes) {
      StringBuilder nodeSb = new StringBuilder();

      nodeSb.append(node.getLabel() + " [id:" + node.getId());
      if (node.getLastModified() != null) {
        nodeSb.append(", lastModified:" + node.getLastModified());
      }
      if (node.getCreated() != null) {
        nodeSb.append(", created:" + node.getCreated());
      }
      if (node.getMessage() != null && !node.getMessage().isEmpty()) {
        nodeSb.append(", message:" + node.getMessage());
      }
      nodeSb.append(", type:" + node.getType().toString() + "]" + System.lineSeparator());

      if (node.isDirectory()) {
        numOfDirectories++;
        sb.append(Ansi.ansi().fg(Ansi.Color.CYAN).render(nodeSb.toString()).toString());
      } else {
        numOfFiles++;
        sb.append(Ansi.ansi().fg(Ansi.Color.WHITE).render(nodeSb.toString()).toString());
      }
    }

    sb.append(Ansi.ansi().newline().fg(Ansi.Color.WHITE).render("Total of " + numOfDirectories + " directories and " + numOfFiles + " files.").newline().toString());

    return sb.toString();
  }

  protected List<ConnectorNode> sortNodes(List<ConnectorNode> nodes) {
    List<ConnectorNode> dirs = new ArrayList<>();
    List<ConnectorNode> files = new ArrayList<>();

    for (ConnectorNode node : nodes) {
      if (node.isDirectory()) {
        dirs.add(node);
      } else {
        files.add(node);
      }
    }

    Comparator<ConnectorNode> labelComparator = new Comparator<ConnectorNode>() {
      @Override
      public int compare(ConnectorNode o1, ConnectorNode o2) {
        return o1.getLabel().compareToIgnoreCase(o2.getLabel());
      }
    };
    Collections.sort(dirs, labelComparator);
    Collections.sort(files, labelComparator);
    dirs.addAll(files);

    return nodes = dirs;
  }

}
