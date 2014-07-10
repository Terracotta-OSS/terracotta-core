/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server.util;

import com.tc.cli.CommandLineBuilder;
import com.tc.cli.ManagementToolUtil;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.ConnectException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ServerStat {
  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  private static final int MAX_TRIES = 50;
  private static final int RETRY_INTERVAL = 1000;

  private static final String UNKNOWN                 = "unknown";
  private static final String NEWLINE                 = System.getProperty("line.separator");

  private final String        host;
  private final String        hostName;

  private final int                 port;
  private final boolean             connected;
  private final String              groupName;
  private final String              errorMessage;
  private final String              state;
  private final String              role;
  private final String              health;

  private ServerStat(String host, int port, String error) {
    this.errorMessage = error;
    this.connected = false;
    this.port = port;
    this.groupName = UNKNOWN;
    this.state = UNKNOWN;
    this.role = UNKNOWN;
    this.health = UNKNOWN;
    this.host = host;
    this.hostName = null;
  }

  private ServerStat(String host, String hostAlias, int port, String groupName, String state, String role,
                     String health) {
    this.host = host;
    this.hostName = hostAlias;
    this.port = port;
    this.groupName = groupName;
    this.state = state;
    this.role = role;
    this.health = health;
    this.connected = true;
    this.errorMessage = "";
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

  public String getErrorMessage() {
    return errorMessage;
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

    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(ServerStat.class.getName(),
        replaceServerArg(args));
    ManagementToolUtil.addConnectionOptionsTo(commandLineBuilder);
    commandLineBuilder.addOption("h", "help", String.class, false);
    commandLineBuilder.setUsageMessage(usage);
    commandLineBuilder.parse();

    if (commandLineBuilder.hasOption('h')) {
      commandLineBuilder.usageAndDie();
    }

    for (WebTarget target : ManagementToolUtil.getTargets(commandLineBuilder, true)) {
      System.out.println(getStats(target));
    }
  }

  /**
   * Handle the "-s" for list of servers in the usage. In ManagementToolUtil, "-s" means secured.
   *
   * @param args args from the commandline
   * @return filtered args
   */
  private static String[] replaceServerArg(String[] args) {
    String[] argCopy = new String[args.length];
    for (int i = 0; i < args.length; i++) {
      argCopy[i] = args[i].equals("-s") ? "-servers" : args[i];
    }
    return argCopy;
  }

  public static ServerStat getStats(WebTarget target) throws IOException {
    Response response = null;
    String host = target.getUri().getHost();
    int port = target.getUri().getPort();
    for (int i = 0; i < MAX_TRIES; i++) {
      try {
        response = target.path("/tc-management-api/v2/local/stat").request(MediaType.APPLICATION_JSON_TYPE).get();
      } catch (RuntimeException e) {
        if (getRootCause(e) instanceof ConnectException) {
          return new ServerStat(host, port, "Connection refused to " + host + ":" + port + ". Is the TSA running?");
        } else {
          throw e;
        }
      }

      if (response.getStatus() >= 200 && response.getStatus() < 300) {
        Map<String, String> map = response.readEntity(Map.class);
        return new ServerStat(host, map.get("name"), port, map.get("serverGroupName"), map.get("state"), map.get("role"),
            map.get("health"));
      } else if (response.getStatus() == 401) {
        return new ServerStat(host, port, "Authentication error, check username/password and try again.");
      } else if (response.getStatus() == 404) {
        consoleLogger.debug("Got a 404 getting the server stats. Management service might not be started yet. Trying again.");
        ThreadUtil.reallySleep(RETRY_INTERVAL);
      } else {
        Map<?, ?> errorResponse = response.readEntity(Map.class);
        return new ServerStat(host, port, "Error fetching stats: " + errorResponse.get("error"));
      }
    }
    return new ServerStat(host, port, "Got a 404, is the management server running?");
  }

  private static Throwable getRootCause(Throwable e) {
    Throwable t = e;
    while (t != null) {
      e = t;
      t = t.getCause();
    }
    return e;
  }

  public static ServerStat getStats(String host, int port, String username, String password,
                                    boolean secured, boolean ignoreUntrusted)
      throws KeyManagementException, NoSuchAlgorithmException, IOException {
    return getStats(ManagementToolUtil.targetFor(host, port, username, password, secured, ignoreUntrusted));
  }
}
