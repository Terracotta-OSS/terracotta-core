/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.testing.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.terracotta.testing.config.ConfigConstants.DEFAULT_CLIENT_RECONNECT_WINDOW;
import static org.terracotta.testing.config.ConfigConstants.DEFAULT_VOTER_COUNT;

public interface ITestMaster<C extends ITestClusterConfiguration> {
  String getConfigNamespaceSnippet();

  String getServiceConfigXMLSnippet();

  /**
   * @return tc properties to be used with server
   */
  default Properties getTcProperties() {
    return new Properties();
  }

  default Properties getServerProperties() {
    return new Properties();
  }

  /**
   * @return client reconnect window time in seconds
   */
  default int getClientReconnectWindowTime() {
    return DEFAULT_CLIENT_RECONNECT_WINDOW;
  }

  default int getFailoverPriorityVoterCount() {
    return DEFAULT_VOTER_COUNT;
  }

  /**
   * @return A list of paths to JARs which must be copied to the server kit being used in the test.
   */
  Set<Path> getExtraServerJarPaths();

  String getTestClassName();

  /**
   * The error class must implement IClientErrorHandler but is optional so null can be returned here.
   *
   * @return The name of the IClientErrorHandler implementation to use in the client.  Null if none desired.
   */
  String getClientErrorHandlerClassName();

  int getClientsToStart();

  /**
   * The test will be run for each of these server-side cluster configurations.
   */
  List<C> getRunConfigurations();
}
