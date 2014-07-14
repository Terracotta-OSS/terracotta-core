/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlObject;
import org.terracotta.groupConfigForL1.GroupnameId;
import org.terracotta.groupConfigForL1.GroupnameIdMapDocument;
import org.terracotta.groupConfigForL1.ServerGroupsDocument;
import org.xml.sax.SAXParseException;

import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.core.ConnectionInfo;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.security.PwProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.io.ServerURL;
import com.terracottatech.config.L1ReconnectPropertiesDocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.Map;

/*
 * Read the configuration from L2 via Servlet. It loops through L2 server list known to L1 until a successful
 * connection.
 */
public class ConfigInfoFromL2Impl implements ConfigInfoFromL2 {

  private static final TCLogger  logger                                       = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger  consoleLogger                                = CustomerLogging.getConsoleLogger();
  private static final int       MAX_CONNECT_TRIES                            = -1;
  private static final long      RECONNECT_WAIT_INTERVAL                      = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getLong(TCPropertiesConsts.L1_SOCKET_RECONNECT_WAIT_INTERVAL);
  private static final long      MIN_RETRY_INTERVAL_MILLS                     = 1000;

  public static final String     GROUP_INFO_SERVLET_PATH                      = "/groupinfo";
  public static final String     GROUPID_MAP_SERVLET_PATH                     = "/groupidmap";
  public static final String     L1_RECONNECT_PROPERTIES_FROML2_SERVELET_PATH = "/l1reconnectproperties";

  private final long             connectRetryInterval;
  private final ConnectionInfo[] connections;
  private final PwProvider       pwProvider;

  public ConfigInfoFromL2Impl(final L1ConfigurationSetupManager configSetupManager) {
    this(configSetupManager, null);
  }

  public ConfigInfoFromL2Impl(final L1ConfigurationSetupManager configSetupManager, PwProvider pwProvider) {
    this(getConnectionsFromL1Config(configSetupManager), pwProvider);
  }

  public ConfigInfoFromL2Impl(final ConnectionInfo[] connections, PwProvider pwProvider) {
    this.connections = connections;
    this.pwProvider = pwProvider;

    long interval = RECONNECT_WAIT_INTERVAL;
    if (RECONNECT_WAIT_INTERVAL < MIN_RETRY_INTERVAL_MILLS) {
      logger.info("Setting config connect retry interval to " + MIN_RETRY_INTERVAL_MILLS + " ms");
      interval = MIN_RETRY_INTERVAL_MILLS;
    }
    connectRetryInterval = interval;
  }

  /*
   * Get L2 connection info from L1ConfigurationSetupManager. Made static to be called in constructor.
   * @param configSetupManager L1 configuration setup
   */
  private static ConnectionInfo[] getConnectionsFromL1Config(final L1ConfigurationSetupManager configSetupManager) {
    L2Data[] l2s = null;
    // synchronized here as same issue of MNK-1984. ArrayIndexOutOfBoundsException in multi threaded environment due to
    // apache bug https://issues.apache.org/jira/browse/XMLBEANS-328
    synchronized (configSetupManager) {
      l2s = configSetupManager.l2Config().l2Data();
    }
    // clean groupID which is supported to get from L2.
    for (L2Data l2 : l2s) {
      l2.setGroupId(0);
    }
    ConnectionInfoConfig connectInfo = new ConnectionInfoConfig(l2s, configSetupManager.getSecurityInfo());
    return connectInfo.getConnectionInfos();
  }

  /*
   * Get Groupname to GroupID mapping from L2. To maintain the GroupID assignment consistency between L2 and L1. L1 gets
   * those from L2. Refer DEV-5463 for why problem occurred.
   * @return Map of Groupname(String) to GroupID
   */
  @Override
  public Map<String, GroupID> getGroupNameIDMapFromL2() throws ConfigurationSetupException {
    Map<String, GroupID> map = new HashMap<String, GroupID>();

    GroupnameIdMapDocument groupnameIdMapDocument = getAndParseDocumentFromL2("Groupname ID Map",
                                                                              GROUPID_MAP_SERVLET_PATH,
                                                                              new FactoryParser<GroupnameIdMapDocument>() {
                                                                                @Override
                                                                                public GroupnameIdMapDocument parse(InputStream in2)
                                                                                    throws Exception {
                                                                                  return GroupnameIdMapDocument.Factory
                                                                                      .parse(in2);
                                                                                }
                                                                              });
    for (GroupnameId gid : groupnameIdMapDocument.getGroupnameIdMap().getGroupnameIdArray()) {
      int groupID = gid.getGid().intValue();
      if (groupID <= -1) { throw new ConfigurationSetupException("Wrong group ID " + groupID + " of " + gid.getName()); }
      map.put(gid.getName(), new GroupID(groupID));
    }

    // a little bit verification
    int groupCount = getGroupCount();
    if (map.size() != groupCount) { throw new ConfigurationSetupException("Expect group count " + groupCount
                                                                          + " but see mapping " + map.size()); }

    return map;
  }

  /*
   * Get L1Reconnect properties from L2. To make sure L1 and L2 using the same L1Reconnect properties.
   * @return com.terracottatech.config.L1ReconnectPropertiesDocument defines/generated by terracotta6.xsd
   */
  @Override
  public L1ReconnectPropertiesDocument getL1ReconnectPropertiesFromL2() throws ConfigurationSetupException {
    return getAndParseDocumentFromL2("L1 Reconnect Properties", L1_RECONNECT_PROPERTIES_FROML2_SERVELET_PATH,
                                     new FactoryParser<L1ReconnectPropertiesDocument>() {
                                       @Override
                                       public L1ReconnectPropertiesDocument parse(InputStream in2) throws Exception {
                                         return L1ReconnectPropertiesDocument.Factory.parse(in2);
                                       }
                                     });
  }

  /*
   * Get server groups configuration properties from L2. To make sure L2 and L1 using the same L2s.
   * @return org.terracotta.groupConfigForL1.ServerGroupsDocument.ServerGroups defined/generated by terracotta6.xsd
   */
  @Override
  public ServerGroupsDocument getServerGroupsFromL2() throws ConfigurationSetupException {
    return getAndParseDocumentFromL2("Cluster topology", GROUP_INFO_SERVLET_PATH,
                                     new FactoryParser<ServerGroupsDocument>() {
                                       @Override
                                       public ServerGroupsDocument parse(InputStream in2) throws Exception {
                                         return ServerGroupsDocument.Factory.parse(in2);
                                       }
                                     });
  }

  private <T extends XmlObject> T getAndParseDocumentFromL2(String message, String httpPath,
                                                            FactoryParser<T> factoryParser)
      throws ConfigurationSetupException {
    int tries = 0;
    while (true) {
      InputStream in = getPropertiesFromServerViaHttp(message, httpPath);
      try {
        try {
          return new ParseXmlObjectStream<T>().parse(message, in, factoryParser);
        } catch (SAXParseException e) {
          if (tries++ < 10) {
            logger.warn("Got an XML parse exception retrieving L1 reconnect properties. Retrying...");
            continue;
          } else {
            throw e;
          }
        }
      } catch (Exception e) {
        consoleLogger.error(e.getMessage());
        logger.error(e);
        throw new AssertionError(e);
      }

    }
  }

  private int getGroupCount() throws ConfigurationSetupException {
    return getServerGroupsFromL2().getServerGroups().getServerGroupArray().length;
  }

  /*
   * Wrap a method to be called by ParseXmlObjectStream.parse().
   * @param <T> a class defined/generated by terracotta6.xsd, a factory method to parse InputStream into <T>.
   */
  interface FactoryParser<T extends XmlObject> {
    T parse(InputStream in) throws Exception;
  }

  /*
   * Parse InputStream into an XmlObject document.
   * @param <T> an XmlObject document to be parsed out.
   */
  private static class ParseXmlObjectStream<T extends XmlObject> {

    /*
     * Parse InputStream by parser according to the specified XmlObject type.
     * @param mesg used in logger to know which properties to be handled.
     * @param in InputStream from http servlet connection to L2.
     * @parser a factory method for specified type of XmlObject.
     * @return <T> XmlObject defined/generated by terracotta6.xsd.
     */
    public T parse(final String mesg, final InputStream in, final FactoryParser<T> parser) throws Exception {
      PushbackInputStream pin = new PushbackInputStream(in, 100);
      byte[] prop = new byte[100];
      int propHeadReadLength = pin.read(prop);
      if (propHeadReadLength > 0) {
        pin.unread(prop, 0, propHeadReadLength);
      }

      try {
        return parser.parse(pin);
      } catch (Exception e) {
        if (propHeadReadLength > 0) {
          logger.error("Error parsing " + mesg + "  from server : " + new String(prop, 0, propHeadReadLength) + "...");
        }
        throw e;
      }
    }
  }

  /*
   * Looping through L2 list until a valid connection established or over MAX_CONNECT_TRIES.
   * @param message used only for logger.
   * @httpPathExtension path to the servlet.
   * @return InputStream to connected L2 servlet.
   */
  private InputStream getPropertiesFromServerViaHttp(final String message, final String httpPathExtension)
      throws ConfigurationSetupException {
    StringBuilder text = new StringBuilder("Can't connect to ");
    if (connections.length > 1) text.append("any of the servers [");
    else text.append("server [");

    for (int i = 0; i < connections.length; i++) {
      if (i > 0) text.append(", ");
      text.append(connections[i]);
    }
    text.append("].");

    boolean loggedInConsole = false;
    int count = 0;
    while (true) {
      count++;
      InputStream in = getPropertiesFromL2Stream(message, httpPathExtension);
      if (in != null) { return in; }

      if (loggedInConsole == false) {
        consoleLogger.warn(text + "Retrying... \n");
        loggedInConsole = true;
      }
      if (connections.length > 1) {
        logger.warn(text + "Retrying... \n");
      }

      if (MAX_CONNECT_TRIES > 0 && count >= MAX_CONNECT_TRIES) { throw new ConfigurationSetupException(text.toString()); }
      ThreadUtil.reallySleep(connectRetryInterval);
    }
  }

  /*
   * Open an InputStream via http servlet from one of L2s.
   */
  public InputStream getPropertiesFromL2Stream(final String message, final String httpPathExtension) {
    InputStream propFromL2Stream;
    ServerURL theURL;
    for (int i = 0; i < connections.length; i++) {
      ConnectionInfo ci = connections[i];
      try {
        theURL = new ServerURL(ci.getHostname(), ci.getPort(), httpPathExtension, ci.getSecurityInfo());
        String text = "Trying to get " + message + " from " + theURL.toString();
        logger.info(text);
        propFromL2Stream = theURL.openStream(pwProvider);
        if (propFromL2Stream != null) {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          try {
            IOUtils.copy(propFromL2Stream, baos);
          } finally {
            propFromL2Stream.close();
          }

          return new ByteArrayInputStream(baos.toByteArray());
        }
      } catch (IOException e) {
        if (i < connections.length - 1) {
          logger.warn("Can't connect to [" + ci + "]. Will retry next server.");
        } else {
          logger.warn("Can't connect to [" + ci + "].");
        }
      }
    }
    return null;
  }

}
