package org.terracotta.testing.master;

import org.terracotta.testing.common.Assert;

/**
 * @author vmad
 */
public class ServerInfo {
  private static final String DELIM = ",";
  private final String name;
  private final int serverPort;
  private final int groupPort;


  public ServerInfo(String name, int serverPort, int groupPort) {
    this.name = name;
    this.serverPort = serverPort;
    this.groupPort = groupPort;
  }

  public String getName() {
    return name;
  }

  public int getServerPort() {
    return serverPort;
  }

  public int getGroupPort() {
    return groupPort;
  }

  public String encode() {
    return name + DELIM + serverPort + DELIM + groupPort;
  }

  public static ServerInfo decode(String from) {
    String[] tokens = from.split(DELIM);
    Assert.assertTrue(tokens.length == 3);
    return new ServerInfo(tokens[0], Integer.valueOf(tokens[1]), Integer.valueOf(tokens[2]));
  }

}
