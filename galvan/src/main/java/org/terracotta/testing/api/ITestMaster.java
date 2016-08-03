/*
 * Copyright Terracotta, Inc.
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

import java.util.List;


public interface ITestMaster<C extends ITestClusterConfiguration> {
  public String getConfigNamespaceSnippet();

  public String getServiceConfigXMLSnippet();

  public String getEntityConfigXMLSnippet();

  /**
   * @return A list of paths to JARs which must be copied to the server kit being used in the test.
   */
  public List<String> getExtraServerJarPaths();

  public String getTestClassName();

  /**
   * The error class must implement IClientErrorHandler but is optional so null can be returned here.
   * 
   * @return The name of the IClientErrorHandler implementation to use in the client.  Null if none desired.
   */
  public String getClientErrorHandlerClassName();

  public int getClientsToStart();

  public boolean isRestartable();

  /**
   * The test will be run for each of these server-side cluster configurations.
   */
  public List<C> getRunConfigurations();
}
