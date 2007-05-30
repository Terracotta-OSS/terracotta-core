/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

import org.apache.commons.io.FileUtils;

import com.tc.config.schema.test.ApplicationConfigBuilder;
import com.tc.config.schema.test.HaConfigBuilder;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.L2SConfigBuilder;
import com.tc.config.schema.test.SystemConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class ActivePassiveServerConfigCreator {
  public static final String DEV_MODE  = "development";
  public static final String PROD_MODE = "production";

  private static TCLogger    logger    = TCLogging.getTestingLogger(ActivePassiveServerConfigCreator.class);
  private final int          serverCount;
  private final int[]        dsoPorts;
  private final int[]        jmxPorts;
  private final int[]        l2GroupPorts;
  private final String[]     serverNames;
  private final String       serverPersistence;
  private final boolean      serverDiskless;
  private final String       configModel;
  private final File         configFile;
  private final File         tempDir;

  public ActivePassiveServerConfigCreator(int serverCount, int[] dsoPorts, int[] jmxPorts, int[] l2GroupPorts,
                                          String[] serverNames, String serverPersistence, boolean serverDiskless,
                                          String configModel, File configFile, File tempDir) {
    this.serverCount = serverCount;
    this.dsoPorts = dsoPorts;
    this.jmxPorts = jmxPorts;
    this.l2GroupPorts = l2GroupPorts;
    this.serverNames = serverNames;
    this.serverPersistence = serverPersistence;
    this.serverDiskless = serverDiskless;
    this.configModel = configModel;
    this.configFile = configFile;
    this.tempDir = tempDir;

    checkPersistenceAndDiskLessMode();
  }

  private void checkPersistenceAndDiskLessMode() {
    if (!serverDiskless && serverPersistence.equals(L2ConfigBuilder.PERSISTENCE_MODE_TEMPORARY_SWAP_ONLY)) { throw new AssertionError(
                                                                                                                                      "The servers are not running in diskless mode so persistence mode should be set to permanent-store"); }
  }

  private void checkConfigurationModel() {
    if (!configModel.equals(DEV_MODE) && !configModel.equals(PROD_MODE)) { throw new AssertionError(
                                                                                                    "Unknown operating mode."); }
  }

  private void cleanDataDirectory(String dataLocation) throws IOException {
    File dbDir = new File(dataLocation);
    logger.info("DBHome: " + dbDir.getAbsolutePath());
    if (dbDir.exists()) {
      FileUtils.cleanDirectory(dbDir);
    }
  }

  public void writeL2Config() throws Exception {
    checkConfigurationModel();
    SystemConfigBuilder system = SystemConfigBuilder.newMinimalInstance();
    system.setConfigurationModel(configModel);

    String dataLocationHome = tempDir.getAbsolutePath() + File.separator + "server-data";
    cleanDataDirectory(dataLocationHome);
    String logLocationHome = tempDir.getAbsolutePath() + File.separator + "server-logs" + File.separator;
    L2ConfigBuilder[] l2s = new L2ConfigBuilder[serverCount];
    for (int i = 0; i < serverCount; i++) {
      L2ConfigBuilder l2 = new L2ConfigBuilder();
      if (serverDiskless) {
        l2.setData(dataLocationHome + File.separator + "server-" + i);
      } else {
        l2.setData(dataLocationHome);
      }
      l2.setLogs(logLocationHome + "server-" + i);
      l2.setName(serverNames[i]);
      l2.setDSOPort(dsoPorts[i]);
      l2.setJMXPort(jmxPorts[i]);
      l2.setL2GroupPort(l2GroupPorts[i]);
      l2.setPersistenceMode(serverPersistence);
      l2s[i] = l2;
    }
    HaConfigBuilder ha = new HaConfigBuilder();
    if (this.serverDiskless) {
      ha.setMode(HaConfigBuilder.HA_MODE_NETWORKED_ACTIVE_PASSIVE);
    } else {
      ha.setMode(HaConfigBuilder.HA_MODE_ACTIVE_PASSIVE);
    }

    L2SConfigBuilder l2sConfigbuilder = new L2SConfigBuilder();
    l2sConfigbuilder.setL2s(l2s);
    l2sConfigbuilder.setHa(ha);

    ApplicationConfigBuilder app = ApplicationConfigBuilder.newMinimalInstance();

    TerracottaConfigBuilder configBuilder = new TerracottaConfigBuilder();
    configBuilder.setSystem(system);
    configBuilder.setServers(l2sConfigbuilder);
    configBuilder.setApplication(app);

    String configAsString = configBuilder.toString();
    System.err.println("Writing config to file:" + configFile.getAbsolutePath() + configAsString);

    FileOutputStream fileOutputStream = new FileOutputStream(configFile);
    PrintWriter out = new PrintWriter((fileOutputStream));
    out.println(configAsString);
    out.flush();
    out.close();
  }
}
