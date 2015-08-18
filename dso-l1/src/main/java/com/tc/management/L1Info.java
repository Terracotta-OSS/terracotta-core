/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.management;

import com.tc.handler.LockInfoDumpHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.JVMMemoryManager;
import com.tc.runtime.TCRuntime;
import com.tc.util.ProductInfo;
import com.tc.util.StringUtil;
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.LockInfoByThreadIDImpl;
import com.tc.util.runtime.ThreadDumpUtil;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;

public class L1Info extends AbstractTerracottaMBean implements L1InfoMBean {
  private static final TCLogger                logger = TCLogging.getLogger(L1Info.class);

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;
  static {
    final String[] notifTypes = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
    final String name = AttributeChangeNotification.class.getName();
    final String description = "An attribute of this MBean has changed";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }
  private long                                 nextSequenceNumber;

  private final ProductInfo                    productInfo;
  private final String                         buildID;
  private final String                         rawConfigText;
  private final JVMMemoryManager               manager;
  private final TCClient                       client;
  private final LockInfoDumpHandler            lockInfoDumpHandler;

  public L1Info(TCClient client, String rawConfigText) throws NotCompliantMBeanException {
    super(L1InfoMBean.class, true);

    this.productInfo = ProductInfo.getInstance();
    this.buildID = productInfo.buildID();
    this.manager = TCRuntime.getJVMMemoryManager();
    this.rawConfigText = rawConfigText;
    this.client = client;
    this.lockInfoDumpHandler = client;
    this.nextSequenceNumber = 1;
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return Arrays.asList(NOTIFICATION_INFO).toArray(EMPTY_NOTIFICATION_INFO);
  }

  private synchronized void _sendNotification(String msg, String attr, String type, Object oldVal, Object newVal) {
    sendNotification(new AttributeChangeNotification(this, nextSequenceNumber++, System.currentTimeMillis(), msg, attr,
                                                     type, oldVal, newVal));
  }

  // for tests
  public L1Info(LockInfoDumpHandler lockInfoDumpHandler) throws NotCompliantMBeanException {
    super(L1InfoMBean.class, true);
    this.productInfo = ProductInfo.getInstance();
    this.buildID = productInfo.buildID();
    this.rawConfigText = null;
    this.manager = TCRuntime.getJVMMemoryManager();
    this.lockInfoDumpHandler = lockInfoDumpHandler;
    this.client = null;
    this.nextSequenceNumber = 1;
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
    return productInfo.patchLevel();
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
  public String getClientUUID() {
    return client.getUUID();
  }

  @Override
  public String getCopyright() {
    return productInfo.copyright();
  }

  @Override
  public String getMainClassName() {
    return System.getProperty("sun.java.command", "<unknown>");
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
    StringBuffer sb = new StringBuffer();
    Enumeration keys = properties.propertyNames();
    ArrayList<String> l = new ArrayList<String>();

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
    String[] args = client.processArguments();
    List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
    if (args == null) {
      return inputArgs.toArray(new String[inputArgs.size()]);
    } else {
      List<String> l = new ArrayList<String>();
      l.add(StringUtil.toString(args, " ", null, null));
      l.addAll(inputArgs);
      return l.toArray(new String[l.size()]);
    }
  }

  @Override
  public String getConfig() {
    return rawConfigText;
  }

  @Override
  public String takeThreadDump(long requestMillis) {
    LockInfoByThreadID lockInfo = new LockInfoByThreadIDImpl();
    this.lockInfoDumpHandler.addAllLocksTo(lockInfo);
    String text = ThreadDumpUtil.getThreadDump(lockInfo, this.lockInfoDumpHandler.getThreadIDMap());
    logger.info(text);
    return text;
  }

  @Override
  public byte[] takeCompressedThreadDump(long requestMillis) {
    LockInfoByThreadID lockInfo = new LockInfoByThreadIDImpl();
    this.lockInfoDumpHandler.addAllLocksTo(lockInfo);
    return ThreadDumpUtil.getCompressedThreadDump(lockInfo, this.lockInfoDumpHandler.getThreadIDMap());
  }

  @Override
  public Map getStatistics() {
    HashMap map = new HashMap();

    map.put(MEMORY_USED, Long.valueOf(getUsedMemory()));
    map.put(MEMORY_MAX, Long.valueOf(getMaxMemory()));

    return map;
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
  public void reset() {
    /**/
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
}
