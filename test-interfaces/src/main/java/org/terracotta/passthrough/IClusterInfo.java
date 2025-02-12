/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
