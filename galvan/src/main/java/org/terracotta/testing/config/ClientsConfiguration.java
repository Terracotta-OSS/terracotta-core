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
package org.terracotta.testing.config;

import org.terracotta.testing.common.Assert;

import java.nio.file.Path;

/**
 * This class is essentially a struct containing the data which describes how the client for a test are to be configured
 * and how they should be run. Additionally, it also provides much of the environment description and other meta-data
 * which tests can use to calibrate themselves.
 */
public class ClientsConfiguration {
  private final Path testParentDirectory;
  private final String clientClassPath;
  private final int clientsToCreate;
  private final IClientArgumentBuilder clientArgumentBuilder;
  private final String connectUri;
  private final int numberOfStripes;
  private final int numberOfServersPerStripe;

  // Debug options specific to clients.
  private final int setupClientDebugPort;
  private final int destroyClientDebugPort;
  private final int testClientDebugPortStart;
  private final boolean failOnLog;
  private final ClusterInfo clusterInfo;

  public ClientsConfiguration(Path testParentDirectory, String clientClassPath, int clientsToCreate,
                              IClientArgumentBuilder clientArgumentBuilder, String connectUri, int numberOfStripes,
                              int numberOfServersPerStripe, int setupClientDebugPort, int destroyClientDebugPort,
                              int testClientDebugPortStart, boolean failOnLog, ClusterInfo clusterInfo) {
    Assert.assertTrue(clientsToCreate > 0);
    Assert.assertTrue(connectUri.length() > 0);
    Assert.assertNotNull(clusterInfo);
    Assert.assertTrue(numberOfStripes > 0);
    Assert.assertTrue(numberOfServersPerStripe > 0);

    this.testParentDirectory = testParentDirectory;
    this.clientClassPath = clientClassPath;
    this.clientsToCreate = clientsToCreate;
    this.clientArgumentBuilder = clientArgumentBuilder;
    this.connectUri = connectUri;
    this.numberOfStripes = numberOfStripes;
    this.numberOfServersPerStripe = numberOfServersPerStripe;
    this.setupClientDebugPort = setupClientDebugPort;
    this.destroyClientDebugPort = destroyClientDebugPort;
    this.testClientDebugPortStart = testClientDebugPortStart;
    this.failOnLog = failOnLog;
    this.clusterInfo = clusterInfo;
  }

  public Path getTestParentDirectory() {
    return testParentDirectory;
  }

  public String getClientClassPath() {
    return clientClassPath;
  }

  public int getClientsToCreate() {
    return clientsToCreate;
  }

  public IClientArgumentBuilder getClientArgumentBuilder() {
    return clientArgumentBuilder;
  }

  public String getConnectUri() {
    return connectUri;
  }

  public int getNumberOfStripes() {
    return numberOfStripes;
  }

  public int getNumberOfServersPerStripe() {
    return numberOfServersPerStripe;
  }

  public int getSetupClientDebugPort() {
    return setupClientDebugPort;
  }

  public int getDestroyClientDebugPort() {
    return destroyClientDebugPort;
  }

  public int getTestClientDebugPortStart() {
    return testClientDebugPortStart;
  }

  public boolean isFailOnLog() {
    return failOnLog;
  }

  public ClusterInfo getClusterInfo() {
    return clusterInfo;
  }
}