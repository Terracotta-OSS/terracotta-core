/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server.util;

import org.apache.commons.io.FileUtils;
import org.apache.xmlbeans.XmlException;
import org.terracotta.license.util.Base64;

import com.tc.cli.CommandLineBuilder;
import com.tc.config.Loader;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class ServerStat {
  private static final String UNKNOWN                 = "unknown";
  private static final String NEWLINE                 = System.getProperty("line.separator");

  static final int            DEFAULT_MANAGEMENT_PORT = 9540;

  private final String        host;
  private final String        hostName;

  private int                 port;
  private boolean             connected               = false;
  private String              groupName               = "UNKNOWN";
  private String              errorMessage            = "";
  private String              state;
  private String              role;
  private String              health;

  public ServerStat(String host, String hostAlias) {
    this.host = host;
    this.hostName = hostAlias;
  }

  public String getState() {
    return state;
  }

  public String getRole() {
    return role;
  }

  public String getHealth() {
    return health;
  }

  /**
   * Finds and returns the name of the group which this server belongs to.
   */
  public String getGroupName() {
    if (!connected) return UNKNOWN;
    return groupName;
  }

  @Override
  public String toString() {
    String serverId = hostName != null ? hostName : host;
    StringBuilder sb = new StringBuilder();
    sb.append(serverId + ".health: " + getHealth()).append(NEWLINE);
    sb.append(serverId + ".role: " + getRole()).append(NEWLINE);
    sb.append(serverId + ".state: " + getState()).append(NEWLINE);
    sb.append(serverId + ".port: " + port).append(NEWLINE);
    sb.append(serverId + ".group name: " + getGroupName()).append(NEWLINE);
    if (!connected) {
      sb.append(serverId + ".error: " + errorMessage).append(NEWLINE);
    }
    return sb.toString();
  }

  public static void main(String[] args) throws Exception {
    String usage = " server-stat -s host1,host2" + NEWLINE + "       server-stat -s host1:9540,host2:9540" + NEWLINE
                   + "       server-stat -f /path/to/tc-config.xml" + NEWLINE;

    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(ServerStat.class.getName(), args);

    commandLineBuilder.addOption("s", true, "Terracotta server instance list (comma separated)", String.class, false,
                                 "list");
    commandLineBuilder.addOption("f", true, "Terracotta tc-config file", String.class, false, "file");
    commandLineBuilder.addOption("h", "help", String.class, false);
    commandLineBuilder.addOption(null, "secured", false, "secured", String.class, false);
    commandLineBuilder.addOption("u", "username", true, "username", String.class, false);
    commandLineBuilder.addOption("w", "password", true, "password", String.class, false);
    commandLineBuilder.setUsageMessage(usage);
    commandLineBuilder.parse();

    if (commandLineBuilder.hasOption('h')) {
      commandLineBuilder.usageAndDie();
    }

    boolean secured = false;
    if (commandLineBuilder.hasOption("secured")) {
      initSecurityManager();
      secured = true;
    }

    String username = null;
    String password = null;
    if (commandLineBuilder.hasOption('u')) {
      username = commandLineBuilder.getOptionValue('u');
      if (commandLineBuilder.hasOption('w')) {
        password = commandLineBuilder.getOptionValue('w');
      } else {
        password = CommandLineBuilder.readPassword();
      }
    }

    String hostList = commandLineBuilder.getOptionValue('s');
    String configFile = commandLineBuilder.getOptionValue('f');

    try {
      if (configFile != null) {
        handleConfigFile(username, password, secured, configFile);
      } else {
        handleList(username, password, secured, hostList);
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private static void initSecurityManager() throws Exception {
    final Class<?> securityManagerClass = Class.forName("com.tc.net.core.security.TCClientSecurityManager");
    securityManagerClass.getConstructor(boolean.class).newInstance(true);
  }

  private static void handleConfigFile(String username, String password, boolean secured, String configFilePath)
      throws Exception {
    TcConfigDocument tcConfigDocument = null;
    try {

      String configFileContent = FileUtils.readFileToString(new File(configFilePath));
      String configFileSubstitutedContent = ParameterSubstituter.substitute(configFileContent);

      tcConfigDocument = new Loader().parse(configFileSubstitutedContent);
    } catch (IOException e) {
      throw new RuntimeException("Error reading " + configFilePath + ": " + e.getMessage());
    } catch (XmlException e) {
      throw new RuntimeException("Error parsing " + configFilePath + ": " + e.getMessage());
    }
    TcConfig tcConfig = tcConfigDocument.getTcConfig();
    Servers tcConfigServers = tcConfig.getServers();
    Server[] servers = L2DSOConfigObject.getServers(tcConfigServers);
    for (Server server : servers) {
      String host = server.getHost();
      if (!secured && tcConfigServers.isSetSecure() && tcConfigServers.getSecure()) {
        initSecurityManager();
        secured = true;
      }

      printStat(username, password, secured, host + ":" + server.getManagementPort().getIntValue(), server.getName());
    }
  }

  private static void handleList(String username, String password, boolean secured, String hostList) {
    if (hostList == null) {
      printStat(username, password, secured, "localhost:" + DEFAULT_MANAGEMENT_PORT, null);
    } else {
      String[] pairs = hostList.split(",");
      for (String info : pairs) {
        printStat(username, password, secured, info, null);
        System.out.println();
      }
    }
  }

  // info = host | host:port
  private static void printStat(String username, String password, boolean secured, String info, String hostAlias) {
    String host = info;
    int port = DEFAULT_MANAGEMENT_PORT;
    if (info.indexOf(':') > 0) {
      String[] args = info.split(":");
      host = args[0];
      try {
        port = Integer.valueOf(args[1]);
      } catch (NumberFormatException e) {
        throw new RuntimeException("Failed to parse port: " + info);
      }
    }

    InputStream myInputStream = null;
    String prefix = secured ? "https" : "http";
    String urlAsString = prefix + "://" + host + ":" + port + "/tc-management-api/v2/local/stat";

    ServerStat stat = new ServerStat(host, hostAlias);
    HttpURLConnection conn = null;
    try {
      URL url = new URL(urlAsString);
      conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("GET");
      String headerValue = username + ":" + password;
      byte[] bytes = headerValue.getBytes("UTF-8");
      String encodeBase64 = Base64.encodeBytes(bytes);
      // Basic auth
      conn.addRequestProperty("Basic", encodeBase64);

      // we send as text/plain , the forceStop attribute, that basically is a boolean
      conn.addRequestProperty("Content-Type", "application/json");
      conn.addRequestProperty("Accept", "*/*");

      myInputStream = conn.getInputStream();
      if (myInputStream != null) {
        String responseContent = toString(myInputStream);
        // { "health" : "OK", "role" : "ACTIVE", "state": "ACTIVE-COORDINATOR", "managementPort" : "9540",
        // "serverGroupName" : "defaultGroup"}
        stat.decodeJsonAndSetFields(responseContent);
        // consoleLogger.debug("Response code is : " + responseCode);
        // consoleLogger.debug("Response content is : " + responseContent);
      }

    } catch (IOException e) {
      stat.errorMessage = "Unexpected error while getting stat: " + e.getMessage();
    } finally {
      conn.disconnect();
    }

    System.out.println(stat.toString());
  }

  void decodeJsonAndSetFields(String responseContent) {
    connected = true;

    String strippedResponseContent = responseContent.replace("{", "");
    strippedResponseContent = strippedResponseContent.replace("}", "");
    String[] splittedFields = strippedResponseContent.split(",");
    for (String jsonKeyValue : splittedFields) {
      String[] keyValue = jsonKeyValue.split(":");
      String key = keyValue[0].trim();
      key = key.replace("\"", "");
      String value = keyValue[1].trim();
      value = value.replace("\"", "");

      if ("health".equals(key)) {
        health = value;
      }
      if ("role".equals(key)) {
        role = value;
      }
      if ("state".equals(key)) {
        state = value;
      }
      if ("managementPort".equals(key)) {
        port = Integer.valueOf(value);
      }
      if ("serverGroupName".equals(key)) {
        groupName = value;
      }
    }
  }

  public static String toString(InputStream stream) throws IOException {
    Writer writer = new StringWriter();
    char[] buffer = new char[1024];
    try {
      Reader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName("UTF-8")));
      int n;
      while ((n = reader.read(buffer)) != -1) {
        writer.write(buffer, 0, n);
      }
    } finally {
      stream.close();
    }
    return writer.toString();
  }

}
