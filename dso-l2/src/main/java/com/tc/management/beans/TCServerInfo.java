/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.management.beans;

import com.tc.async.impl.MonitoringEventCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.JVMMemoryManager;
import com.tc.runtime.TCRuntime;
import com.tc.server.TCServer;
import com.tc.util.ProductInfo;
import com.tc.util.StringUtil;
import com.tc.util.runtime.ThreadDumpUtil;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;

public class TCServerInfo extends AbstractTerracottaMBean implements TCServerInfoMBean, StateChangeListener {
  private static final Logger logger = LoggerFactory.getLogger(TCServerInfo.class);

  private static final boolean                 DEBUG           = false;

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;
  static {
    final String[] notifTypes = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
    final String name = AttributeChangeNotification.class.getName();
    final String description = "An attribute of this MBean has changed";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  private final TCServer                       server;
  private final ProductInfo                    productInfo;
  private final String                         buildID;

  private final StateChangeNotificationInfo    stateChangeNotificationInfo;
  private long                                 nextSequenceNumber;

  private final JVMMemoryManager               manager;

  public TCServerInfo(TCServer server)
      throws NotCompliantMBeanException {
    super(TCServerInfoMBean.class, true);
    this.server = server;
    this.productInfo = ProductInfo.getInstance();
    this.buildID = productInfo.buildID();
    this.nextSequenceNumber = 1;
    this.stateChangeNotificationInfo = new StateChangeNotificationInfo();
    this.manager = TCRuntime.getJVMMemoryManager();
    if (TCPropertiesImpl.getProperties().getBoolean("tc.pipeline.monitoring.stats", false)) {
      setPipelineMonitoring(true);
    }
  }

  @Override
  public void reset() {
    // nothing to reset
  }

  @Override
  public boolean isStarted() {
    return server.isStarted();
  }

  @Override
  public boolean isActive() {
    return server.isActive();
  }

  @Override
  public boolean isPassiveUninitialized() {
    return server.isPassiveUnitialized();
  }

  @Override
  public boolean isPassiveStandby() {
    return server.isPassiveStandby();
  }

  @Override
  public boolean isReconnectWindow() {
    return server.isReconnectWindow();
  }

  @Override
  public int getReconnectWindowTimeout() {
    return server.getReconnectWindowTimeout();
  }

  @Override
  public long getStartTime() {
    return server.getStartTime();
  }

  @Override
  public long getActivateTime() {
    return server.getActivateTime();
  }

  @Override
  public void stop() {
    server.stop();
    _sendNotification("TCServer stopped", "Started", "java.lang.Boolean", Boolean.TRUE, Boolean.FALSE);
  }

  @Override
  public boolean isShutdownable() {
    return server.canShutdown();
  }

  /**
   * This schedules the shutdown to occur one second after we return from this call because otherwise JMX will be
   * shutdown and we'll get all sorts of other errors trying to return from this call.
   */
  @Override
  public void shutdown() {
    if (!server.canShutdown()) {
      String msg = "Server cannot be shutdown because it is not fully started.";
      logger.error(msg);
      throw new RuntimeException(msg);
    }
    logger.warn("shutdown is invoked by MBean");
    final Timer timer = new Timer("TCServerInfo shutdown timer");
    final TimerTask task = new TimerTask() {
      @Override
      public void run() {
        server.shutdown();
      }
    };
    timer.schedule(task, 1000);
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return Arrays.asList(NOTIFICATION_INFO).toArray(EMPTY_NOTIFICATION_INFO);
  }

  @Override
  public String toString() {
    if (isStarted()) {
      return "starting, startTime(" + getStartTime() + ")";
    } else if (isActive()) {
      return "active, activateTime(" + getActivateTime() + ")";
    } else {
      return "stopped";
    }
  }

  @Override
  public String getState() {
    return server.getState().getName();
  }

  @Override
  public String getVersion() {
    return productInfo.toShortString();
  }

  @Override
  public String getMavenArtifactsVersion() {
    return productInfo.mavenArtifactsVersion();
  }

  @Override
  public String getBuildID() {
    return buildID;
  }
  
  

  @Override
  public boolean isPatched() {
    return productInfo.isPatched();
  }

  @Override
  public String getPatchLevel() {
    if (productInfo.isPatched()) {
      return productInfo.patchLevel();
    } else {
      return "";
    }
  }

  @Override
  public String getPatchVersion() {
    if (productInfo.isPatched()) {
      return productInfo.toLongPatchString();
    } else {
      return "";
    }
  }

  @Override
  public String getPatchBuildID() {
    if (productInfo.isPatched()) {
      return productInfo.patchBuildID();
    } else {
      return "";
    }
  }

  @Override
  public String getCopyright() {
    return productInfo.copyright();
  }

  @Override
  public String getDescriptionOfCapabilities() {
    return server.getDescriptionOfCapabilities();
  }

  @Override
  public L2Info[] getL2Info() {
    return server.infoForAllL2s();
  }

  @Override
  public String getL2Identifier() {
    return server.getL2Identifier();
  }

  @Override
  public ServerGroupInfo getStripeInfo() {
    return server.getStripeInfo();
  }

  @Override
  public int getTSAListenPort() {
    return server.getTSAListenPort();
  }

  @Override
  public int getTSAGroupPort() {
    return server.getTSAGroupPort();
  }

  @Override
  public long getUsedMemory() {
    return manager.getMemoryUsage().getUsedMemory();
  }

  @Override
  public long getMaxMemory() {
    return manager.getMemoryUsage().getMaxMemory();
  }

  @Override
  public Map<String, Object> getStatistics() {
    Map<String, Object> map = new HashMap<>();

    map.put(MEMORY_USED, getUsedMemory());
    map.put(MEMORY_MAX, getMaxMemory());

    return map;
  }

  @Override
  public byte[] takeCompressedThreadDump(long requestMillis) {
    return ThreadDumpUtil.getCompressedThreadDump();
  }

  @Override
  public String getEnvironment() {
    return format(System.getProperties());
  }

  @Override
  public String getTCProperties() {
    Properties props = TCPropertiesImpl.getProperties().addAllPropertiesTo(new Properties());
    String keyPrefix = /* TCPropertiesImpl.SYSTEM_PROP_PREFIX */null;
    return format(props, keyPrefix);
  }

  private String format(Properties properties) {
    return format(properties, null);
  }

  private String format(Properties properties, String keyPrefix) {
    StringBuilder sb = new StringBuilder();
    Enumeration<?> keys = properties.propertyNames();
    ArrayList<String> l = new ArrayList<>();

    while (keys.hasMoreElements()) {
      Object o = keys.nextElement();
      if (o instanceof String) {
        String key = (String) o;
        l.add(key);
      }
    }

    String[] props = l.toArray(new String[l.size()]);
    Arrays.sort(props);
    l.clear();
    l.addAll(Arrays.asList(props));

    int maxKeyLen = 0;
    for (String key : l) {
      maxKeyLen = Math.max(key.length(), maxKeyLen);
    }

    for (String key : l) {
      if (keyPrefix != null) {
        sb.append(keyPrefix);
      }
      sb.append(key);
      sb.append(":");
      int spaceLen = maxKeyLen - key.length() + 1;
      for (int i = 0; i < spaceLen; i++) {
        sb.append(" ");
      }
      sb.append(properties.getProperty(key));
      sb.append("\n");
    }

    return sb.toString();
  }

  @Override
  public String[] getProcessArguments() {
    String[] args = server.processArguments();
    List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
    if (args == null) {
      return inputArgs.toArray(new String[inputArgs.size()]);
    } else {
      List<String> l = new ArrayList<>();
      l.add(StringUtil.toString(args, " ", null, null));
      l.addAll(inputArgs);
      return l.toArray(new String[l.size()]);
    }
  }

  @Override
  public String getConfig() {
    return server.getConfig();
  }

  @Override
  public String getHealthStatus() {
    // FIXME: the returned value should eventually contain a true representative status of L2 server.
    // for now just return 'OK' to indicate that the process is up-and-running..
    return "OK";
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    ServerMode cmode = StateManager.convert(sce.getCurrentState());

    if (cmode == ServerMode.ACTIVE) {
      server.updateActivateTime();
    }

    debugPrintln("*****  msg=[" + stateChangeNotificationInfo.getMsg(cmode) + "] attrName=["
                 + stateChangeNotificationInfo.getAttributeName(cmode) + "] attrType=["
                 + stateChangeNotificationInfo.getAttributeType(cmode) + "] stateName=[" + cmode.getName() + "]");

    _sendNotification(stateChangeNotificationInfo.getMsg(cmode), stateChangeNotificationInfo.getAttributeName(cmode),
                      stateChangeNotificationInfo.getAttributeType(cmode), Boolean.FALSE, Boolean.TRUE);
  }

  private synchronized void _sendNotification(String msg, String attr, String type, Object oldVal, Object newVal) {
    sendNotification(new AttributeChangeNotification(this, nextSequenceNumber++, System.currentTimeMillis(), msg, attr,
                                                     type, oldVal, newVal));
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  @Override
  public void gc() {
    ManagementFactory.getMemoryMXBean().gc();
  }

  @Override
  public boolean isVerboseGC() {
    return ManagementFactory.getMemoryMXBean().isVerbose();
  }

  @Override
  public void setVerboseGC(boolean verboseGC) {
    boolean oldValue = isVerboseGC();
    ManagementFactory.getMemoryMXBean().setVerbose(verboseGC);
    _sendNotification("VerboseGC changed", "VerboseGC", "java.lang.Boolean", oldValue, verboseGC);
  }

  @Override
  public final void setPipelineMonitoring(boolean monitor) {
    if (monitor) {
      MonitoringEventCreator.setPipelineMonitor(consumer->{
        logger.info(consumer.toString());
      });
    } else {
      MonitoringEventCreator.setPipelineMonitor(null);
    }
  }
  
  @Override
  public String getResourceState() {
    return server.getResourceState();
  }
}
