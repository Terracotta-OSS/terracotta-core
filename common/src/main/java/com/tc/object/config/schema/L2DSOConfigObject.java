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
package com.tc.object.config.schema;

import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;

import com.tc.config.schema.ActiveServerGroupsConfigObject;
import com.tc.config.schema.BaseConfigObject;
import com.tc.config.schema.UpdateCheckConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.text.Banner;
import com.tc.util.Assert;
import com.terracottatech.config.Auth;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.DataStorage;
import com.terracottatech.config.DataStorageOffheap;
import com.terracottatech.config.GarbageCollection;
import com.terracottatech.config.Keychain;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.Restartable;
import com.terracottatech.config.Security;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.Ssl;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The standard implementation of {@link L2DSOConfig}.
 */
public class L2DSOConfigObject extends BaseConfigObject implements L2DSOConfig {
  private static final TCLogger   logger                                = TCLogging.getLogger(L2DSOConfigObject.class);
  private static final String     WILDCARD_IP                           = "0.0.0.0";
  private static final String     LOCALHOST                             = "localhost";
  public static final short       DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT        = 10;
  public static final short       DEFAULT_GROUPPORT_OFFSET_FROM_TSAPORT      = 20;
  public static final short       DEFAULT_MANAGEMENTPORT_OFFSET_FROM_TSAPORT = 30;
  public static final int         MIN_PORTNUMBER                        = 0x0FFF;
  public static final int         MAX_PORTNUMBER                        = 0xFFFF;
  public static final String      DEFAULT_DATA_STORAGE_SIZE                 = "2g";

  private final Security          securityConfig;
  private final GarbageCollection garbageCollection;
  private final BindPort          tsaPort;
  private final BindPort          tsaGroupPort;
  private final BindPort          managementPort;
  private final String            host;
  private final String            serverName;
  private final String            bind;
  private final int               clientReconnectWindow;
  private final Restartable       restartable;
  private final DataStorage       dataStorage;
  private volatile boolean        jmxEnabled;

  public L2DSOConfigObject(ConfigContext context, GarbageCollection gc, int clientReconnectWindow,
                           Restartable restartable) {
    super(context);

    this.context.ensureRepositoryProvides(Server.class);
    Server server = (Server) this.context.bean();
    this.garbageCollection = gc;
    this.clientReconnectWindow = clientReconnectWindow;
    this.restartable = restartable;

    this.bind = server.getBind();
    this.host = server.getHost();
    if (this.host.equalsIgnoreCase(LOCALHOST)) {
      logger.warn("The specified hostname \"" + this.host
                  + "\" may not work correctly if clients and operator console are connecting from other hosts. "
                  + "Replace \"" + this.host + "\" with an appropriate hostname in configuration.");
    }
    this.serverName = server.getName();
    this.tsaPort = server.getTsaPort();
    this.tsaGroupPort = server.getTsaGroupPort();
    this.managementPort = server.getManagementPort();
    this.dataStorage = server.getDataStorage();
    this.jmxEnabled = server.getJmxEnabled();
    if (server.isSetSecurity()) {
      this.securityConfig = server.getSecurity();
    } else {
      this.securityConfig = Security.Factory.newInstance();
      this.securityConfig.setSsl(Ssl.Factory.newInstance());
      this.securityConfig.setKeychain(Keychain.Factory.newInstance());
      this.securityConfig.setAuth(Auth.Factory.newInstance());
    }
  }

  @Override
  public void setJmxEnabled(boolean b) {
    this.jmxEnabled = b;
  }

  @Override
  public boolean isJmxEnabled() {
    return jmxEnabled;
  }

  @Override
  public DataStorage getDataStorage() {
    return dataStorage;
  }

  @Override
  public Security securityConfig() {
    return this.securityConfig;
  }

  @Override
  public DataStorageOffheap getOffheap() {
    return dataStorage.getOffheap();
  }

  @Override
  public BindPort tsaPort() {
    return this.tsaPort;
  }

  @Override
  public BindPort tsaGroupPort() {
    return this.tsaGroupPort;
  }

  @Override
  public BindPort managementPort() {
    return this.managementPort;
  }

  @Override
  public Restartable getRestartable() {
    return this.restartable;
  }

  @Override
  public String host() {
    return host;
  }

  @Override
  public String serverName() {
    return this.serverName;
  }

  @Override
  public GarbageCollection garbageCollection() {
    return this.garbageCollection;
  }

  @Override
  public int clientReconnectWindow() {
    return this.clientReconnectWindow;
  }

  @Override
  public String bind() {
    return this.bind;
  }

  public static void initializeServers(TcConfig config, DefaultValueProvider defaultValueProvider,
                                       File directoryLoadedFrom) throws XmlException, ConfigurationSetupException {
    if (!config.isSetServers()) {
      config.addNewServers();
    }

    Servers servers = config.getServers();
    if (servers.getServerArray().length > 0) {
      if (servers.getMirrorGroupArray().length > 0) {
        //
        throw new ConfigurationSetupException("Cannot use both <server> and <mirror-group> at this level");
      }
    }

    ActiveServerGroupsConfigObject.initializeMirrorGroups(servers, defaultValueProvider);
    for (MirrorGroup group : servers.getMirrorGroupArray()) {
      if (group.getServerArray().length == 0) {
        group.addNewServer();
      }
    }

    if (servers.getServerArray() != null && servers.getServerArray().length != 0) {
      // Top level Servers should have been placed in the default mirror-group
      throw new AssertionError("servers are still defined but not in any mirror-group");
    }

    if (!servers.isSetSecure()) {
      servers.setSecure(false);
    }

    initializeClientReconnectWindow(servers, defaultValueProvider);
    initializeRestartable(servers, defaultValueProvider);
    initializeGarbageCollection(servers, defaultValueProvider);

    for (int i = 0; i < servers.sizeOfMirrorGroupArray(); i++) {
      MirrorGroup group = servers.getMirrorGroupArray(i);
      for (int j = 0; j < group.sizeOfServerArray(); j++) {
        Server server = group.getServerArray(j);
        initializeServerBind(server, defaultValueProvider);
        initializeTsaPort(server, defaultValueProvider);
        initializeJmxPort(server, defaultValueProvider);
        initializeTsaGroupPort(server, defaultValueProvider);
        initializeManagementPort(server, defaultValueProvider);
        // CDV-1220: per our documentation in the schema itself, host is supposed to default to server name or '%i'
        // and name is supposed to default to 'host:tsa-port'
        initializeNameAndHost(server, defaultValueProvider);
        initializeDataDirectory(server, defaultValueProvider, directoryLoadedFrom);
        initializeLogsDirectory(server, defaultValueProvider, directoryLoadedFrom);
        initializeDataBackupDirectory(server, defaultValueProvider, directoryLoadedFrom);
        initializeIndexDiretory(server, defaultValueProvider, directoryLoadedFrom);
        initializeSecurity(server, defaultValueProvider);
        initializeDatastore(server, defaultValueProvider);

        if (!server.isSetJmxEnabled()) {
          server.setJmxEnabled(false);
        }
      }
    }

    UpdateCheckConfigObject.initializeUpdateCheck(servers, defaultValueProvider);
  }

  private static void initializeDatastore(final Server server, final DefaultValueProvider defaultValueProvider) throws XmlException {
    if (!server.isSetDataStorage()) {
      server.addNewDataStorage();
      if (server.isSetOffheap()) {
        server.getDataStorage().setSize(server.getOffheap().getMaxDataSize());
      } else {
        CustomerLogging.getConsoleLogger().warn(Banner.makeBanner("Max data size not specified. Using a default of " + DEFAULT_DATA_STORAGE_SIZE, "WARNING"));
        server.getDataStorage().setSize(DEFAULT_DATA_STORAGE_SIZE);
      }
    }

    if (server.isSetOffheap()) {
      if (server.getDataStorage().isSetOffheap()) {
        throw new IllegalStateException("<offheap/> configured in both <datastore/> and <server/> levels");
      }
      server.getDataStorage().addNewOffheap();
      server.getDataStorage().getOffheap().setSize(server.getOffheap().getMaxDataSize());
      server.unsetOffheap();
    }

    if (!server.getDataStorage().isSetOffheap()) {
      server.getDataStorage().addNewOffheap();
      server.getDataStorage().getOffheap().setSize(server.getDataStorage().getSize());
    }
  }

  private static void initializeServerBind(Server server, DefaultValueProvider defaultValueProvider) {
    if (!server.isSetBind() || server.getBind().trim().length() == 0) {
      server.setBind(WILDCARD_IP);
    }
    server.setBind(ParameterSubstituter.substitute(server.getBind()));
  }

  private static void initializeTsaPort(Server server, DefaultValueProvider defaultValueProvider) throws XmlException {
    XmlObject[] tsaPorts = server.selectPath("tsa-port");
    Assert.assertTrue(tsaPorts.length <= 1);
    if (!server.isSetTsaPort()) {
      final XmlInteger defaultValue = (XmlInteger) defaultValueProvider.defaultFor(server.schemaType(), "tsa-port");
      int defaultTsaPort = defaultValue.getBigIntegerValue().intValue();
      BindPort tsaPort = server.addNewTsaPort();
      tsaPort.setIntValue(defaultTsaPort);
      tsaPort.setBind(server.getBind());
    } else if (!server.getTsaPort().isSetBind()) {
      server.getTsaPort().setBind(server.getBind());
    }
  }

  private static void initializeJmxPort(Server server, DefaultValueProvider defaultValueProvider) {
    XmlObject[] jmxPorts = server.selectPath("jmx-port");
    Assert.assertTrue(jmxPorts.length <= 1);
    if (!server.isSetJmxPort()) {
      BindPort jmxPort = server.addNewJmxPort();
      int defaultJmxPort = computeJMXPortFromTSAPort(server.getTsaPort().getIntValue());

      jmxPort.setIntValue(defaultJmxPort);
      jmxPort.setBind(server.getBind());
    } else if (!server.getJmxPort().isSetBind()) {
      server.getJmxPort().setBind(server.getBind());
    }
  }

  private static void initializeManagementPort(Server server, DefaultValueProvider defaultValueProvider) {
    XmlObject[] managementPorts = server.selectPath("management-port");
    Assert.assertTrue(managementPorts.length <= 1);
    if (!server.isSetManagementPort()) {
      BindPort managementPort = server.addNewManagementPort();
      int defaultManagementPort = computeManagementPortFromTSAPort(server.getTsaPort().getIntValue());

      managementPort.setIntValue(defaultManagementPort);
      managementPort.setBind(server.getBind());
    } else if (!server.getManagementPort().isSetBind()) {
      server.getManagementPort().setBind(server.getBind());
    }
  }

  public static int computeJMXPortFromTSAPort(int tsaPort) {
    int tempJmxPort = tsaPort + DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT;
    return ((tempJmxPort <= MAX_PORTNUMBER) ? tempJmxPort : (tempJmxPort % MAX_PORTNUMBER)
                                                                          + MIN_PORTNUMBER);
  }

  public static int computeManagementPortFromTSAPort(int tsaPort) {
    int tempPort = tsaPort + DEFAULT_MANAGEMENTPORT_OFFSET_FROM_TSAPORT;
    return ((tempPort <= MAX_PORTNUMBER) ? tempPort : (tempPort % MAX_PORTNUMBER) + MIN_PORTNUMBER);
  }

  private static void initializeTsaGroupPort(Server server, DefaultValueProvider defaultValueProvider) {
    XmlObject[] tsaGroupPorts = server.selectPath("tsa-group-port");
    Assert.assertTrue(tsaGroupPorts.length <= 1);
    if (!server.isSetTsaGroupPort()) {
      BindPort l2GrpPort = server.addNewTsaGroupPort();
      int tempGroupPort = server.getTsaPort().getIntValue() + DEFAULT_GROUPPORT_OFFSET_FROM_TSAPORT;
      int defaultGroupPort = ((tempGroupPort <= MAX_PORTNUMBER) ? (tempGroupPort) : (tempGroupPort % MAX_PORTNUMBER)
                                                                                    + MIN_PORTNUMBER);
      l2GrpPort.setIntValue(defaultGroupPort);
      l2GrpPort.setBind(server.getBind());
    } else if (!server.getTsaGroupPort().isSetBind()) {
      server.getTsaGroupPort().setBind(server.getBind());
    }
  }

  private static void initializeNameAndHost(Server server, DefaultValueProvider defaultValueProvider) {
    if (!server.isSetHost() || server.getHost().trim().length() == 0) {
      if (!server.isSetName()) {
        server.setHost("%i");
      } else {
        server.setHost(server.getName());
      }
    }

    if (!server.isSetName() || server.getName().trim().length() == 0) {
      int tsaPort = server.getTsaPort().getIntValue();
      server.setName(server.getHost() + (tsaPort > 0 ? ":" + tsaPort : ""));
    }

    // CDV-77: add parameter expansion to the <server> attributes ('host' and 'name')
    server.setHost(ParameterSubstituter.substitute(server.getHost()));
    server.setName(ParameterSubstituter.substitute(server.getName()));
  }

  private static void initializeDataDirectory(Server server, DefaultValueProvider defaultValueProvider,
                                              File directoryLoadedFrom) throws XmlException {
    if (!server.isSetData()) {
      final XmlString defaultValue = (XmlString) defaultValueProvider.defaultFor(server.schemaType(), "data");
      String substitutedString = ParameterSubstituter.substitute(defaultValue.getStringValue());

      server.setData(new File(directoryLoadedFrom, substitutedString).getAbsolutePath());
    } else {
      server.setData(getAbsolutePath(ParameterSubstituter.substitute(server.getData()), directoryLoadedFrom));
    }
  }

  private static void initializeIndexDiretory(Server server, DefaultValueProvider defaultValueProvider,
                                              File directoryLoadedFrom) {
    if (!server.isSetIndex()) {
      Assert.assertTrue(server.isSetData());
      server.setIndex(new File(server.getData(), "index").getAbsolutePath());
    } else {
      server.setIndex(getAbsolutePath(ParameterSubstituter.substitute(server.getIndex()), directoryLoadedFrom));
    }
  }

  private static void initializeLogsDirectory(Server server, DefaultValueProvider defaultValueProvider,
                                              File directoryLoadedFrom) throws XmlException {
    if (!server.isSetLogs()) {
      final XmlString defaultValue = (XmlString) defaultValueProvider.defaultFor(server.schemaType(), "logs");
      String substitutedString = ParameterSubstituter.substitute(defaultValue.getStringValue());
      server.setLogs(new File(directoryLoadedFrom, substitutedString).getAbsolutePath());
    } else {
      server.setLogs(getAbsolutePath(ParameterSubstituter.substitute(server.getLogs()), directoryLoadedFrom));
    }
  }

  private static void initializeDataBackupDirectory(Server server, DefaultValueProvider defaultValueProvider,
                                                    File directoryLoadedFrom) throws XmlException {
    if (!server.isSetDataBackup()) {
      final XmlString defaultValue = (XmlString) defaultValueProvider.defaultFor(server.schemaType(), "data-backup");
      String substitutedString = ParameterSubstituter.substitute(defaultValue.getStringValue());
      server.setDataBackup(new File(directoryLoadedFrom, substitutedString).getAbsolutePath());
    } else {
      server
          .setDataBackup(getAbsolutePath(ParameterSubstituter.substitute(server.getDataBackup()), directoryLoadedFrom));
    }
  }

  private static String getAbsolutePath(String substituted, File directoryLoadedFrom) {
    File out = new File(substituted);
    if (!out.isAbsolute()) {
      out = new File(directoryLoadedFrom, substituted);
    }

    return out.getAbsolutePath();
  }

  private static void initializeClientReconnectWindow(Servers servers, DefaultValueProvider defaultValueProvider)
      throws XmlException {

    if (!servers.isSetClientReconnectWindow()) {
      servers.setClientReconnectWindow(getDefaultReconnectWindow(servers, defaultValueProvider));
    }
  }

  private static void initializeSecurity(Server server, DefaultValueProvider defaultValueProvider) throws XmlException {
    if (server.isSetSecurity()) {
      initializeSsl(server.getSecurity(), defaultValueProvider);
      initializeKeyChain(server.getSecurity(), defaultValueProvider);
      initializeAuth(server.getSecurity(), defaultValueProvider);
    }
  }

  private static void initializeSsl(Security security, DefaultValueProvider defaultValueProvider) {
    security.getSsl().setCertificate(ParameterSubstituter.substitute(security.getSsl().getCertificate()));
  }

  private static void initializeKeyChain(final Security security, final DefaultValueProvider defaultValueProvider)
      throws XmlException {
    final String defaultKeyChainImpl = ((XmlString) defaultValueProvider.defaultFor(security.schemaType(),
                                                                                    "keychain/class")).getStringValue();
    final String defaultKeyChainUrl = ((XmlString) defaultValueProvider.defaultFor(security.schemaType(),
                                                                                    "keychain/url")).getStringValue();
    final String defaultKeyChainSecretProvider = ((XmlString) defaultValueProvider.defaultFor(security.schemaType(),
                                                                                    "keychain/secret-provider")).getStringValue();
    if (!security.isSetKeychain()) {
      Keychain keychain = Keychain.Factory.newInstance();
      security.setKeychain(keychain);
    }

    if (!security.getKeychain().isSetClass1()) {
      security.getKeychain().setClass1(defaultKeyChainImpl);
    }

    if (!security.getKeychain().isSetUrl()) {
      security.getKeychain().setUrl(defaultKeyChainUrl);
    }
    security.getKeychain().setUrl(ParameterSubstituter.substitute(security.getKeychain().getUrl()));

    if (!security.getKeychain().isSetSecretProvider()) {
      security.getKeychain().setSecretProvider(defaultKeyChainSecretProvider);
    }
  }

  private static void initializeAuth(final Security security, final DefaultValueProvider defaultValueProvider)
      throws XmlException {
    final String defaultRealm = ((XmlString) defaultValueProvider.defaultFor(security.schemaType(), "auth/realm"))
        .getStringValue();
    final String defaultUser = ((XmlString) defaultValueProvider.defaultFor(security.schemaType(), "auth/user"))
        .getStringValue();

    if (!security.getAuth().isSetRealm()) {
      security.getAuth().setRealm(defaultRealm);
    }
    if (!security.getAuth().isSetUser()) {
      security.getAuth().setUser(defaultUser);
    }

    security.getAuth().setUrl(ParameterSubstituter.substitute(security.getAuth().getUrl()));
  }

  private static void initializeRestartable(Servers servers, DefaultValueProvider defaultValueProvider) {
    if (!servers.isSetRestartable()) {
      servers.addNewRestartable();
    }

    Restartable restartable = servers.getRestartable();
    Assert.assertNotNull(restartable);
  }

  private static int getDefaultReconnectWindow(Servers servers, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlInteger) defaultValueProvider.defaultFor(servers.schemaType(), "client-reconnect-window"))
        .getBigIntegerValue().intValue();
  }

  private static void initializeGarbageCollection(Servers servers, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    if (!servers.isSetGarbageCollection()) {
      servers.addNewGarbageCollection();
    }

    GarbageCollection gc = servers.getGarbageCollection();
    Assert.assertNotNull(gc);
    if (!gc.isSetEnabled()) {
      gc.setEnabled(getDefaultGarbageCollectionEnabled(servers, defaultValueProvider));
    }

    if (!gc.isSetVerbose()) {
      gc.setVerbose(getDefaultGarbageCollectionVerbose(servers, defaultValueProvider));
    }

    if (!gc.isSetInterval()) {
      gc.setInterval(getDefaultGarbageCollectionInterval(servers, defaultValueProvider));
    }
  }

  private static boolean getDefaultGarbageCollectionEnabled(Servers servers, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(servers.schemaType(), "garbage-collection/enabled"))
        .getBooleanValue();
  }

  private static boolean getDefaultGarbageCollectionVerbose(Servers servers, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(servers.schemaType(), "garbage-collection/verbose"))
        .getBooleanValue();
  }

  private static int getDefaultGarbageCollectionInterval(Servers servers, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlInteger) defaultValueProvider.defaultFor(servers.schemaType(), "garbage-collection/interval"))
        .getBigIntegerValue().intValue();
  }

  public static String[] getServerNames(MirrorGroup mirrorGroup) {
    List<String> names = new ArrayList<String>();

    if (mirrorGroup.getServerArray() != null) {
      for (Server server : mirrorGroup.getServerArray()) {
        names.add(server.getName());
      }
    }

    return names.toArray(new String[names.size()]);
  }

  public static String[] getServerNames(Servers servers) {
    List<String> names = new ArrayList<String>();

    for (Server server : getServers(servers)) {
      names.add(server.getName());
    }

    return names.toArray(new String[names.size()]);
  }

  public static Server[] getServers(Servers servers) {
    List<Server> serverList = new ArrayList<Server>();

    if (servers.getMirrorGroupArray() != null) {
      for (MirrorGroup group : servers.getMirrorGroupArray()) {
        if (group.getServerArray() != null) {
          for (Server server : group.getServerArray()) {
            serverList.add(server);
          }
        }
      }
    }

    if (servers.getServerArray() != null) {
      for (Server server : servers.getServerArray()) {
        serverList.add(server);
      }
    }

    return serverList.toArray(new Server[serverList.size()]);
  }
}
