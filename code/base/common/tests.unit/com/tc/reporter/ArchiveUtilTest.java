/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.reporter;

import com.tc.config.schema.test.L1ConfigBuilder;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.L2SConfigBuilder;
import com.tc.config.schema.test.SystemConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.sysinfo.EnvStats;
import com.tc.test.TCTestCase;
import com.tc.util.ExternalProcessStreamWriter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ArchiveUtilTest extends TCTestCase {

  private static final String NONE               = "none";
  private static final String TC_CONFIG          = "tc-config.xml";
  private static final String ARCHIVE            = "mock-archive.zip";
  private static final String MK_DATA_DIR        = "mockdata";
  private static final String MK_CLIENT_DIR      = "client-logs";
  private static final String MK_CLIENT_LOG0     = "terracotta-client.log";
  private static final String MK_CLIENT_LOG1     = "terracotta-client.log.1";
  private static final String MK_SERVER_LOG_DIR  = "server-logs";
  private static final String MK_SERVER_LOG0     = "terracotta-server.log";
  private static final String MK_SERVER_LOG1     = "terracotta-server.log.1";
  private static final String MK_SERVER_DATA_DIR = "server-data";
  private static final String MK_SERVER_DATA0    = "objectdb";
  private static final String MK_SERVER_DATA1    = "startup.lck";

  private File                mockDataDir;
  private File                archiveFile;

  public void setUp() throws Exception {
    generateMockFiles(getTempDirectory());
    archiveFile = new File(mockDataDir + File.separator + ARCHIVE);
  }

  private File generateMockFiles(File tmpDir) throws IOException {
    mockDataDir = new File(tmpDir + File.separator + MK_DATA_DIR);
    mockDataDir.mkdir();
    new File(mockDataDir + File.separator + MK_CLIENT_DIR).mkdir();
    new File(mockDataDir + File.separator + MK_CLIENT_DIR + File.separator + MK_CLIENT_LOG0).createNewFile();
    new File(mockDataDir + File.separator + MK_CLIENT_DIR + File.separator + MK_CLIENT_LOG1).createNewFile();
    new File(mockDataDir + File.separator + MK_SERVER_LOG_DIR).mkdir();
    new File(mockDataDir + File.separator + MK_SERVER_LOG_DIR + File.separator + MK_SERVER_LOG0).createNewFile();
    new File(mockDataDir + File.separator + MK_SERVER_LOG_DIR + File.separator + MK_SERVER_LOG1).createNewFile();
    new File(mockDataDir + File.separator + MK_SERVER_DATA_DIR).mkdir();
    new File(mockDataDir + File.separator + MK_SERVER_DATA_DIR + File.separator + MK_SERVER_DATA0).createNewFile();
    new File(mockDataDir + File.separator + MK_SERVER_DATA_DIR + File.separator + MK_SERVER_DATA1).createNewFile();
    return mockDataDir;
  }

  private String createConfig(String clientLogs, String[] serverLogs, String[] serverData) {
    TerracottaConfigBuilder builder = new TerracottaConfigBuilder();
    SystemConfigBuilder sysBuilder = new SystemConfigBuilder();
    sysBuilder.setConfigurationModel("production");
    builder.setSystem(sysBuilder);
    L2ConfigBuilder[] l2Builder = new L2ConfigBuilder[serverLogs.length];
    for (int i = 0; i < l2Builder.length; i++) {
      l2Builder[i] = new L2ConfigBuilder();
      l2Builder[i].setName("localhost-" + i);
      l2Builder[i].setData(serverData[i]);
      l2Builder[i].setLogs(serverLogs[i]);
    }
    L2SConfigBuilder l2SBuilder = new L2SConfigBuilder();
    l2SBuilder.setL2s(l2Builder);
    builder.setServers(l2SBuilder);
    L1ConfigBuilder l1Builder = new L1ConfigBuilder();
    l1Builder.setLogs(clientLogs);
    builder.setClient(l1Builder);
    return builder.toString();
  }

  private File writeConfig(byte[] data) throws IOException {
    File configFile = new File(mockDataDir + File.separator + TC_CONFIG);
    if (configFile.exists()) configFile.delete();
    FileOutputStream out = new FileOutputStream(configFile);
    out.write(data);
    out.flush();
    out.close();
    return configFile;
  }

  private Set listArchiveContents(File archive) throws IOException {
    Set contents = new HashSet();
    ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)));
    ZipEntry entry;
    while ((entry = in.getNextEntry()) != null) {
      contents.add(entry.getName());
      in.closeEntry();
    }
    in.close();
    return contents;
  }

  private int executeArchiveUtil(String[] args) throws IOException, InterruptedException {
    String[] commandLine = new String[args.length + 4];
    commandLine[0] = "java";
    commandLine[1] = "-classpath";
    commandLine[2] = System.getProperty("java.class.path");
    commandLine[3] = ArchiveUtil.class.getName();
    System.arraycopy(args, 0, commandLine, 4, args.length);
    System.out.println("\n***** Executing Java process -- ArchiveUtil *****\n");
    Runtime runtime = Runtime.getRuntime();
    Process process = runtime.exec(commandLine, new String[0], mockDataDir);

    ExternalProcessStreamWriter writeSys = new ExternalProcessStreamWriter();
    writeSys.printSys(process.getInputStream());
    ExternalProcessStreamWriter writeErr = new ExternalProcessStreamWriter();
    writeErr.printErr(process.getInputStream());

    int code = process.waitFor();
    if (writeSys.hasException()) throw writeSys.getException();
    if (writeErr.hasException()) throw writeErr.getException();
    return code;
  }

  private void clear() {
    if (archiveFile.exists()) archiveFile.delete();
  }
  
  private void log(String str) {
    System.out.println("--------------------------------------------------------------------------------");
    System.out.println("-- " + str);
    System.out.println("--------------------------------------------------------------------------------");
  }

  public void testValidServerArchiveContents() throws Exception {
    log("<server> valid archive contents");
    String[] slogs = new String[] { MK_SERVER_LOG_DIR };
    String[] sdata = new String[] { MK_SERVER_DATA_DIR };
    String config = createConfig(NONE, slogs, sdata);
    File configFile = writeConfig(config.getBytes());
    String[] args = new String[] { configFile.toString() };
    assertEquals(0, executeArchiveUtil(args));
    DateFormat df = new SimpleDateFormat("y-M-d");
    File defaultArchive = new File(mockDataDir + File.separator + "tc-archive" + "_"
        + df.format(new Date(System.currentTimeMillis())) + ".zip");
    Set contents = listArchiveContents(defaultArchive);
    assertTrue(contents.contains(TC_CONFIG));
    assertTrue(contents.contains(EnvStats.filename));
    assertTrue(contents.contains(MK_SERVER_LOG_DIR + "/"));
    assertTrue(contents.contains(MK_SERVER_LOG_DIR + "/" + MK_SERVER_LOG0));
    assertTrue(contents.contains(MK_SERVER_LOG_DIR + "/" + MK_SERVER_LOG1));
    assertFalse(contents.contains(MK_SERVER_DATA_DIR + "/"));
    assertFalse(contents.contains(MK_SERVER_DATA_DIR + "/" + MK_SERVER_DATA0));
    assertFalse(contents.contains(MK_SERVER_DATA_DIR + "/" + MK_SERVER_DATA1));
    if (defaultArchive.exists()) defaultArchive.delete();
  }

  public void testFileArg() throws Exception {
    clear();
    log("<server> test file argument");
    String[] slogs = new String[] { MK_SERVER_LOG_DIR };
    String[] sdata = new String[] { MK_SERVER_DATA_DIR };
    String config = createConfig(NONE, slogs, sdata);
    File configFile = writeConfig(config.getBytes());
    String[] args = new String[] { configFile.toString(), archiveFile.toString() };
    assertEquals(0, executeArchiveUtil(args));
    Set contents = listArchiveContents(archiveFile);
    assertTrue(contents.contains(TC_CONFIG));
    assertTrue(contents.contains(EnvStats.filename));
    assertTrue(contents.contains(MK_SERVER_LOG_DIR + "/"));
    assertTrue(contents.contains(MK_SERVER_LOG_DIR + "/" + MK_SERVER_LOG0));
    assertTrue(contents.contains(MK_SERVER_LOG_DIR + "/" + MK_SERVER_LOG1));
    assertFalse(contents.contains(MK_SERVER_DATA_DIR + "/"));
    assertFalse(contents.contains(MK_SERVER_DATA_DIR + "/" + MK_SERVER_DATA0));
    assertFalse(contents.contains(MK_SERVER_DATA_DIR + "/" + MK_SERVER_DATA1));
  }

  public void testValidServerFullArchiveContents() throws Exception {
    clear();
    log("<server> -f valid archive contents");
    String[] slogs = new String[] { MK_SERVER_LOG_DIR };
    String[] sdata = new String[] { MK_SERVER_DATA_DIR };
    String config = createConfig(NONE, slogs, sdata);
    File configFile = writeConfig(config.getBytes());
    String[] args = new String[] { "-f", configFile.toString(), archiveFile.toString() };
    assertEquals(0, executeArchiveUtil(args));
    Set contents = listArchiveContents(archiveFile);
    assertTrue(contents.contains(TC_CONFIG));
    assertTrue(contents.contains(EnvStats.filename));
    assertTrue(contents.contains(MK_SERVER_LOG_DIR + "/"));
    assertTrue(contents.contains(MK_SERVER_LOG_DIR + "/" + MK_SERVER_LOG0));
    assertTrue(contents.contains(MK_SERVER_LOG_DIR + "/" + MK_SERVER_LOG1));
    assertTrue(contents.contains(MK_SERVER_DATA_DIR + "/"));
    assertTrue(contents.contains(MK_SERVER_DATA_DIR + "/" + MK_SERVER_DATA0));
    assertTrue(contents.contains(MK_SERVER_DATA_DIR + "/" + MK_SERVER_DATA1));
  }

  public void testValidClientArchiveContents() throws Exception {
    clear();
    log("<client> valid archive contents");
    String[] slogs = new String[] { MK_SERVER_LOG_DIR };
    String[] sdata = new String[] { MK_SERVER_DATA_DIR };
    String config = createConfig(MK_CLIENT_DIR, slogs, sdata);
    File configFile = writeConfig(config.getBytes());
    String[] args = new String[] { "-c", configFile.toString(), archiveFile.toString() };
    assertEquals(0, executeArchiveUtil(args));
    Set contents = listArchiveContents(archiveFile);
    assertTrue(contents.contains(TC_CONFIG));
    assertTrue(contents.contains(EnvStats.filename));
    assertTrue(contents.contains(MK_CLIENT_DIR + "/"));
    assertTrue(contents.contains(MK_CLIENT_DIR + "/" + MK_CLIENT_LOG0));
    assertTrue(contents.contains(MK_CLIENT_DIR + "/" + MK_CLIENT_LOG1));
    assertFalse(contents.contains(MK_SERVER_LOG_DIR + "/"));
    assertFalse(contents.contains(MK_SERVER_LOG_DIR + "/" + MK_SERVER_LOG0));
    assertFalse(contents.contains(MK_SERVER_LOG_DIR + "/" + MK_SERVER_LOG1));
    assertFalse(contents.contains(MK_SERVER_DATA_DIR + "/"));
    assertFalse(contents.contains(MK_SERVER_DATA_DIR + "/" + MK_SERVER_DATA0));
    assertFalse(contents.contains(MK_SERVER_DATA_DIR + "/" + MK_SERVER_DATA1));
  }
  
  public void testIgnoresFOptionForClient() throws Exception {
    clear();
    log("<client> ignores -f option");
    String config = createConfig(NONE, new String[] { NONE }, new String[] { NONE });
    File configFile = writeConfig(config.getBytes());
    assertEquals(0, executeArchiveUtil(new String[] { "-c", "-f", configFile.toString(), archiveFile.toString() }));
    assertFalse(archiveFile.exists());
  }

  public void testInvalidArgsOrder1() throws Exception {
    clear();
    log("<server> invalid args: <output file> <config>");
    String config = createConfig(NONE, new String[] { NONE }, new String[] { NONE });
    File configFile = writeConfig(config.getBytes());
    assertEquals(0, executeArchiveUtil(new String[] { archiveFile.toString(), configFile.toString() }));
    assertFalse(archiveFile.exists());
  }

  public void testInvalidArgsOrder2() throws Exception {
    clear();
    log("<server> invalid args: <config> <output file> -c");
    String config = createConfig(NONE, new String[] { NONE }, new String[] { NONE });
    File configFile = writeConfig(config.getBytes());
    assertEquals(0, executeArchiveUtil(new String[] { configFile.toString(), archiveFile.toString(), "-c" }));
    assertFalse(archiveFile.exists());
  }
  
  public void testInvalidArgsOrder3() throws Exception {
    clear();
    log("<server> invalid args: <config> -c <output file>");
    String config = createConfig(NONE, new String[] { NONE }, new String[] { NONE });
    File configFile = writeConfig(config.getBytes());
    assertEquals(0, executeArchiveUtil(new String[] { configFile.toString(), "-c", archiveFile.toString() }));
    assertFalse(archiveFile.exists());
  }
  
  public void testInvalidArgs1() throws Exception {
    clear();
    log("<server> invalid args: config");
    assertEquals(0, executeArchiveUtil(new String[] { "foo", archiveFile.toString() }));
    assertFalse(archiveFile.exists());
  }
  
  public void testInvalidArgs2() throws Exception {
    clear();
    log("<server> invalid args: -x");
    String config = createConfig(NONE, new String[] { NONE }, new String[] { NONE });
    File configFile = writeConfig(config.getBytes());
    assertEquals(0, executeArchiveUtil(new String[] { "-x", configFile.toString(), archiveFile.toString() }));
    assertFalse(archiveFile.exists());
  }
  
  public void testInvalidConfig1() throws Exception {
    clear();
    log("<server> invalid config: <foo>");
    File configFile = writeConfig("foo".getBytes());
    assertEquals(0, executeArchiveUtil(new String[] { configFile.toString(), archiveFile.toString() }));
    assertFalse(archiveFile.exists());
  }
  
  public void testInvalidConfig2() throws Exception {
    clear();
    log("<server> invalid config: logs=foo");
    String[] slogs = new String[] { "foo" };
    String config = createConfig(MK_CLIENT_DIR, slogs, new String[] { NONE });
    File configFile = writeConfig(config.getBytes());
    assertEquals(0, executeArchiveUtil(new String[] { configFile.toString(), archiveFile.toString() }));
    assertFalse(archiveFile.exists());
  }
  
  public void testInvalidConfig3() throws Exception {
    clear();
    log("<server> invalid config: data=foo");
    String[] sdata = new String[] { "foo" };
    String config = createConfig(MK_CLIENT_DIR, new String[] { NONE }, sdata);
    File configFile = writeConfig(config.getBytes());
    assertEquals(0, executeArchiveUtil(new String[] { "-f", configFile.toString(), archiveFile.toString() }));
    assertFalse(archiveFile.exists());
  }
  
  public void testInvalidConfig4() throws Exception {
    clear();
    log("<client> invalid config: logs=foo");
    String config = createConfig("foo", new String[] { NONE }, new String[] { NONE });
    File configFile = writeConfig(config.getBytes());
    assertEquals(1, executeArchiveUtil(new String[] { "-c", configFile.toString(), archiveFile.toString() }));
    assertFalse(archiveFile.exists());
  }
  
  // TODO: wildcards will not be tested CDV-93 
}