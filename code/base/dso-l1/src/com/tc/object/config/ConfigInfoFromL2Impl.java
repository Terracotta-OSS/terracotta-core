/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import org.apache.xmlbeans.XmlObject;
import org.terracotta.groupConfigForL1.GroupnameId;
import org.terracotta.groupConfigForL1.GroupnameIdMapDocument;
import org.terracotta.groupConfigForL1.ServerGroupsDocument;

import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.core.ConnectionInfo;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.ThreadUtil;
import com.terracottatech.config.L1ReconnectPropertiesDocument;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/*
 * Read the configuration from L2 via Servlet. It loops through L2 server list known to L1 until a successful
 * connection.
 */
public class ConfigInfoFromL2Impl implements ConfigInfoFromL2 {

  private static final TCLogger  logger                                       = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger  consoleLogger                                = CustomerLogging.getConsoleLogger();
  private static final int       MAX_CONNECT_TRIES                            = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getInt(
                                                                                          TCPropertiesConsts.L1_MAX_CONNECT_RETRIES);
  private static final long      RECONNECT_WAIT_INTERVAL                      = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getLong(
                                                                                           TCPropertiesConsts.L1_SOCKET_RECONNECT_WAIT_INTERVAL);
  private static final long      MIN_RETRY_INTERVAL_MILLS                     = 1000;

  public static final String     GROUP_INFO_SERVLET_PATH                      = "/groupinfo";
  public static final String     GROUPID_MAP_SERVLET_PATH                     = "/groupidmap";
  public static final String     L1_RECONNECT_PROPERTIES_FROML2_SERVELET_PATH = "/l1reconnectproperties";

  private final long             connectRetryInterval;
  private final ConnectionInfo[] connections;

  public ConfigInfoFromL2Impl(final L1ConfigurationSetupManager configSetupManager) {
    this(getConnectionsFromL1Config(configSetupManager));
  }

  public ConfigInfoFromL2Impl(final ConnectionInfo[] connections) {
    this.connections = connections;

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
    ConnectionInfoConfig connectInfo = new ConnectionInfoConfig(l2s);
    return connectInfo.getConnectionInfos();
  }

  /*
   * Get Groupname to GroupID mapping from L2. To maintain the GroupID assignment consistency between L2 and L1. L1 gets
   * those from L2. Refer DEV-5463 for why problem occurred.
   * @return Map of Groupname(String) to GroupID
   */
  public Map<String, GroupID> getGroupNameIDMapFromL2() throws ConfigurationSetupException {
    Map<String, GroupID> map = new HashMap<String, GroupID>();

    InputStream in = getPropertiesFromServerViaHttp("Groupname ID Map", GROUPID_MAP_SERVLET_PATH);

    GroupnameIdMapDocument groupnameIdMapDocument = null;
    try {
      groupnameIdMapDocument = new ParseXmlObjectStream<GroupnameIdMapDocument>()
          .parse("l1 properties", in, new FactoryParser<GroupnameIdMapDocument>() {
            public GroupnameIdMapDocument parse(InputStream in2) throws Exception {
              return GroupnameIdMapDocument.Factory.parse(in2);
            }
          });
    } catch (Exception e) {
      consoleLogger.error(e.getMessage());
      logger.error(e.getMessage());
      throw new AssertionError(e);
    }
    GroupnameId[] gids = groupnameIdMapDocument.getGroupnameIdMap().getGroupnameIdArray();
    for (GroupnameId gid : gids) {
      int groupID = gid.getGid().intValue();
      if (groupID <= -1) { throw new ConfigurationSetupException("Wrong group ID " + groupID + " of " + gid.getName()); }
      map.put(gid.getName(), new GroupID(groupID));
    }

    // a little bit verification
    int groupCount = geGroupCount();
    if (map.size() != groupCount) { throw new ConfigurationSetupException("Expect group count " + groupCount
                                                                          + " but see mapping " + map.size()); }

    return map;
  }

  /*
   * Get L1Reconnect properties from L2. To make sure L1 and L2 using the same L1Reconnect properties.
   * @return com.terracottatech.config.L1ReconnectPropertiesDocument defines/generated by terracotta6.xsd
   */
  public L1ReconnectPropertiesDocument getL1ReconnectPropertiesFromL2() throws ConfigurationSetupException {
    InputStream in = getPropertiesFromServerViaHttp("L1 Reconnect Properties",
                                                    L1_RECONNECT_PROPERTIES_FROML2_SERVELET_PATH);

    L1ReconnectPropertiesDocument l1ReconnectPropFromL2 = null;
    try {
      l1ReconnectPropFromL2 = new ParseXmlObjectStream<L1ReconnectPropertiesDocument>()
          .parse("l1 properties", in, new FactoryParser<L1ReconnectPropertiesDocument>() {
            public L1ReconnectPropertiesDocument parse(InputStream in2) throws Exception {
              return L1ReconnectPropertiesDocument.Factory.parse(in2);
            }
          });
    } catch (Exception e) {
      consoleLogger.error(e.getMessage());
      logger.error(e.getMessage());
      throw new AssertionError(e);
    }
    return l1ReconnectPropFromL2;
  }

  /*
   * Get server groups configuration properties from L2. To make sure L2 and L1 using the same L2s.
   * @return org.terracotta.groupConfigForL1.ServerGroupsDocument.ServerGroups defined/generated by terracotta6.xsd
   */
  public ServerGroupsDocument getServerGroupsFromL2() throws ConfigurationSetupException {
    InputStream in = getPropertiesFromServerViaHttp("Cluster topology", GROUP_INFO_SERVLET_PATH);

    ServerGroupsDocument serversGrpDocument = null;
    try {
      serversGrpDocument = new ParseXmlObjectStream<ServerGroupsDocument>()
          .parse("server groups properties", in, new FactoryParser<ServerGroupsDocument>() {
            public ServerGroupsDocument parse(InputStream in2) throws Exception {
              return ServerGroupsDocument.Factory.parse(in2);
            }
          });
    } catch (Exception e) {
      consoleLogger.error(e.getMessage());
      logger.error(e.getMessage());
      throw new AssertionError(e);
    }
    return serversGrpDocument;
  }

  private int geGroupCount() throws ConfigurationSetupException {
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
  private class ParseXmlObjectStream<T extends XmlObject> {

    /*
     * Parse InputStream by parser according to the specified XmlObject type.
     * @param mesg used in logger to know which properties to be handled.
     * @param in InputStream from http servlet connection to L2.
     * @parser a factory method for specified type of XmlObject.
     * @return <T> XmlObject defined/generated by terracotta6.xsd.
     */
    public T parse(final String mesg, final InputStream in, final FactoryParser<T> parser) {
      T document = null;
      try {
        if (in.markSupported()) in.mark(100);
        document = parser.parse(in);
      } catch (Exception e) {
        if (in.markSupported()) {
          byte[] prop = new byte[100];
          int bytesRead = -1;
          try {
            in.reset();
            bytesRead = in.read(prop, 0, prop.length);
          } catch (IOException ioe) {
            throw new AssertionError(e);
          }
          if (bytesRead > 0) {
            logger.error("Error parsing " + mesg + "  from server : " + new String(prop) + "...");
          }
        }
      }
      return document;
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
    InputStream in = null;
    String serverList = "";
    boolean loggedInConsole = false;

    for (ConnectionInfo connection : connections) {
      if (serverList.length() > 0) serverList += ", ";
      serverList += connection;
    }

    String text = "Can't connect to " + (connections.length > 1 ? "any of the servers" : "server") + "[" + serverList
                  + "].";

    int count = 0;
    while (true) {
      count++;
      in = getPropertiesFromL2Stream(message, httpPathExtension);
      if (in != null) { return in; }

      if (loggedInConsole == false) {
        consoleLogger.warn(text + "Retrying... \n");
        loggedInConsole = true;
      }
      if (connections.length > 1) {
        logger.warn(text + "Retrying... \n");
      }

      if (MAX_CONNECT_TRIES > 0 && count >= MAX_CONNECT_TRIES) { throw new ConfigurationSetupException(text); }
      ThreadUtil.reallySleep(connectRetryInterval);
    }
  }

  /*
   * Open an InputStream via http servlet from one of L2s.
   */
  public InputStream getPropertiesFromL2Stream(final String message, final String httpPathExtension) {
    URLConnection connection = null;
    InputStream propFromL2Stream = null;
    URL theURL = null;
    for (int i = 0; i < connections.length; i++) {
      ConnectionInfo ci = connections[i];
      try {
        theURL = new URL("http", ci.getHostname(), ci.getPort(), httpPathExtension);
        String text = "Trying to get " + message + " from " + theURL.toString();
        logger.info(text);
        connection = theURL.openConnection();
        propFromL2Stream = connection.getInputStream();
        if (propFromL2Stream != null) return propFromL2Stream;
      } catch (IOException e) {
        String text = "Can't connect to [" + ci + "].";
        boolean tryAgain = (i < connections.length - 1);
        if (tryAgain) text += " Will retry next server.";
        logger.warn(text);
      }
    }
    return null;
  }

}
