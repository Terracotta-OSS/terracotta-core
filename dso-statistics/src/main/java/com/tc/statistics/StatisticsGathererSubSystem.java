/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.config.schema.StatisticsConfig;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.statistics.database.exceptions.StatisticsDatabaseStructureMismatchError;
import com.tc.statistics.gatherer.StatisticsGatherer;
import com.tc.statistics.gatherer.impl.StatisticsGathererImpl;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.exceptions.StatisticsStoreException;
import com.tc.statistics.store.h2.H2StatisticsStoreImpl;
import com.tc.util.io.TCFileUtils;

import java.io.File;

public class StatisticsGathererSubSystem {
  private final static TCLogger DSO_LOGGER = CustomerLogging.getDSOGenericLogger();
  private final static TCLogger CONSOLE_LOGGER = CustomerLogging.getConsoleLogger();

  private volatile StatisticsStore statisticsStore;
  private volatile StatisticsGatherer statisticsGatherer;

  private volatile boolean active = false;

  public boolean isActive() {
    return active;
  }

  public synchronized boolean setup(final StatisticsConfig config) {
    // create the statistics store
    File stat_path = config.statisticsPath();
    if (!TCFileUtils.ensureWritableDir(stat_path, 
                                       new TCFileUtils.EnsureWritableDirReporter() {

      public void reportFailedCreate(File dir, Exception e) {
        // TODO: needs to be properly written and put in a properties file
        String msg =
          "\n**************************************************************************************\n"
          + "Unable to create the directory '" + dir.getAbsolutePath() + "' for the statistics buffer.\n"
          + "This directory is specified in the Terracotta configuration. Please ensure that the\n"
          + "Terracotta client has read and write privileges to this directory and its parent directories.\n"
          + "**************************************************************************************\n";
        CONSOLE_LOGGER.error(msg);
        DSO_LOGGER.error(msg, e);
     }

      public void reportReadOnly(File dir, Exception e) {
        // TODO: needs to be properly written and put in a properties file
        String msg =
          "\n**************************************************************************************\n"
          + "Unable to write to the directory '" + dir.getAbsolutePath() + "' for the statistics buffer.\n"
          + "This directory is specified in the Terracotta configuration. Please ensure that the\n"
          + "Terracotta client has write privileges in this directory.\n"
          + "**************************************************************************************\n";
        CONSOLE_LOGGER.error(msg);
        DSO_LOGGER.error(msg, e);
      }
      
    })) {
      return false;
    }
    try {
      statisticsStore = new H2StatisticsStoreImpl(stat_path);
      statisticsStore.open();
    } catch (StatisticsDatabaseStructureMismatchError e) {
      // TODO: needs to be properly written and put in a properties file
      String msg =
        "\n**************************************************************************************\n"
        + "The statistics store couldn't be opened at \n"
        + "'" + stat_path.getAbsolutePath() + "'.\n"
        + "The CVT system will not be active for this node because the statistics store database\n"
        + "structure version doesn't correspond to the one expected by the system.\n"
        + "\n"
        + "A simple solution is to delete the directory in which the statistics are stored so\n"
        + "that a new version of the database can be installed.\n"
        + "**************************************************************************************\n";
      CONSOLE_LOGGER.error(msg);
      DSO_LOGGER.error(msg, e);
      return false;
    } catch (StatisticsStoreException e) {
      // TODO: needs to be properly written and put in a properties file
      String msg =
        "\n**************************************************************************************\n"
        + "The statistics store couldn't be opened at \n"
        + "'" + stat_path.getAbsolutePath() + "'.\n"
        + "The CVT gathering system will not be active for this node.\n"
        + "\n"
        + "A common reason for this is that you're launching several Terracotta clients or\n"
        + "servers on the same machine. The default directory for the statistics store\n"
        + "uses the IP address of the machine that it runs on as the identifier.\n"
        + "When several clients are being executed on the same machine, a typical solution\n"
        + "to properly separate these directories is by using a JVM property at startup\n"
        + "that is unique for each client.\n"
        + "\n"
        + "For example:\n"
        + "  dso-java.sh -Dtc.node-name=node1 your.main.Class\n"
        + "\n"
        + "You can then adapt the tc-config.xml file so that this JVM property is picked\n"
        + "up when the statistics directory is configured by using %(tc.node-name) in the\n"
        + "statistics path.\n"
        + "**************************************************************************************\n";
      CONSOLE_LOGGER.error(msg);
      DSO_LOGGER.error(msg, e);
      return false;
    }
    String info_msg = "Statistics store: '" + stat_path.getAbsolutePath() + "'.";
    DSO_LOGGER.info(info_msg);

    statisticsGatherer = new StatisticsGathererImpl(statisticsStore);

    active = true;
    return true;
  }

  public synchronized void reinitialize() throws Exception {
    statisticsGatherer.reinitialize();
    statisticsStore.reinitialize();
  }

  public synchronized void cleanup() throws Exception {
    try {
      if (statisticsGatherer != null) {
        statisticsGatherer.disconnect();
        statisticsGatherer = null;
      }
    } finally {
      if (statisticsStore != null) {
        statisticsStore.close();
        statisticsStore = null;
      }
    }
  }

  public StatisticsStore getStatisticsStore() {
    return statisticsStore;
  }

  public StatisticsGatherer getStatisticsGatherer() {
    return statisticsGatherer;
  }
}