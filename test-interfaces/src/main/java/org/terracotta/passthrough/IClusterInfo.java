/*
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
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
