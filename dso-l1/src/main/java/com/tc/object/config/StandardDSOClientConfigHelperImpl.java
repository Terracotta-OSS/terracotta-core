/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import org.terracotta.groupConfigForL1.ServerGroup;
import org.terracotta.groupConfigForL1.ServerGroupsDocument.ServerGroups;
import org.terracotta.groupConfigForL1.ServerInfo;

import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.SecurityInfo;
import com.tc.object.LiteralValues;
import com.tc.object.Portability;
import com.tc.object.PortabilityImpl;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.security.PwProvider;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.io.ServerURL;
import com.tc.util.version.Version;
import com.tc.util.version.VersionCompatibility;
import com.terracottatech.config.L1ReconnectPropertiesDocument;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class StandardDSOClientConfigHelperImpl implements DSOClientConfigHelper {

  private static final TCLogger             logger                      = CustomerLogging.getDSOGenericLogger();
  private final L1ConfigurationSetupManager configSetupManager;

  // ====================================================================================================================
  /**
   * The lock for both {@link #userDefinedBootSpecs} and {@link #classSpecs} Maps
   */
  private final Object                      specLock                    = new Object();

  /**
   * A map of class names to TransparencyClassSpec for individual classes
   * 
   * @GuardedBy {@link #specLock}
   */
  private final Map                         classSpecs                  = new HashMap();
  // ====================================================================================================================

  private final Portability                 portability;
  private int                               faultCount                  = -1;
  private final Set<String>                 tunneledMBeanDomains        = Collections
                                                                            .synchronizedSet(new HashSet<String>());
  private ReconnectConfig                   l1ReconnectConfig           = null;
  private static final long                 CONFIGURATION_TOTAL_TIMEOUT = TCPropertiesImpl
                                                                            .getProperties()
                                                                            .getLong(TCPropertiesConsts.TC_CONFIG_TOTAL_TIMEOUT);

  public StandardDSOClientConfigHelperImpl(final boolean initializedModulesOnlyOnce,
                                           final L1ConfigurationSetupManager configSetupManager)
      throws ConfigurationSetupException {
    this(configSetupManager);
  }

  public StandardDSOClientConfigHelperImpl(final L1ConfigurationSetupManager configSetupManager)
      throws ConfigurationSetupException {
    this.portability = new PortabilityImpl(this);
    this.configSetupManager = configSetupManager;

    try {
      doPreInstrumentedAutoconfig();
      doAutoconfig();
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }
  }

  @Override
  public String rawConfigText() {
    return configSetupManager.rawConfigText();
  }

  @Override
  public Portability getPortability() {
    return this.portability;
  }

  @Override
  public CommonL1Config getNewCommonL1Config() {
    return configSetupManager.commonL1Config();
  }

  private void doPreInstrumentedAutoconfig() {
    getOrCreateSpec("com.tcclient.object.DistributedMethodCall");
    markAllSpecsPreInstrumented();
  }

  private void doAutoconfig() throws Exception {
    getOrCreateSpec("java.lang.Object");
  }

  @Override
  public SecurityInfo getSecurityInfo() {
    return configSetupManager.getSecurityInfo();
  }

  private void markAllSpecsPreInstrumented() {
    // Technically, synchronization isn't needed here if this method is only called
    // during construction, in a 1.5 JVM, and if specLock is final, because the JMM guarantees
    // initialization safety w/o synchronization under those conditions
    synchronized (specLock) {
      for (Iterator i = classSpecs.values().iterator(); i.hasNext();) {
        TransparencyClassSpec s = (TransparencyClassSpec) i.next();
        s.markPreInstrumented();
      }
    }
  }

  @Override
  public void setFaultCount(final int count) {
    this.faultCount = count;
  }

  @Override
  public int getFaultCount() {
    return faultCount < 0 ? this.configSetupManager.dsoL1Config().faultCount() : faultCount;
  }

  @Override
  public String getPreCreateMethodIfDefined(final String className) {
    TransparencyClassSpec spec = getSpec(className);
    if (spec != null) {
      return spec.getPreCreateMethod();
    } else {
      return null;
    }
  }

  @Override
  public String getPostCreateMethodIfDefined(final String className) {
    TransparencyClassSpec spec = getSpec(className);
    if (spec != null) {
      return spec.getPostCreateMethod();
    } else {
      return null;
    }
  }

  private TransparencyClassSpec basicGetOrCreateSpec(final String className, final String applicator,
                                                     final boolean rememberSpec) {
    synchronized (specLock) {
      TransparencyClassSpec spec = getSpec(className);
      if (spec == null) {
        if (applicator != null) {
          spec = new TransparencyClassSpecImpl(className, this, applicator);
        } else {
          spec = new TransparencyClassSpecImpl(className, this);
        }
        if (rememberSpec) {
          addSpec(spec);
        }
      }
      return spec;
    }
  }

  @Override
  public TransparencyClassSpec getOrCreateSpec(final String className) {
    return basicGetOrCreateSpec(className, null, true);
  }

  @Override
  public TransparencyClassSpec getOrCreateSpec(final String className, final String applicator) {
    if (applicator == null) throw new AssertionError();
    return basicGetOrCreateSpec(className, applicator, true);
  }

  private void addSpec(final TransparencyClassSpec spec) {
    synchronized (specLock) {
      Assert.eval(!classSpecs.containsKey(spec.getClassName()));
      Assert.assertNotNull(spec);
      classSpecs.put(spec.getClassName(), spec);
    }
  }

  @Override
  public boolean isLogical(final String className) {
    TransparencyClassSpec spec = getSpec(className);
    return spec != null && spec.isLogical();
  }

  @Override
  public Class getChangeApplicator(final Class clazz) {
    ChangeApplicatorSpec applicatorSpec = null;
    TransparencyClassSpec spec = getSpec(clazz.getName());
    if (spec != null) {
      applicatorSpec = spec.getChangeApplicatorSpec();
    }

    if (applicatorSpec == null) { return null; }
    return applicatorSpec.getChangeApplicator(clazz);
  }

  // TODO: Need to optimize this by identifying the module to query instead of querying all the modules.
  @Override
  public boolean isUseNonDefaultConstructor(final Class clazz) {
    String className = clazz.getName();
    if (LiteralValues.isLiteral(className)) { return true; }
    TransparencyClassSpec spec = getSpec(className);
    if (spec != null) { return spec.isUseNonDefaultConstructor(); }
    return false;
  }

  @Override
  public boolean addTunneledMBeanDomain(final String tunneledMBeanDomain) {
    return this.tunneledMBeanDomains.add(tunneledMBeanDomain);
  }

  /**
   * This is used in BootJarTool. In BootJarTool, it changes the package of our implementation of ReentrantLock and
   * FutureTask to the java.util.concurrent package. In order to change the different adapter together, we need to
   * create a spec with our package and remove the spec after the instrumentation is done.
   */
  @Override
  public void removeSpec(String className) {
    className = className.replace('/', '.');
    classSpecs.remove(className);
  }

  @Override
  public TransparencyClassSpec getSpec(String className) {
    synchronized (specLock) {
      // NOTE: This method doesn't create a spec for you. If you want that use getOrCreateSpec()
      className = className.replace('/', '.');
      return (TransparencyClassSpec) classSpecs.get(className);
    }
  }

  @Override
  public TransparencyClassSpec[] getAllSpecs() {
    synchronized (specLock) {
      return (TransparencyClassSpec[]) classSpecs.values().toArray(new TransparencyClassSpec[classSpecs.size()]);
    }
  }

  @Override
  public String toString() {
    return "<StandardDSOClientConfigHelperImpl: " + configSetupManager + ">";
  }

  @Override
  public String getLogicalExtendingClassName(final String className) {
    TransparencyClassSpec spec = getSpec(className);
    if (spec == null || !spec.isLogical()) { return null; }
    return spec.getLogicalExtendingClassName();
  }

  @Override
  public void validateGroupInfo(final PwProvider pwProvider) throws ConfigurationSetupException {
    PreparedComponentsFromL2Connection connectionComponents = new PreparedComponentsFromL2Connection(
                                                                                                     configSetupManager,
                                                                                                     pwProvider);
    ServerGroups serverGroupsFromL2 = new ConfigInfoFromL2Impl(configSetupManager, pwProvider).getServerGroupsFromL2()
        .getServerGroups();

    ConnectionInfoConfig[] connectionInfoItems = connectionComponents.createConnectionInfoConfigItemByGroup();
    HashSet<ConnectionInfo> connInfoFromL1 = new HashSet<ConnectionInfo>();
    for (int i = 0; i < connectionInfoItems.length; i++) {
      ConnectionInfo[] connectionInfo = connectionInfoItems[i].getConnectionInfos();
      for (int j = 0; j < connectionInfo.length; j++) {
        ConnectionInfo connectionIn = new ConnectionInfo(getIpAddressOfServer(connectionInfo[j].getHostname()),
                                                         connectionInfo[j].getPort(), i * j + j,
                                                         connectionInfo[j].getGroupName()); // We don't care about
                                                                                            // security info here
        connInfoFromL1.add(connectionIn);
      }
    }

    HashSet<ConnectionInfo> connInfoFromL2 = new HashSet<ConnectionInfo>();
    ServerGroup[] grpArray = serverGroupsFromL2.getServerGroupArray();
    for (int i = 0; i < grpArray.length; i++) {
      String grpName = grpArray[i].getGroupName();
      ServerInfo[] serverInfos = grpArray[i].getServerInfoArray();
      for (int j = 0; j < serverInfos.length; j++) {
        ConnectionInfo connectionIn = new ConnectionInfo(getIpAddressOfServer(serverInfos[j].getName()), serverInfos[j]
            .getTsaPort().intValue(), i * j + j, grpName); // No security info neither, only carrying to compare the
                                                           // hosts, ports & groups
        connInfoFromL2.add(connectionIn);
      }
    }

    String errMsg = "Client and server configurations don't match.\n";
    if (connInfoFromL1.size() != connInfoFromL2.size()) {
      StringBuilder builder = new StringBuilder();
      builder.append("The number of servers specified in the client and server configs are different. ");
      // dump connInfoFromL1 and connInfoFromL2 for debugging DEV-4769
      dumpConnInfo(builder, "ConnInfo from L1", connInfoFromL1);
      dumpConnInfo(builder, "ConnInfo from L2", connInfoFromL2);
      errMsg += builder.toString();
      throw new ConfigurationSetupException(errMsg);
    }

    /**
     * This check is there because of TC_SERVER env variable
     */
    if (connInfoFromL1.size() == 1) {
      ConnectionInfo[] temp = new ConnectionInfo[1];
      connInfoFromL1.toArray(temp);
      int portFromL1 = temp[0].getPort();
      connInfoFromL2.toArray(temp);
      int portFromL2 = temp[0].getPort();
      if (portFromL1 == portFromL2) {
        return;
      } else {
        logConfigMismatchAndThrowException(connInfoFromL1, connInfoFromL2, errMsg);
      }
    }

    if (!connInfoFromL1.containsAll(connInfoFromL2)) {
      logConfigMismatchAndThrowException(connInfoFromL1, connInfoFromL2, errMsg);
    }
  }

  @Override
  public void validateClientServerCompatibility(PwProvider pwProvider, SecurityInfo securityInfo)
      throws ConfigurationSetupException {
    PreparedComponentsFromL2Connection connectionComponents = new PreparedComponentsFromL2Connection(
                                                                                                     configSetupManager,
                                                                                                     pwProvider);
    ConnectionInfoConfig[] connectionInfoItems = connectionComponents.createConnectionInfoConfigItemByGroup();
    for (int stripeNumber = 0; stripeNumber < connectionInfoItems.length; stripeNumber++) {
      ConnectionInfo[] connectionInfo = connectionInfoItems[stripeNumber].getConnectionInfos();
      boolean foundCompatibleActive = false;
      boolean activeDown = false;
      int serverNumberInStripe = 0;
      long startTime = System.currentTimeMillis();
      long endTime = System.currentTimeMillis();
      // keep looping till we find version of an active server
      // or the timeout occurs
      while ((endTime - startTime) < CONFIGURATION_TOTAL_TIMEOUT) {

        ConnectionInfo connectionIn = new ConnectionInfo(connectionInfo[serverNumberInStripe].getHostname(),
                                                         connectionInfo[serverNumberInStripe].getPort(),
                                                         stripeNumber * serverNumberInStripe + serverNumberInStripe,
                                                         connectionInfo[serverNumberInStripe].getGroupName(),
                                                         connectionInfo[serverNumberInStripe].getSecurityInfo());

        ServerURL serverUrl = null;
        try {
          serverUrl = new ServerURL(connectionIn.getHostname(), connectionIn.getPort(), "/version",
                                    connectionIn.getSecurityInfo());
        } catch (MalformedURLException e) {
          throw new ConfigurationSetupException("Error while trying to verify Client-Server version Compatibility ");
        }

        String strServerVersion = null;
        try {
          strServerVersion = serverUrl.getHeaderField("Version", pwProvider, true);
          activeDown = false;
          logger.info("Server: " + serverUrl + " returned server version = " + strServerVersion);
        } catch (IOException e) {
          // server that we pinged was not up
          // we should try other servers in stripe
          activeDown = true;
          logger.info("Server seems to be down.." + serverUrl + ", retrying next available in stripe");
        }
        if (strServerVersion == null) {
          if (serverNumberInStripe == (connectionInfo.length - 1)) {
            if (activeDown) {
              // active was down and we have reached the end of connectionInfo Array
              // so we need to start checking from 0th index again
              ThreadUtil.reallySleep(500); // sleep for 1 sec before trying again
              serverNumberInStripe = 0;
            } else {
              // active was not down and we have reached end of array
              // we didn't find any compatible active
              foundCompatibleActive = false;
              break;
            }
          } else {
            // we found serverNumberInStripe = null
            // but there are some server left in stripe we should try to get version from them
            serverNumberInStripe++;
          }
          endTime = System.currentTimeMillis();
          continue;
        } else {
          Version serverVersion = new Version(strServerVersion);
          foundCompatibleActive = checkServerClientVersion(serverVersion, serverUrl);
          break;
        }
      }
      if ((endTime - startTime) > CONFIGURATION_TOTAL_TIMEOUT) { throw new ConfigurationSetupException(
                                                                                                       "Timeout occured while trying to get Server Version, No Active server Found for : "
                                                                                                           + CONFIGURATION_TOTAL_TIMEOUT); }
      if (!foundCompatibleActive) {
        if (activeDown) {
          throw new IllegalStateException(
                                          "At least one of the stripes is down, couldn't get the server version for compatibility check!");
        } else {
          throw new IllegalStateException("client Server Version mismatch occured: client version : "
                                          + getClientVersion()
                                          + " is not compatible with a server of Terracotta version: 4.0 or before");
        }
      }
    }
  }

  private boolean checkServerClientVersion(Version serverVersion, ServerURL serverUrl) {
    Version clientVersion = getClientVersion();
    if (!new VersionCompatibility().isCompatibleClientServer(clientVersion, serverVersion)) {
      throw new IllegalStateException("Client-Server versions are incompatible: client version=" + clientVersion
                                      + ", serverVersion=" + serverVersion);
    } else {
      logger.debug("Found Compatible active Server = " + serverUrl);
      return true;
    }
  }

  private Version getClientVersion() {
    return new Version(ProductInfo.getInstance().version());
  }

  private void dumpConnInfo(StringBuilder builder, String mesg, HashSet<ConnectionInfo> connInfo) {
    builder.append(mesg);
    builder.append("[");
    for (ConnectionInfo ci : connInfo) {
      builder.append(ci.toString());
      builder.append(" ");
    }
    builder.append("] ");
  }

  private void logConfigMismatchAndThrowException(final HashSet<ConnectionInfo> connInfoFromL1,
                                                  final HashSet<ConnectionInfo> connInfoFromL2, String errMsg)
      throws ConfigurationSetupException {
    logger.info("L1 connection info: " + connInfoFromL1);
    logger.info("L2 connection info: " + connInfoFromL2);
    errMsg = errMsg
             + "See \"L1 connection info\" and \"L2 connection info\" in the Terracotta log files for more information.";
    throw new ConfigurationSetupException(errMsg);
  }

  private static String getIpAddressOfServer(final String name) throws ConfigurationSetupException {
    InetAddress address;
    try {
      address = InetAddress.getByName(name);
      if (address.isLoopbackAddress()) {
        try {
          address = InetAddress.getLocalHost();
        } catch (ArrayIndexOutOfBoundsException e) {
          // DEV-9391. JDK + OS X Lion issue.
          address = InetAddress.getByName(null);
        }
      }
    } catch (UnknownHostException e) {
      throw new ConfigurationSetupException("Unknown Host Exception!!Could not resolve IpAddress for this hostName: '"
                                            + e.getMessage() + "'");
    }
    return address.getHostAddress();
  }

  private void setupL1ReconnectProperties(PwProvider pwProvider) throws ConfigurationSetupException {

    L1ReconnectPropertiesDocument l1ReconnectPropFromL2 = new ConfigInfoFromL2Impl(configSetupManager, pwProvider)
        .getL1ReconnectPropertiesFromL2();

    boolean l1ReconnectEnabled = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectEnabled();
    int l1ReconnectTimeout = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectTimeout().intValue();
    int l1ReconnectSendqueuecap = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectSendqueuecap()
        .intValue();
    int l1ReconnectMaxdelayedacks = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectMaxDelayedAcks()
        .intValue();
    int l1ReconnectSendwindow = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectSendwindow().intValue();
    this.l1ReconnectConfig = new L1ReconnectConfigImpl(l1ReconnectEnabled, l1ReconnectTimeout, l1ReconnectSendqueuecap,
                                                       l1ReconnectMaxdelayedacks, l1ReconnectSendwindow);
  }

  @Override
  public synchronized ReconnectConfig getL1ReconnectProperties(final PwProvider securityManager)
      throws ConfigurationSetupException {
    if (l1ReconnectConfig == null) {
      setupL1ReconnectProperties(securityManager);
    }
    return l1ReconnectConfig;
  }

  @Override
  public boolean useResolveLockWhenClearing(final Class clazz) {
    // If this condition ever needs to be true for any other classes besides ConcurrentHashMap, this setting should be
    // move into the TransparencyClassSpec (as opposed to growing the list of classes here)
    return !clazz.getName().equals("java.util.concurrent.ConcurrentHashMap");
  }

  @Override
  public String[] getTunneledDomains() {
    synchronized (tunneledMBeanDomains) {
      String[] result = new String[tunneledMBeanDomains.size()];
      tunneledMBeanDomains.toArray(result);
      return result;
    }
  }

  @Override
  public L1ConfigurationSetupManager reloadServersConfiguration() throws ConfigurationSetupException {
    configSetupManager.reloadServersConfiguration();
    return configSetupManager;
  }

  @Override
  public String[] processArguments() {
    return configSetupManager.processArguments();
  }
}
