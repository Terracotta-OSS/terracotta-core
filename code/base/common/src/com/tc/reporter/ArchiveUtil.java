/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.reporter;

import org.apache.xmlbeans.XmlException;

import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.terracottatech.configV2.Client;
import com.terracottatech.configV2.Server;
import com.terracottatech.configV2.Servers;
import com.terracottatech.configV2.TcConfigDocument;
import com.terracottatech.configV2.TcConfigDocument.TcConfig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.namespace.QName;

/**
 * This utility is used to archive Terracotta execution environment information for debugging purposes. Run the
 * <tt>main()</tt> with no arguments for usage.
 */
public final class ArchiveUtil {

  private final boolean       isFull;
  private final boolean       isClient;
  private final File          tcConfig;
  private final File          archiveFile;
  private static final String STDOUT            = "stdout:";
  private static final String STDERR            = "stderr:";
  private static final String ARCHIVE_FILE_NAME = "tc-archive";
  private static final CRC32  crc32             = new CRC32();
  private static final String INVALID           = "Invalid Arguments:\n\n";
  private static final String DASH_F            = "-f";
  private static final String DASH_C            = "-c";
  private static final String HELP              = "\tValid Arguments are:\n\n\t["
                                                    + DASH_F
                                                    + "] (Full, include data files)\n\t["
                                                    + DASH_C
                                                    + "] (Client, collect clent info vs. server)\n\t<path to terracotta config xml file (tc-config.xml)>\n\t[<output filename in .zip format>]\n\nexample:\n\n\t# java "
                                                    + ArchiveUtil.class.getName() + " " + DASH_F
                                                    + " tc-config.xml /home/foo/tc-archive_server.zip";
  private static final Set    validDashArgs          = new HashSet();
  static {
    validDashArgs.add(DASH_F);
    validDashArgs.add(DASH_C);
  }

  private ArchiveUtil(boolean isFull, boolean isClient, File tcConfig, File fileName) {
    this.isFull = isFull;
    this.isClient = isClient;
    this.tcConfig = tcConfig;
    if (fileName == null) {
      File userDir = new File(System.getProperty("user.dir"));
      if (!userDir.exists()) throw new RuntimeException(
          "Unexpected error - system property user.dir does not resolve to an actual directory: " + userDir);
      DateFormat df = new SimpleDateFormat("y-M-d");
      String name = ARCHIVE_FILE_NAME + "_" + df.format(new Date(System.currentTimeMillis())) + ".zip";
      this.archiveFile = new File(userDir + File.separator + name);
    } else {
      this.archiveFile = fileName;
    }
  }

  private static void escape(String msg, Exception e) {
    System.out.println(INVALID + msg);
    if (e != null) e.printStackTrace();
    System.exit(0);
  }

  public static void main(String[] args) {
    if (args.length < 1) escape(HELP, null);
    boolean dashArgs = true;
    int configArg = -1;
    int fileArg = -1;
    Set dashSet = new HashSet(2);
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-")) {
        if (!dashArgs) escape(HELP, null);
        if (validDashArgs.contains(args[i])) dashSet.add(args[i]);
        else escape(HELP, null);
      } else {
        dashArgs = false;
        if (fileArg + configArg > 0) escape(HELP, null);
        if (configArg < 0) configArg = i;
        else if (fileArg < 0) fileArg = i;
        if (fileArg + configArg == -2) escape(HELP, null);
      }
    }
    if (dashSet.size() > 2) escape(HELP, null);
    boolean dashC = dashSet.contains(DASH_C);
    boolean dashF = dashSet.contains(DASH_F);

    if (configArg < 0) escape("Please specify where the Terracotta config file is located", null);
    File tcConfigFile = new File(args[configArg]);
    if (!tcConfigFile.exists()) escape("\tTerracotta Configuration file: " + tcConfigFile + "\n\tdoes not exist", null);
    File outputFile = null;
    if (fileArg > 0) {
      outputFile = new File(args[fileArg]);
      if (!new File(outputFile.getParent()).exists()) escape(
          "\tThe directory specified for the output file does not exist", null);
    }
    try {
      new ArchiveUtil(dashF, dashC, tcConfigFile, outputFile).createArchive();
    } catch (IOException e) {
      escape("\tUnable to read Terracotta configuration file\n", e);
    } catch (XmlException e) {
      escape("\tUnable to parse Terracotta configuration file\n", e);
    }
  }

  private File makeAbsolute(File file) {
    if (file.isAbsolute()) return file;
    return new File(tcConfig.getParent() + File.separator + file);
  }

  private File getClientLogsLocation(TcConfig configBeans) throws IOException, XmlException {
    Client clients = configBeans.getClients();
    if (clients == null) throw new XmlException("The Terracotta config specified doesn't contain the <clients> element");
    String logs = clients.getLogs();
    if (isStdX(logs)) return null;
    if (logs == null) logs = Client.type.getElementProperty(QName.valueOf("logs")).getDefaultText();
    String clientLogs = ParameterSubstituter.substitute(logs);
    File clientLogsDir = makeAbsolute(new File(clientLogs));
    if (!clientLogsDir.exists()) throw new RuntimeException("\nError occured while parsing: " + tcConfig
        + "\n\tUnable to locate client log files at: " + clientLogs);
    return clientLogsDir;
  }

  private boolean isStdX(String value) {
    if (value == null) return false;
    return (value.equals(STDOUT) || value.equals(STDERR));
  }

  private Server[] getServersElement(TcConfig configBeans) throws IOException, XmlException {
    Servers servers = configBeans.getServers();
    if (servers == null) throw new XmlException("The Terracotta config specified doesn't contain the <servers> element");
    return servers.getServerArray();
  }

  private File[] getServerLogsLocation(TcConfig configBeans) throws IOException, XmlException {
    Server[] servers = getServersElement(configBeans);
    String[] logs = new String[servers.length];
    File[] logFiles = new File[servers.length];
    for (int i = 0; i < servers.length; i++) {
      logs[i] = servers[i].getLogs();
      if (isStdX(logs[i])) logs[i] = null;
      if (logs[i] == null) logs[i] = Server.type.getElementProperty(QName.valueOf("logs")).getDefaultText();
      logs[i] = ParameterSubstituter.substitute(logs[i]);
      File serverLogsDir = makeAbsolute(new File(logs[i]));
      if (!serverLogsDir.exists()) throw new XmlException("\nError occured while parsing: " + tcConfig
          + "\n\tUnable to locate server log files at: " + logs[i]);
      logFiles[i] = serverLogsDir;
    }
    return logFiles;
  }

  private File[] getServerDataLocation(TcConfig configBeans) throws IOException, XmlException {
    if (!isFull) return null;
    Server[] servers = getServersElement(configBeans);
    String[] serverData = new String[servers.length];
    File[] dataFiles = new File[servers.length];
    for (int i = 0; i < servers.length; i++) {
      serverData[i] = servers[i].getData();
      if (serverData[i] == null) serverData[i] = Server.type.getElementProperty(QName.valueOf("data")).getDefaultText();
      serverData[i] = ParameterSubstituter.substitute(serverData[i]);
      File serverDataDir = makeAbsolute(new File(serverData[i]));
      if (!serverDataDir.exists()) throw new XmlException("\nError occured while parsing: " + tcConfig
          + "\n\tUnable to locate server data files at: " + serverData[i]);
      dataFiles[i] = serverDataDir;
    }
    return dataFiles;
  }

  private void createArchive() throws IOException, XmlException {
    TcConfig configBeans = TcConfigDocument.Factory.parse(tcConfig).getTcConfig();
    File clientLogsDir = null;
    File[] serverLogsDir = null;
    File[] serverDataDir = null;
    if (isClient) {
      clientLogsDir = getClientLogsLocation(configBeans);
    } else {
      serverLogsDir = getServerLogsLocation(configBeans);
      serverDataDir = getServerDataLocation(configBeans);
    }
    try {
      ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archiveFile)));
      zout.setMethod(ZipEntry.DEFLATED);
      zout.setLevel(9);
      System.out.println("Archiving:");
      putEntry(zout, tcConfig.getName(), readFile(tcConfig));
      if (isClient) {
        if (clientLogsDir != null) putTraverseDirectory(zout, clientLogsDir, clientLogsDir.getName());
      } else {
        for (int i = 0; i < serverLogsDir.length; i++) {
          if (serverLogsDir[i] != null) putTraverseDirectory(zout, serverLogsDir[i], serverLogsDir[i].getName());
        }
        if (serverDataDir != null) {
          for (int i = 0; i < serverDataDir.length; i++) {
            putTraverseDirectory(zout, serverDataDir[i], serverDataDir[i].getName());
          }
        }
      }
      zout.close();
    } catch (IOException e) {
      System.out.println("Unexpected error - unable to write Terracotta archive: " + ARCHIVE_FILE_NAME);
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println("\n\nWrote archive to:" + archiveFile);
  }

  private void putTraverseDirectory(ZipOutputStream zout, File dir, String dirName) throws IOException {
    if (!dir.isDirectory()) throw new IOException("Unexpected Exception: " + dir + "\nis not a directory");
    putDirEntry(zout, dirName);
    String[] files = dir.list();
    for (int i = 0; i < files.length; i++) {
      File file = new File(dir + File.separator + files[i]);
      if (file.isDirectory()) {
        putTraverseDirectory(zout, file, dirName + File.separator + file.getName());
        continue;
      }
      putEntry(zout, dirName + File.separator + files[i], readFile(file));
    }
  }

  private byte[] readFile(File file) throws IOException {
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
    byte[] bytes = new byte[in.available()];
    in.read(bytes);
    in.close();
    return bytes;
  }

  private void putDirEntry(ZipOutputStream zout, String file) throws IOException {
    ZipEntry entry = new ZipEntry(zipPath(file) + "/");
    entry.setSize(0);
    entry.setCrc(0);
    zout.putNextEntry(entry);
  }

  private void putEntry(ZipOutputStream zout, String file, byte[] bytes) throws IOException {
    ZipEntry entry = new ZipEntry(zipPath(file));
    entry.setSize(bytes.length);
    entry.setCrc(getCrc32(bytes));
    zout.putNextEntry(entry);
    zout.write(bytes, 0, bytes.length);
    System.out.print(".");
  }
  
  private long getCrc32(byte[] bytes) {
    crc32.update(bytes);
    long checksum = crc32.getValue();
    crc32.reset();
    return checksum;
  }
  
  private String zipPath(String file) {
    if (File.separator.equals("/")) return file;
    return file.replaceAll("\\\\", "/");
  }
}
