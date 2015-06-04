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
package com.terracotta.management.service.impl.util;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.TopologyReloadStatus;
import com.tc.license.LicenseManager;
import com.tc.license.ProductID;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.TCLoggingBroadcasterMBean;
import com.tc.management.beans.object.EnterpriseTCServerMbean;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.objectserver.api.GCStats;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.stats.api.DSOMBean;
import com.tc.util.Conversion;
import com.terracotta.management.web.utils.TSAConfig;
import org.terracotta.license.LicenseConstants;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipInputStream;

/**
 * @author Ludovic Orban
 */
public class LocalManagementSource {

  private static final int ZIP_BUFFER_SIZE = 2048;

  private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
  private final String localServerName;
  private final ObjectName dsoObjectName;
  private final ObjectName internalTerracottaServerObjectName;
  private final TCServerInfoMBean tcServerInfoMBean;
  private final TCLoggingBroadcasterMBean tcLoggingBroadcaster;
  private final DSOMBean dsoMBean;
  private final ObjectManagementMonitorMBean objectManagementMonitorMBean;
  private final L2DumperMBean l2DumperMBean;
  private final EnterpriseTCServerMbean enterpriseTCServerMbean;


  public LocalManagementSource() {
    try {
      dsoObjectName = new ObjectName("org.terracotta:type=Terracotta Server,name=DSO");
      internalTerracottaServerObjectName = new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server");
      tcServerInfoMBean = JMX.newMBeanProxy(mBeanServer, internalTerracottaServerObjectName, TCServerInfoMBean.class);
      tcLoggingBroadcaster = JMX.newMBeanProxy(mBeanServer, new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Logger"), TCLoggingBroadcasterMBean.class);
      dsoMBean = JMX.newMBeanProxy(mBeanServer, dsoObjectName, DSOMBean.class);
      objectManagementMonitorMBean = JMX.newMBeanProxy(mBeanServer, new ObjectName("org.terracotta:type=Terracotta Server,subsystem=Object Management,name=ObjectManagement"), ObjectManagementMonitorMBean.class);
      l2DumperMBean = JMX.newMBeanProxy(mBeanServer, new ObjectName("org.terracotta.internal:type=Terracotta Server,name=L2Dumper"), L2DumperMBean.class);
      if (isEnterpriseEdition()) {
        enterpriseTCServerMbean = JMX.newMBeanProxy(mBeanServer, new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Enterprise Terracotta Server"), EnterpriseTCServerMbean.class);
      } else {
        enterpriseTCServerMbean = null;
      }

      localServerName = (String)mBeanServer.getAttribute(internalTerracottaServerObjectName, "L2Identifier");
    } catch (JMException e) {
      throw new ManagementSourceException(e);
    }
  }

  public String getLocalServerName() {
    return localServerName;
  }

  public String getVersion() {
    return this.getClass().getPackage().getImplementationVersion();
  }

  public Map<String, String> getRemoteServerUrls() throws ManagementSourceException {
    Map<String, String> serverUrls = getServerUrls();
    serverUrls.remove(getLocalServerName());
    return serverUrls;
  }

  public Map<String, String> getServerUrls() throws ManagementSourceException {
    try {
      L2Info[] l2Infos = (L2Info[])mBeanServer.getAttribute(internalTerracottaServerObjectName, "L2Info");
      Map<String, String> result = new HashMap<String, String>();

      for (L2Info l2Info : l2Infos) {
        String name = l2Info.name();
        String host = l2Info.host();
        int managementPort = l2Info.managementPort();
        boolean sslEnabled = TSAConfig.isSslEnabled();
        String urlPrefix = (sslEnabled ? "https://" : "http://") + host + ":" + managementPort;
        result.put(name, urlPrefix);
      }

      return result;
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public boolean isLegacyProductionModeEnabled() {
    return tcServerInfoMBean.isLegacyProductionModeEnabled();
  }

  public boolean isEnterpriseEdition() throws ManagementSourceException {
    try {
      mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Enterprise Terracotta Server"), "Enabled");
      return true;
    } catch (InstanceNotFoundException infe) {
      return false;
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public ServerGroupInfo[] getServerGroupInfos() throws ManagementSourceException {
    try {
      return (ServerGroupInfo[])mBeanServer.getAttribute(internalTerracottaServerObjectName, "ServerGroupInfo");
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public L2Info[] getL2Infos() throws ManagementSourceException {
    try {
      return (L2Info[])mBeanServer.getAttribute(internalTerracottaServerObjectName, "L2Info");
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public String serverThreadDump() throws ManagementSourceException {
    try {
      byte[] bytes = tcServerInfoMBean.takeCompressedThreadDump(10000L);
      return unzipThreadDump(bytes);
    } catch (IOException ioe) {
      throw new ManagementSourceException(ioe);
    }
  }

  private String unzipThreadDump(byte[] bytes) throws IOException {
    ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
    zis.getNextEntry();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[ZIP_BUFFER_SIZE];

    while (true) {
      int read = zis.read(buffer);
      if (read == -1) { break; }
      baos.write(buffer, 0, read);
    }

    zis.close();
    baos.close();

    byte[] uncompressedBytes = baos.toByteArray();
    return Conversion.bytes2String(uncompressedBytes);
  }


  public Map<String, Object> getServerAttributes(String[] attributeNames) throws ManagementSourceException {
    try {
      Map<String, Object> result = new HashMap<String, Object>();

      AttributeList attributes = mBeanServer.getAttributes(internalTerracottaServerObjectName, attributeNames);
      for (Object attributeObj : attributes) {
        Attribute attribute = (Attribute)attributeObj;
        result.put(attribute.getName(), attribute.getValue());
      }

      return result;
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public Map<String, Object> getDsoAttributes(String[] attributeNames) throws ManagementSourceException {
    try {
      Map<String, Object> result = new HashMap<String, Object>();

      AttributeList attributes = mBeanServer.getAttributes(dsoObjectName, attributeNames);
      for (Object attributeObj : attributes) {
        Attribute attribute = (Attribute)attributeObj;
        result.put(attribute.getName(), attribute.getValue());
      }

      return result;
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public GCStats[] getGcStats() throws ManagementSourceException {
    try {
      return (GCStats[])mBeanServer.getAttribute(dsoObjectName, "GarbageCollectorStats");
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public Map<String, Object> getServerInfoAttributes() throws ManagementSourceException {
    Map<String, Object> result = new HashMap<String, Object>();

    result.put("tcProperties", tcServerInfoMBean.getTCProperties());
    result.put("config", tcServerInfoMBean.getConfig());
    result.put("environment", tcServerInfoMBean.getEnvironment());
    result.put("processArguments", tcServerInfoMBean.getProcessArguments());

    return result;
  }

  public boolean isActiveCoordinator() throws ManagementSourceException {
    return "ACTIVE-COORDINATOR".equals(tcServerInfoMBean.getState());
  }

  public boolean containsJmxMBeans() {
    // the active of groupId 0 is always the one where the MBeans are tunneled to
    // see: com.tc.net.OrderedGroupIDs.getActiveCoordinatorGroup()
    return isActiveCoordinator() && isLocalGroupCoordinator();
  }


  private boolean isLocalGroupCoordinator() {
    String localServerName = getLocalServerName();
    ServerGroupInfo[] serverGroupInfos = getServerGroupInfos();
    for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
      L2Info[] members = serverGroupInfo.members();
      for (L2Info member : members) {
        if (member.name().equals(localServerName)) {
          return serverGroupInfo.isCoordinator();
        }
      }
    }
    throw new ManagementSourceException("Cannot find local server group in topology data structure");
  }


  public Map<String, String> getBackupStatuses() throws ManagementSourceException {
    try {
      return tcServerInfoMBean.getBackupStatuses();
    } catch (IOException ioe) {
      throw new ManagementSourceException(ioe);
    }
  }

  public String getBackupFailureReason(String backupName) throws ManagementSourceException {
    try {
      return tcServerInfoMBean.getBackupFailureReason(backupName);
    } catch (IOException ioe) {
      throw new ManagementSourceException(ioe);
    }
  }

  public Collection<Notification> getNotifications(Long sinceWhen) throws ManagementSourceException {
    List<Notification> logNotifications;
    if (sinceWhen == null) {
      logNotifications = tcLoggingBroadcaster.getLogNotifications();
    } else {
      logNotifications = tcLoggingBroadcaster.getLogNotificationsSince(sinceWhen);
    }
    return logNotifications;
  }

  public Collection<TerracottaOperatorEvent> getOperatorEvents(Long sinceWhen) throws ManagementSourceException {
    List<TerracottaOperatorEvent> operatorEvents;
    if (sinceWhen == null) {
      operatorEvents = dsoMBean.getOperatorEvents();
    } else {
      operatorEvents = dsoMBean.getOperatorEvents(sinceWhen);
    }
    return operatorEvents;
  }

  public Set<ObjectName> queryNames(String query) throws ManagementSourceException {
    try {
      if (query == null) {
        query = "*:*";
      }
      return mBeanServer.queryNames(new ObjectName(query), null);
    } catch (MalformedObjectNameException mone) {
      throw new ManagementSourceException(mone);
    }
  }

  public Map<String, String> getMBeanAttributeInfo(ObjectName objectName) throws ManagementSourceException {
    try {
      Map<String, String> result = new HashMap<String, String>();
      MBeanAttributeInfo[] attributes = mBeanServer.getMBeanInfo(objectName).getAttributes();
      for (MBeanAttributeInfo attribute : attributes) {
        result.put(attribute.getName(), attribute.getType());
      }
      return result;
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public Map<String, Integer> getUnreadOperatorEventCount() throws ManagementSourceException {
    return dsoMBean.getUnreadOperatorEventCount();
  }

  /**
   * When expectedProductIds is null, only non-internal product ID client object names are returned.
   */
  public Collection<ObjectName> fetchClientObjectNames(Set<ProductID> expectedProductIds) throws ManagementSourceException {
    try {
      ObjectName[] objectNames = (ObjectName[])mBeanServer.getAttribute(dsoObjectName, "Clients");

      List<ObjectName> result = new ArrayList<ObjectName>();
      for (ObjectName objectName : objectNames) {
        String productIdName = objectName.getKeyProperty("productId");

        if (expectedProductIds == null && !ProductID.valueOf(productIdName).isInternal()) {
          result.add(objectName);
        }
        if (expectedProductIds != null && expectedProductIds.contains(ProductID.valueOf(productIdName))) {
          result.add(objectName);
        }
      }
      return result;
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public String getClientID(ObjectName clientObjectName) throws ManagementSourceException {
    try {
      return mBeanServer.getAttribute(clientObjectName, "ClientID").toString();
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  private L1InfoMBean getL1InfoMBean(ObjectName clientObjectName) throws ManagementSourceException {
    try {
      ObjectName l1InfoObjectName = (ObjectName)mBeanServer.getAttribute(clientObjectName, "L1InfoBeanName");
      return JMX.newMBeanProxy(mBeanServer, l1InfoObjectName, L1InfoMBean.class);
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public String clientThreadDump(ObjectName clientObjectName) throws ManagementSourceException {
    try {
      L1InfoMBean l1InfoMBean = getL1InfoMBean(clientObjectName);
      byte[] bytes = l1InfoMBean.takeCompressedThreadDump(10000L);
      return unzipThreadDump(bytes);
    } catch (IOException ioe) {
      throw new ManagementSourceException(ioe);
    }
  }

  public Map<String, Object> getClientAttributes(ObjectName clientObjectName) throws ManagementSourceException {
    try {
      ObjectName l1InfoObjectName = (ObjectName) mBeanServer.getAttribute(clientObjectName, "L1InfoBeanName");
      Map<String, Object> result = getMBeanAttributes(l1InfoObjectName, new String[] { "Version", "BuildID",
          "ClientUUID", "MavenArtifactsVersion" });

      result.put("RemoteAddress", mBeanServer.getAttribute(clientObjectName, "RemoteAddress"));
      result.put("ClientID", mBeanServer.getAttribute(clientObjectName, "ClientID").toString());

      return result;
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public Map<String, Object> getClientStatistics(String clientId, String[] attributeNames) throws ManagementSourceException {
    try {
      Set<ObjectName> objectNames = mBeanServer.queryNames(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO,channelID=" + clientId + ",productId=*"), null);
      if (objectNames.size() != 1) {
        throw new ManagementSourceException("there should only be 1 client at org.terracotta:type=Terracotta Server,name=DSO,channelID=" + clientId + ",productId=*");
      }
      ObjectName clientObjectName = objectNames.iterator().next();
      return getMBeanAttributes(clientObjectName, attributeNames);
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  private Map<String, Object> getMBeanAttributes(ObjectName objectName, String[] attributeNames)
      throws ManagementSourceException {
    try {
      Map<String, Object> result = new HashMap<String, Object>();
      AttributeList attributes = mBeanServer.getAttributes(objectName, attributeNames);
      for (Object attributeObj : attributes) {
        Attribute attribute = (Attribute) attributeObj;
        result.put(attribute.getName(), attribute.getValue());
      }

      return result;
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public Map<String, Object> getClientConfig(ObjectName clientObjectName) throws ManagementSourceException {
    try {

      ObjectName l1InfoObjectName = (ObjectName)mBeanServer.getAttribute(clientObjectName, "L1InfoBeanName");
      Map<String, Object> result = getMBeanAttributes(l1InfoObjectName, new String[] { "TCProperties", "Config",
          "Environment", "ProcessArguments" });

      result.put("tcProperties", result.remove("TCProperties"));
      result.put("config", result.remove("Config"));
      result.put("environment", result.remove("Environment"));
      result.put("processArguments", result.remove("ProcessArguments"));

      return result;
    } catch (JMException jme) {
      throw new ManagementSourceException(jme);
    }
  }

  public void runDgc() throws ManagementSourceException {
    if (!isActiveCoordinator()) {
      return;
    }
    objectManagementMonitorMBean.runGC();
  }

  public void dumpClusterState() {
    l2DumperMBean.dumpClusterState();
  }

  public void backup(String backupName) throws ManagementSourceException {
    if (!isActiveCoordinator()) {
      return;
    }
    try {
      tcServerInfoMBean.backup(backupName);
    } catch (IOException ioe) {
      throw new ManagementSourceException(ioe);
    }
  }

  public String getBackupStatus(String backupName) throws ManagementSourceException {
    try {
      return tcServerInfoMBean.getBackupStatus(backupName);
    } catch (IOException ioe) {
      throw new ManagementSourceException(ioe);
    }
  }

  public void shutdownServer() throws ManagementSourceException {
    tcServerInfoMBean.shutdown();
  }

  public boolean markOperatorEvent(TerracottaOperatorEvent terracottaOperatorEvent, boolean read) throws ManagementSourceException {
    return dsoMBean.markOperatorEvent(terracottaOperatorEvent, read);
  }

  public TopologyReloadStatus reloadConfiguration() throws ManagementSourceException {
    if (enterpriseTCServerMbean == null) {
      throw new NotSupportedFeatureException("Configuration reloading is only possible with the Enterprise product.");
    }
    try {
      return enterpriseTCServerMbean.reloadConfiguration();
    } catch (ConfigurationSetupException e) {
      throw new ManagementSourceException(e);
    }
  }

  /**
   * Returns license properties of this L2 without the signature
   */
  public Properties getLicenseProperties() {
    try {
      Properties licenseProperties = new Properties();
      // make a copy of license properties
      licenseProperties.putAll(LicenseManager.getLicense().getProperties());

      // then remove the signature
      licenseProperties.remove(LicenseConstants.LICENSE_SIGNATURE);
      return licenseProperties;
    } catch (Exception e) {
      throw new ManagementSourceException(e);
    }
  }
}
