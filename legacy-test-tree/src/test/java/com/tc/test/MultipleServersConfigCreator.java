/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import org.apache.commons.io.FileUtils;

import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.config.test.schema.ApplicationConfigBuilder;
import com.tc.config.test.schema.GroupConfigBuilder;
import com.tc.config.test.schema.GroupsConfigBuilder;
import com.tc.config.test.schema.HaConfigBuilder;
import com.tc.config.test.schema.L2ConfigBuilder;
import com.tc.config.test.schema.L2SConfigBuilder;
import com.tc.config.test.schema.MembersConfigBuilder;
import com.tc.config.test.schema.SystemConfigBuilder;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class MultipleServersConfigCreator {

  public static final String                           DEV_MODE  = "development";
  public static final String                           PROD_MODE = "production";

  protected final int                                  serverCount;

  protected final String                               serverPersistence;
  protected final boolean                              serverDiskless;
  protected final String                               configModel;
  protected final File                                 configFile;
  protected final File                                 tempDir;
  protected final TestConfigurationSetupManagerFactory configFactory;
  protected final String[]                             dataLocations;
  protected final DSOApplicationConfigBuilder          dsoApplicationBuilder;
  protected static TCLogger                            logger    = TCLogging
                                                                     .getTestingLogger(MultipleServersConfigCreator.class);

  private final MultipleServersTestSetupManager        setupManager;
  private final GroupData[]                            groupData;

  public MultipleServersConfigCreator(MultipleServersTestSetupManager setupManager, GroupData[] groupData,
                                      String configModel, File configFile, File tempDir,
                                      TestConfigurationSetupManagerFactory configFactory,
                                      DSOApplicationConfigBuilder dsoApplicationBuilder) {

    this.setupManager = setupManager;
    this.groupData = groupData;
    this.serverCount = setupManager.getServerCount();

    this.serverPersistence = setupManager.getServerPersistenceMode();
    this.serverDiskless = setupManager.isNetworkShare();
    this.configModel = configModel;
    this.configFile = configFile;
    this.tempDir = tempDir;
    this.configFactory = configFactory;
    dataLocations = new String[serverCount];
    this.dsoApplicationBuilder = dsoApplicationBuilder;

    checkPersistenceAndDiskLessMode();
  }

  protected void checkPersistenceAndDiskLessMode() {
    if (!serverDiskless && serverPersistence.equals(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY)) { throw new AssertionError(
                                                                                                                                    "The servers are not running in diskless mode so persistence mode should be set to permanent-store"); }
  }

  protected void checkConfigurationModel() {
    if (!configModel.equals(DEV_MODE) && !configModel.equals(PROD_MODE)) { throw new AssertionError(
                                                                                                    "Unknown operating mode."); }
  }

  protected void createOrCleanDirectory(String dataLocation, String dirFor, boolean cleanIfExist) throws IOException {
    File dbDir = new File(dataLocation);
    logger.info(dirFor + ": " + dbDir.getAbsolutePath());
    if (dbDir.exists()) {
      if (cleanIfExist) {
        FileUtils.cleanDirectory(dbDir);
      }
    } else {
      FileUtils.forceMkdir(dbDir);
    }
  }

  public String getDataLocation(int i) {
    if (i < 1 && i > dataLocations.length) { throw new AssertionError("Invalid index=[" + i + "]... there are ["
                                                                      + dataLocations.length
                                                                      + "] servers involved in this test."); }
    return dataLocations[i];
  }

  public void writeL2Config() throws Exception {
    writeL2ConfigBase(true);
  }

  public void writeL2ConfigWithoutCleanData() throws Exception {
    writeL2ConfigBase(false);
  }

  private void writeL2ConfigBase(boolean cleanIfDataExist) throws Exception {
    checkConfigurationModel();
    SystemConfigBuilder system = SystemConfigBuilder.newMinimalInstance();
    system.setConfigurationModel(configModel);

    String dataLocationHome = tempDir.getAbsolutePath() + File.separator + "server-data";
    createOrCleanDirectory(dataLocationHome, "DBHome", cleanIfDataExist);
    String logLocationHome = tempDir.getAbsolutePath() + File.separator + "server-logs" + File.separator;
    createOrCleanDirectory(logLocationHome, "Log home", cleanIfDataExist);
    String statisticsLocationHome = tempDir.getAbsolutePath() + File.separator + "server-statistics" + File.separator;
    createOrCleanDirectory(statisticsLocationHome, "Statistics home", cleanIfDataExist);

    boolean gcEnabled = configFactory.getGCEnabled();
    boolean gcVerbose = configFactory.getGCVerbose();
    int gcIntervalInSec = configFactory.getGCIntervalInSec();

    boolean offHeapEnabled = configFactory.isOffHeapEnabled();
    String offHeapMaxSize = configFactory.getOffHeapMaxDataSize();

    L2ConfigBuilder[] l2s = new L2ConfigBuilder[serverCount];
    int serverIndex = 0;
    for (int i = 0; i < groupData.length; i++) {
      for (int j = 0; j < groupData[i].getServerCount(); j++) {
        L2ConfigBuilder l2 = new L2ConfigBuilder();
        l2.setReconnectWindowForPrevConnectedClients(setupManager.getReconnectWindow());
        String mode = setupManager.getGroupServerShareDataMode(i);
        boolean isServerDiskless = !mode.equals(MultipleServersSharedDataMode.DISK) ? true : false;
        if (isServerDiskless) {
          dataLocations[serverIndex] = dataLocationHome + File.separator + "server-" + serverIndex;
        } else {
          int index = 0;
          for (int k = 0; k < i; k++) {
            index = index + groupData[k].getServerCount();
          }
          dataLocations[serverIndex] = dataLocationHome + File.separator + "server-" + index;
        }
        l2.setData(dataLocations[serverIndex]);
        l2.setLogs(logLocationHome + "server-" + serverIndex);
        l2.setStatistics(statisticsLocationHome + "server-" + serverIndex);
        l2.setName(groupData[i].getServerNames()[j]);
        l2.setDSOPort(groupData[i].getDsoPorts()[j]);
        l2.setJMXPort(groupData[i].getJmxPorts()[j]);
        l2.setL2GroupPort(groupData[i].getL2GroupPorts()[j]);
        l2.setPersistenceMode(serverPersistence);
        l2.setGCEnabled(gcEnabled);
        l2.setGCVerbose(gcVerbose);
        if (configFactory.isOffHeapEnabled()) {
          l2.setOffHeapEnabled(offHeapEnabled);
          l2.setOffHeapMaxDataSize(offHeapMaxSize);
        }
        l2.setGCInterval(gcIntervalInSec);
        l2s[serverIndex] = l2;
        serverIndex++;
      }
    }

    HaConfigBuilder ha = new HaConfigBuilder();
    if (this.serverDiskless) {
      ha.setMode(HaConfigBuilder.HA_MODE_NETWORKED_ACTIVE_PASSIVE);
    } else {
      ha.setMode(HaConfigBuilder.HA_MODE_DISK_BASED_ACTIVE_PASSIVE);
    }
    ha.setElectionTime(this.setupManager.getElectionTime() + "");

    L2SConfigBuilder l2sConfigbuilder = new L2SConfigBuilder();
    l2sConfigbuilder.setL2s(l2s);
    l2sConfigbuilder.setHa(ha);

    int indent = 7;
    GroupsConfigBuilder groupsConfigBuilder = new GroupsConfigBuilder();
    for (int i = 0; i < this.groupData.length; i++) {
      GroupConfigBuilder groupConfigBuilder = new GroupConfigBuilder(groupData[i].getGroupName());
      HaConfigBuilder groupHaConfigBuilder = new HaConfigBuilder(indent);
      String mode = null;
      if (setupManager.getGroupServerShareDataMode(i).equals(MultipleServersSharedDataMode.DISK)) mode = HaConfigBuilder.HA_MODE_DISK_BASED_ACTIVE_PASSIVE;
      else mode = HaConfigBuilder.HA_MODE_NETWORKED_ACTIVE_PASSIVE;
      groupHaConfigBuilder.setMode(mode);
      groupHaConfigBuilder.setElectionTime("" + this.setupManager.getGroupElectionTime(i));
      MembersConfigBuilder members = new MembersConfigBuilder();
      for (int j = 0; j < groupData[i].getServerCount(); j++) {
        members.addMember(groupData[i].getServerNames()[j]);
      }
      groupConfigBuilder.setHa(groupHaConfigBuilder);
      groupConfigBuilder.setMembers(members);
      groupsConfigBuilder.addGroupConfigBuilder(groupConfigBuilder);
    }
    l2sConfigbuilder.setGroups(groupsConfigBuilder);

    ApplicationConfigBuilder app = ApplicationConfigBuilder.newMinimalInstance();
    app.setDSO(dsoApplicationBuilder);

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
