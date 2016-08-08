package org.terracotta.passthrough;

import java.util.Collection;

/**
 * An abstraction to expose cluster information to clients for e.g. server configuration etc
 *
 * @author vmad
 */
public interface IClusterInfo {

  /**
   * @param name server name to identify the server
   * @return corresponding {@link IServerInfo}
   */
  IServerInfo getServerInfo(String name);

  /**
   * @return A collection {@link IServerInfo} for all servers in this stripe
   */
  Collection<IServerInfo> getServersInfo();
}
