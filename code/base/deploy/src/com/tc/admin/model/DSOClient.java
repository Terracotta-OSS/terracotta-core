/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.admin.ConnectionContext;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.admin.model.IClusterModel.PollScope;
import com.tc.management.beans.L1DumperMBean;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.statistics.StatisticData;
import com.tc.stats.DSOClientMBean;
import com.tc.util.ProductInfo;

import java.beans.PropertyChangeEvent;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipInputStream;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class DSOClient extends BaseClusterNode implements IClient, NotificationListener {
  protected final ConnectionContext   cc;
  private final ObjectName            beanName;
  private ClientID                    clientId;
  private final IClusterModel         clusterModel;
  protected final DSOClientMBean      delegate;
  private long                        channelId;
  private String                      remoteAddress;
  private String                      host;
  private Integer                     port;
  protected ProductVersion            productInfo;
  protected ObjectName                l1InfoBeanName;
  protected ObjectName                instrumentationLoggingBeanName;
  protected ObjectName                runtimeLoggingObjectName;
  protected ObjectName                runtimeOutputOptionsBeanName;
  protected ObjectName                l1DumperBeanName;
  protected ObjectName                l1OperatorEventsBeanName;
  private final AtomicBoolean         ready = new AtomicBoolean();
  private boolean                     isListeningForTunneledBeans;
  private L1InfoMBean                 l1InfoBean;
  private L1DumperMBean               l1DumperBean;
  private InstrumentationLoggingMBean instrumentationLoggingBean;
  private RuntimeLoggingMBean         runtimeLoggingBean;
  private RuntimeOutputOptionsMBean   runtimeOutputOptionsBean;

  public DSOClient(ConnectionContext cc, ObjectName beanName, IClusterModel clusterModel) {
    this.cc = cc;
    this.beanName = beanName;
    this.clusterModel = clusterModel;
    this.delegate = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, beanName, DSOClientMBean.class, true);

    String[] attributes = { "ChannelID", "ClientID", "RemoteAddress", "L1InfoBeanName",
        "InstrumentationLoggingBeanName", "RuntimeLoggingBeanName", "RuntimeOutputOptionsBeanName", "L1DumperBeanName",
        "L1OperatorEventsBeanName" };
    AttributeList attrList;
    try {
      attrList = cc.mbsc.getAttributes(beanName, attributes);
      channelId = ((ChannelID) ((Attribute) attrList.get(0)).getValue()).toLong();
      clientId = (ClientID) ((Attribute) attrList.get(1)).getValue();
      remoteAddress = (String) ((Attribute) attrList.get(2)).getValue();
      l1InfoBeanName = (ObjectName) ((Attribute) attrList.get(3)).getValue();
      instrumentationLoggingBeanName = (ObjectName) ((Attribute) attrList.get(4)).getValue();
      runtimeLoggingObjectName = (ObjectName) ((Attribute) attrList.get(5)).getValue();
      runtimeOutputOptionsBeanName = (ObjectName) ((Attribute) attrList.get(6)).getValue();
      l1DumperBeanName = (ObjectName) ((Attribute) attrList.get(7)).getValue();
      l1OperatorEventsBeanName = (ObjectName) ((Attribute) attrList.get(8)).getValue();
    } catch (Exception e) {
      e.printStackTrace();
    }

    initPolledAttributes();
  }

  public void testSetupTunneledBeans() {
    if (delegate.isTunneledBeansRegistered()) {
      setupTunneledBeans();
    } else {
      startListeningForTunneledBeans();
    }
  }

  private final Map<ObjectName, ObjectName> tunneledBeanNames = new HashMap<ObjectName, ObjectName>();

  public ObjectName getTunneledBeanName(ObjectName on) {
    ObjectName result = tunneledBeanNames.get(on);
    if (result == null) {
      try {
        Hashtable keyPropertyList = new Hashtable(on.getKeyPropertyList());
        keyPropertyList.put("clients", "Clients");
        keyPropertyList.put("node", getRemoteAddress().replace(':', '/'));
        tunneledBeanNames.put(on, result = new ObjectName(on.getDomain(), keyPropertyList));
      } catch (MalformedObjectNameException mone) {
        throw new RuntimeException("Creating ObjectName", mone);
      }
    }
    return result;
  }

  protected void setupTunneledBeans() {
    l1InfoBean = (L1InfoMBean) MBeanServerInvocationHandler.newProxyInstance(cc.mbsc, l1InfoBeanName,
                                                                             L1InfoMBean.class, true);
    addMBeanNotificationListener(l1InfoBeanName, this, "L1InfoMBean");

    instrumentationLoggingBean = (InstrumentationLoggingMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, instrumentationLoggingBeanName, InstrumentationLoggingMBean.class, true);
    addMBeanNotificationListener(instrumentationLoggingBeanName, this, "InstrumentationLoggingMBean");

    runtimeLoggingBean = (RuntimeLoggingMBean) MBeanServerInvocationHandler.newProxyInstance(cc.mbsc,
                                                                                             runtimeLoggingObjectName,
                                                                                             RuntimeLoggingMBean.class,
                                                                                             true);
    addMBeanNotificationListener(runtimeLoggingObjectName, this, "RuntimeLoggingMBean");

    runtimeOutputOptionsBean = (RuntimeOutputOptionsMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, runtimeOutputOptionsBeanName, RuntimeOutputOptionsMBean.class, true);
    addMBeanNotificationListener(runtimeOutputOptionsBeanName, this, "RuntimeOutputOptionsMBean");

    if (l1DumperBeanName != null) {
      l1DumperBean = (L1DumperMBean) MBeanServerInvocationHandler.newProxyInstance(cc.mbsc, l1DumperBeanName,
                                                                                   L1DumperMBean.class, true);
    }

    fireTunneledBeansRegistered();
  }

  private synchronized boolean isListeningForTunneledBeans() {
    return isListeningForTunneledBeans;
  }

  private synchronized void setListeningForTunneledBeans(boolean listening) {
    isListeningForTunneledBeans = listening;
  }

  private void startListeningForTunneledBeans() {
    if (isListeningForTunneledBeans()) { return; }
    addMBeanNotificationListener(beanName, this, "DSOClientMBean");
    setListeningForTunneledBeans(true);
  }

  private void safeRemoveNotificationListener(ObjectName objectName, NotificationListener listener) {
    try {
      cc.removeNotificationListener(objectName, listener);
    } catch (Exception e) {
      /**/
    }
  }

  protected void addMBeanNotificationListener(ObjectName objectName, NotificationListener listener, String beanType) {
    safeRemoveNotificationListener(objectName, listener);
    try {
      cc.addNotificationListener(objectName, listener);
    } catch (Exception e) {
      // throw new RuntimeException("Adding listener to " + beanType, e);
    }
  }

  private void stopListeningForTunneledBeans() {
    if (!isListeningForTunneledBeans()) { return; }
    setListeningForTunneledBeans(false);
    try {
      cc.removeNotificationListener(beanName, this);
    } catch (Exception e) {
      // throw new RuntimeException("Removing listener from DSOClientMBean", e);
    }
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();

    if (DSOClientMBean.TUNNELED_BEANS_REGISTERED.equals(type) && isListeningForTunneledBeans()) {
      stopListeningForTunneledBeans();
      setupTunneledBeans();
    } else if (type.startsWith("tc.logging.")) {
      Boolean newValue = Boolean.valueOf(notification.getMessage());
      Boolean oldValue = Boolean.valueOf(!newValue.booleanValue());
      PropertyChangeEvent pce = new PropertyChangeEvent(this, type, oldValue, newValue);
      propertyChangeSupport.firePropertyChange(pce);
    } else if ("jmx.attribute.change".equals(type)) {
      AttributeChangeNotification acn = (AttributeChangeNotification) notification;
      PropertyChangeEvent pce = new PropertyChangeEvent(this, acn.getAttributeName(), acn.getOldValue(),
                                                        acn.getNewValue());
      propertyChangeSupport.firePropertyChange(pce);
    }
  }

  private void fireTunneledBeansRegistered() {
    PropertyChangeEvent pce = new PropertyChangeEvent(this, DSOClientMBean.TUNNELED_BEANS_REGISTERED, null, null);
    propertyChangeSupport.firePropertyChange(pce);
    setReady(true);
  }

  private void initPolledAttributes() {
    registerPolledAttribute(new PolledAttribute(getL1InfoBeanName(), POLLED_ATTR_CPU_USAGE));
    registerPolledAttribute(new PolledAttribute(getL1InfoBeanName(), POLLED_ATTR_USED_MEMORY));
    registerPolledAttribute(new PolledAttribute(getL1InfoBeanName(), POLLED_ATTR_MAX_MEMORY));
    registerPolledAttribute(new PolledAttribute(getBeanName(), POLLED_ATTR_OBJECT_FLUSH_RATE));
    registerPolledAttribute(new PolledAttribute(getBeanName(), POLLED_ATTR_OBJECT_FAULT_RATE));
    registerPolledAttribute(new PolledAttribute(getBeanName(), POLLED_ATTR_TRANSACTION_RATE));
    registerPolledAttribute(new PolledAttribute(getBeanName(), POLLED_ATTR_PENDING_TRANSACTIONS_COUNT));
    registerPolledAttribute(new PolledAttribute(getBeanName(), POLLED_ATTR_LIVE_OBJECT_COUNT));
  }

  @Override
  public synchronized void addPolledAttributeListener(String name, PolledAttributeListener listener) {
    super.addPolledAttributeListener(name, listener);
    clusterModel.addPolledAttributeListener(PollScope.CLIENTS, name, listener);
  }

  private void setReady(boolean newReady) {
    if (ready.compareAndSet(!newReady, newReady)) {
      if (newReady) {
        initPolledAttributes();
      }
      propertyChangeSupport.firePropertyChange(PROP_READY, !newReady, newReady);
    }
  }

  public boolean isReady() {
    return ready.get();
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  public ObjectName getBeanName() {
    return beanName;
  }

  public ObjectName getL1InfoBeanName() {
    return l1InfoBeanName;
  }

  public boolean isTunneledBeansRegistered() {
    return delegate.isTunneledBeansRegistered();
  }

  public long getChannelID() {
    return channelId;
  }

  public ClientID getClientID() {
    return clientId;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public String getHost() {
    if (host == null) {
      host = "unknown";

      String addr = getRemoteAddress();
      if (addr != null && addr.indexOf(':') != -1) {
        host = addr.substring(0, addr.lastIndexOf(':'));
      }
    }

    return host;
  }

  public int getPort() {
    if (port == null) {
      port = Integer.valueOf(-1);

      String addr = getRemoteAddress();
      if (addr != null && addr.indexOf(":") != -1) {
        try {
          port = new Integer(addr.substring(addr.lastIndexOf(':') + 1));
        } catch (Exception e) {/**/
        }
      }
    }

    return port.intValue();
  }

  @Override
  public String toString() {
    return getRemoteAddress();
  }

  public Number[] getDSOStatistics(String[] names) {
    return delegate.getStatistics(names);
  }

  public void addNotificationListener(NotificationListener listener) throws Exception {
    addNotificationListener(beanName, listener);
  }

  public void addNotificationListener(ObjectName on, NotificationListener listener) throws Exception {
    safeRemoveNotificationListener(on, listener);
    cc.addNotificationListener(on, listener);
  }

  public ObjectName getL1InfoObjectName() {
    return l1InfoBeanName;
  }

  public L1InfoMBean getL1InfoBean() {
    return l1InfoBean;
  }

  public String[] getCpuStatNames() {
    return getL1InfoBean().getCpuStatNames();
  }

  public StatisticData[] getCpuUsage() {
    return getL1InfoBean().getCpuUsage();
  }

  public long getTransactionRate() {
    return delegate.getTransactionRate();
  }

  public ObjectName getInstrumentationLoggingObjectName() {
    return instrumentationLoggingBeanName;
  }

  public InstrumentationLoggingMBean getInstrumentationLoggingBean() {
    return instrumentationLoggingBean;
  }

  public ObjectName getRuntimeLoggingObjectName() {
    return runtimeLoggingObjectName;
  }

  public RuntimeLoggingMBean getRuntimeLoggingBean() {
    return runtimeLoggingBean;
  }

  public ObjectName getRuntimeOutputOptionsObjectName() {
    return runtimeOutputOptionsBeanName;
  }

  public RuntimeOutputOptionsMBean getRuntimeOutputOptionsBean() {
    return runtimeOutputOptionsBean;
  }

  public String takeThreadDump(long requestMillis) {
    if (l1InfoBean == null) { return "not connected"; }
    byte[] zippedByte = l1InfoBean.takeCompressedThreadDump(requestMillis);
    if (zippedByte == null) { return MESSAGE_ON_EXCEPTION; }
    ZipInputStream zIn = new ZipInputStream(new ByteArrayInputStream(zippedByte));
    return decompress(zIn);
  }

  public String takeClusterDump() {
    if (l1DumperBean == null) { return "not connected"; }
    l1DumperBean.doClientDump();
    return "client dump taken";
  }

  public int getLiveObjectCount() {
    return delegate.getLiveObjectCount();
  }

  public boolean isResident(ObjectID oid) {
    return clusterModel.isResidentOnClient(this, oid);
  }

  public void killClient() {
    delegate.killClient();
  }

  /**
   * TODO: Change this to be like the version in com.tc.admin.model.Server. Remove these "positional parameters" and use
   * string keys.
   */
  public synchronized ProductVersion getProductInfo() {
    if (productInfo == null) {
      String[] attributes = { "Version", "MavenArtifactsVersion", "Patched", "PatchLevel", "PatchVersion", "BuildID",
          "Copyright" };
      String version = ProductInfo.UNKNOWN_VALUE;
      String mavenArtifactsVersion = ProductInfo.UNKNOWN_VALUE;
      String patchLevel = ProductInfo.UNKNOWN_VALUE;
      String patchVersion = ProductInfo.UNKNOWN_VALUE;
      String buildID = ProductInfo.UNKNOWN_VALUE;
      String capabilities = ProductInfo.UNKNOWN_VALUE;
      String copyright = ProductInfo.UNKNOWN_VALUE;
      try {
        AttributeList attrList = cc.mbsc.getAttributes(l1InfoBeanName, attributes);
        if (attrList.get(0) != null) {
          version = (String) ((Attribute) attrList.get(0)).getValue();
        }
        if (attrList.get(1) != null) {
          mavenArtifactsVersion = (String) ((Attribute) attrList.get(1)).getValue();
        }
        boolean isPatched = false;
        if (attrList.get(2) != null) {
          isPatched = (Boolean) ((Attribute) attrList.get(2)).getValue();
        }
        if (attrList.get(3) != null) {
          patchLevel = isPatched ? (String) ((Attribute) attrList.get(3)).getValue() : null;
        }
        if (attrList.get(4) != null) {
          patchVersion = (String) ((Attribute) attrList.get(4)).getValue();
        }
        if (attrList.get(5) != null) {
          buildID = (String) ((Attribute) attrList.get(5)).getValue();
        }
        if (attrList.get(6) != null) {
          copyright = (String) ((Attribute) attrList.get(6)).getValue();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      productInfo = new ProductVersion(version, mavenArtifactsVersion, patchLevel, patchVersion, buildID, capabilities,
                                       copyright);
    }
    return productInfo;
  }

  public String getProductVersion() {
    return getProductInfo().version();
  }

  public String getProductPatchLevel() {
    return getProductInfo().patchLevel();
  }

  public String getProductPatchVersion() {
    return getProductInfo().patchVersion();
  }

  public String getProductBuildID() {
    return getProductInfo().buildID();
  }

  public String getProductLicense() {
    return getProductInfo().license();
  }

  public String getProductCopyright() {
    return getProductInfo().copyright();
  }

  public String getConfig() {
    return getL1InfoBean().getConfig();
  }

  public String getEnvironment() {
    return getL1InfoBean().getEnvironment();
  }

  public String getTCProperties() {
    return getL1InfoBean().getTCProperties();
  }

  public String[] getProcessArguments() {
    return getL1InfoBean().getProcessArguments();
  }

  public Map getL1Statistics() {
    return getL1InfoBean().getStatistics();
  }

  /**
   * Cpu usage, Memory usage, Transaction rate.
   * 
   * @see IClusterModel.getPrimaryClientStatistics
   * @see IClusterModel.getPrimaryServerStatistics
   */
  public Map getPrimaryStatistics() {
    Map result = getL1Statistics();
    result.put("TransactionRate", getTransactionRate());
    return result;
  }

  public String dump() {
    StringBuilder sb = new StringBuilder(toString());
    sb.append(" ready: ");
    sb.append(isReady());
    sb.append(" isConnected: ");
    sb.append(cc.isConnected());
    return sb.toString();
  }

  @Override
  public void tearDown() {
    if (!isReady()) {
      stopListeningForTunneledBeans();
    }
    tunneledBeanNames.clear();
    super.tearDown();
  }

  public void gc() {
    getL1InfoBean().gc();
  }

  public boolean isVerboseGC() {
    return getL1InfoBean().isVerboseGC();
  }

  public void setVerboseGC(boolean verboseGC) {
    getL1InfoBean().setVerboseGC(verboseGC);
  }
}
