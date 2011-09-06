package com.tc.statistics;

import com.tc.config.schema.StatisticsConfig;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.management.TerracottaManagement;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.beans.impl.StatisticsEmitterMBeanImpl;
import com.tc.statistics.beans.impl.StatisticsManagerMBeanImpl;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.exceptions.StatisticsBufferException;
import com.tc.statistics.buffer.h2.H2StatisticsBufferImpl;
import com.tc.statistics.buffer.memory.MemoryStatisticsBufferImpl;
import com.tc.statistics.config.DSOStatisticsConfig;
import com.tc.statistics.config.impl.StatisticsConfigImpl;
import com.tc.statistics.database.exceptions.StatisticsDatabaseStructureMismatchError;
import com.tc.statistics.logging.StatisticsLogger;
import com.tc.statistics.logging.impl.StatisticsLoggerImpl;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.actions.SRAMemoryUsage;
import com.tc.statistics.retrieval.impl.StatisticsRetrievalRegistryImpl;
import com.tc.util.UUID;
import com.tc.util.io.TCFileUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

public class StatisticsAgentSubSystemImpl implements StatisticsAgentSubSystem {

  private final static TCLogger                        DSO_LOGGER                  = CustomerLogging
                                                                                       .getDSOGenericLogger();
  private final static TCLogger                        CONSOLE_LOGGER              = CustomerLogging.getConsoleLogger();
  private final List<StatisticsAgentSubSystemCallback> callbacks                   = new CopyOnWriteArrayList<StatisticsAgentSubSystemCallback>();
  private final StatisticsRetrievalRegistry            statisticsRetrievalRegistry = new StatisticsRetrievalRegistryImpl();
  private volatile StatisticsBuffer                    statisticsBuffer;
  private volatile StatisticsEmitterMBean              statisticsEmitterMBean;
  private volatile StatisticsManagerMBeanImpl          statisticsManagerMBean;
  private volatile StatisticsLogger                    statisticsLogger;
  private volatile boolean                             setupComplete               = false;
  private volatile boolean                             active                      = false;

  public boolean isActive() {
    return active;
  }

  public void addCallback(final StatisticsAgentSubSystemCallback callback) {
    if (callback != null) {
      callbacks.add(callback);
    }
  }

  public void removeCallback(final StatisticsAgentSubSystemCallback callback) {
    if (callback != null) {
      callbacks.remove(callback);
    }
  }

  public void setDefaultAgentIp(final String defaultAgentIp) {
    if (null == statisticsBuffer) { throw new AssertionError("The statistics subsystem has to be setup before."); }
    statisticsBuffer.setDefaultAgentIp(defaultAgentIp);
  }

  public void setDefaultAgentDifferentiator(final String defaultAgentDifferentiator) {
    if (null == statisticsBuffer) { throw new AssertionError("The statistics subsystem has to be setup before."); }
    statisticsBuffer.setDefaultAgentDifferentiator(defaultAgentDifferentiator);
  }

  public synchronized boolean setup(final StatisticsSystemType type, final StatisticsConfig config) {
    try {
      DSOStatisticsConfig statistics_config = new StatisticsConfigImpl();

      // setup the statistics logger
      statisticsLogger = new StatisticsLoggerImpl(statistics_config);
      statisticsLogger.registerAction(new SRAMemoryUsage());
      statisticsLogger.startup();

      switch (type) {
        case CLIENT:
          statisticsBuffer = createClientStatisticsBuffer(type, statistics_config);
          break;
        case SERVER:
          statisticsBuffer = createServerStatisticsBuffer(type, statistics_config, config.statisticsPath());
          break;
        default:
          throw new TCRuntimeException("Unsupported statistics system type : " + type);
      }

      if (null == statisticsBuffer) { return false; }

      // create the statistics emitter mbean
      try {
        statisticsEmitterMBean = new StatisticsEmitterMBeanImpl(statistics_config, statisticsBuffer);
      } catch (NotCompliantMBeanException e) {
        throw new TCRuntimeException("Unable to construct the " + StatisticsEmitterMBeanImpl.class.getName()
                                     + " MBean; this is a programming error. Please go fix that class.", e);
      }

      // setup the statistics manager
      try {
        statisticsManagerMBean = new StatisticsManagerMBeanImpl(statistics_config, statisticsRetrievalRegistry,
                                                                statisticsBuffer);
      } catch (NotCompliantMBeanException e) {
        throw new TCRuntimeException("Unable to construct the " + StatisticsManagerMBeanImpl.class.getName()
                                     + " MBean; this is a programming error. Please go fix that class.", e);
      }

      for (StatisticsAgentSubSystemCallback callback : callbacks) {
        callback.setupComplete(this);
      }

      active = true;
    } finally {
      setupComplete = true;
      this.notifyAll();
    }
    return true;
  }

  private StatisticsBuffer createClientStatisticsBuffer(final StatisticsSystemType type,
                                                        final DSOStatisticsConfig config) {
    final StatisticsBuffer buffer = new MemoryStatisticsBufferImpl(type, config);
    try {
      buffer.open();
    } catch (StatisticsBufferException e) {
      // TODO: needs to be properly written and put in a properties file
      String msg = "\n**************************************************************************************\n"
                   + "The statistics buffer couldn't be opened.\n"
                   + "The CVT system will not be active for this node.\n" + "\n"
                   + "**************************************************************************************\n";
      CONSOLE_LOGGER.warn(msg);
      DSO_LOGGER.warn(msg, e);
      return null;
    }
    DSO_LOGGER.info("Statistics buffer opened");

    return buffer;
  }

  private StatisticsBuffer createServerStatisticsBuffer(final StatisticsSystemType type,
                                                        final DSOStatisticsConfig config, final File statPath) {
    if (!TCFileUtils.ensureWritableDir(statPath, new TCFileUtils.EnsureWritableDirReporter() {

      public void reportFailedCreate(final File dir, final Exception e) {
        // TODO: needs to be properly written and put in a properties file
        String msg = "\n**************************************************************************************\n"
                     + "Unable to create the directory '" + dir.getAbsolutePath() + "' for the statistics buffer.\n"
                     + "The CVT system will not be active for this node. To fix this, ensure that the Terracotta\n"
                     + "client has read and write privileges to this directory and its parent directories.\n"
                     + "**************************************************************************************\n";
        CONSOLE_LOGGER.warn(msg);
        DSO_LOGGER.warn(msg, e);
      }

      public void reportReadOnly(final File dir, final Exception e) {
        // TODO: needs to be properly written and put in a properties file
        String msg = "\n**************************************************************************************\n"
                     + "Unable to write to the directory '" + dir.getAbsolutePath() + "' for the statistics buffer.\n"
                     + "The CVT system will not be active for this node. To fix this, ensure that the Terracotta\n"
                     + "client has write privileges in this directory.\n"
                     + "**************************************************************************************\n";
        CONSOLE_LOGGER.warn(msg);
        DSO_LOGGER.warn(msg, e);
      }
    })) { return null; }

    final StatisticsBuffer buffer = new H2StatisticsBufferImpl(type, config, statPath);

    try {
      buffer.open();
    } catch (StatisticsDatabaseStructureMismatchError e) {
      String msg = "\n**************************************************************************************\n"
                   + "The statistics buffer couldn't be opened at \n" + "'" + statPath.getAbsolutePath() + "'.\n"
                   + "The CVT system will not be active for this node because the statistics buffer database\n"
                   + "structure version doesn't correspond to the one expected by the system.\n" + "\n"
                   + "A simple solution is to delete the directory in which the statistics are stored so\n"
                   + "that a new version of the database can be installed.\n"
                   + "**************************************************************************************\n";
      CONSOLE_LOGGER.warn(msg);
      DSO_LOGGER.warn(msg, e);
      return null;
    } catch (StatisticsBufferException e) {
      // TODO: needs to be properly written and put in a properties file
      String msg = "\n**************************************************************************************\n"
                   + "The statistics buffer couldn't be opened at \n" + "'" + statPath.getAbsolutePath() + "'.\n"
                   + "Do you have another Terracotta Server instance running?" + "\n"
                   + "**************************************************************************************\n";
      CONSOLE_LOGGER.warn(msg);
      DSO_LOGGER.warn(msg, e);
      return null;
    }

    DSO_LOGGER.info("Statistics buffer: '" + statPath.getAbsolutePath() + "'.");
    return buffer;
  }

  public void registerMBeans(final MBeanServer server, final UUID uuid) throws MBeanRegistrationException,
      NotCompliantMBeanException, InstanceAlreadyExistsException, MalformedObjectNameException {
    server.registerMBean(statisticsEmitterMBean,
                         TerracottaManagement.addNodeInfo(StatisticsMBeanNames.STATISTICS_EMITTER, uuid));
    server.registerMBean(statisticsManagerMBean,
                         TerracottaManagement.addNodeInfo(StatisticsMBeanNames.STATISTICS_MANAGER, uuid));
  }

  public void registerMBeans(final MBeanServer server) throws MBeanRegistrationException, NotCompliantMBeanException,
      InstanceAlreadyExistsException {
    server.registerMBean(statisticsEmitterMBean, StatisticsMBeanNames.STATISTICS_EMITTER);
    server.registerMBean(statisticsManagerMBean, StatisticsMBeanNames.STATISTICS_MANAGER);
  }

  public void unregisterMBeans(final MBeanServer server) throws MBeanRegistrationException {
    try {
      server.unregisterMBean(StatisticsMBeanNames.STATISTICS_EMITTER);
    } catch (InstanceNotFoundException e) {
      DSO_LOGGER
          .warn("Unexpected error while unregistering mbean '" + StatisticsMBeanNames.STATISTICS_EMITTER + "'", e);
    }
    try {
      server.unregisterMBean(StatisticsMBeanNames.STATISTICS_MANAGER);
    } catch (Exception e) {
      DSO_LOGGER
          .warn("Unexpected error while unregistering mbean '" + StatisticsMBeanNames.STATISTICS_MANAGER + "'", e);
    }
  }

  public void disableJMX() throws Exception {
    if (statisticsEmitterMBean != null) {
      statisticsEmitterMBean.disable();
    }
  }

  public synchronized void cleanup() throws Exception {
    if (statisticsLogger != null) {
      statisticsLogger.shutdown();
      statisticsLogger = null;
    }

    if (statisticsBuffer != null) {
      statisticsBuffer.close();
      statisticsBuffer = null;
    }
  }

  public StatisticsRetrievalRegistry getStatisticsRetrievalRegistry() {
    return statisticsRetrievalRegistry;
  }

  public AgentStatisticsManager getStatisticsManager() {
    return statisticsManagerMBean;
  }

  public StatisticsLogger getStatisticsLogger() {
    return statisticsLogger;
  }

  public boolean waitUntilSetupComplete() {
    synchronized (this) {
      while (!setupComplete) {
        try {
          this.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return active;
        }
      }

      return active;
    }
  }
}
