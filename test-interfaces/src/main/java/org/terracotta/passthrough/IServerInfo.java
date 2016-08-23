package org.terracotta.passthrough;

/**
 * Provides a server information like configured server port and group port etc
 *
 * @author vmad
 */
public interface IServerInfo {

  /**
   * @return name of this server
   */
  String getName();

  /**
   * @return configured server port for this server
   */
  int getServerPort();

  /**
   * @return configured server group port for this server
   */
  int getGroupPort();
}
