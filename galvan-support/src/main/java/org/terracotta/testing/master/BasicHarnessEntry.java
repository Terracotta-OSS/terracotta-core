/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.terracotta.testing.master;

import org.terracotta.testing.api.BasicTestClusterConfiguration;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.config.BasicClientArgumentBuilder;
import org.terracotta.testing.config.ClientsConfiguration;
import org.terracotta.testing.config.ClusterInfo;
import org.terracotta.testing.config.StripeConfiguration;
import org.terracotta.testing.logging.VerboseManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.terracotta.testing.config.ConfigConstants.DEFAULT_SERVER_HEAP_MB;
import org.terracotta.testing.config.DefaultLegacyConfigBuilder;
import org.terracotta.testing.config.DefaultStartupCommandBuilder;
import org.terracotta.testing.config.StartupCommandBuilder;
import org.terracotta.testing.support.PortTool;
import org.terracotta.utilities.test.net.PortManager;

/**
 * The harness entry-point for the harness running {@link BasicTestClusterConfiguration} tests.
 */
public class BasicHarnessEntry extends AbstractHarnessEntry<BasicTestClusterConfiguration> {
  // Run the one configuration.
  @Override
  protected void runOneConfiguration(VerboseManager verboseManager, DebugOptions debugOptions, CommonHarnessOptions harnessOptions,
                                     BasicTestClusterConfiguration runConfiguration) throws IOException, GalvanFailureException {
    int stripeSize = runConfiguration.serversInStripe;
    Assert.assertTrue(stripeSize > 0);

    /*
     * Debug ports, if requested, are reserved from a specified base port, first.  This
     * provides the best chance of allocating a consecutive list of ports without interference
     * from the server and group port reservations.
     */
    PortManager portManager = PortManager.getInstance();
    List<PortManager.PortRef> debugPortRefs = new ArrayList<>();
    List<Integer> serverDebugPorts = new ArrayList<>();
    PortTool.assignDebugPorts(portManager, debugOptions.serverDebugPortStart, stripeSize, debugPortRefs, serverDebugPorts);

    List<PortManager.PortRef> serverPortRefs = portManager.reservePorts(stripeSize);
    List<PortManager.PortRef> groupPortRefs = portManager.reservePorts(stripeSize);

    List<Integer> serverPorts = serverPortRefs.stream().map(PortManager.PortRef::port).collect(toList());
    List<Integer> serverGroupPorts = groupPortRefs.stream().map(PortManager.PortRef::port).collect(toList());
    List<String> serverNames = IntStream.range(0, stripeSize).mapToObj(i -> "testServer" + i).collect(toList());

    String stripeName = "stripe1";
    Path stripeInstallationDir = harnessOptions.configTestDir.resolve(stripeName);
    Files.createDirectories(stripeInstallationDir);

    VerboseManager stripeVerboseManager = verboseManager.createComponentManager("[" + stripeName + "]");

    StripeConfiguration stripeConfig = new StripeConfiguration(serverDebugPorts, serverPorts, serverGroupPorts, serverNames,
        stripeName, "logback-ext.xml", harnessOptions.serverProperties, harnessOptions.tcProperties,
            harnessOptions.clientReconnectWindow, harnessOptions.voterCount, harnessOptions.consistent);

    int serverHeapSize = DEFAULT_SERVER_HEAP_MB;

    TestStateManager stateManager = new TestStateManager();
    StateInterlock interlock = new StateInterlock(verboseManager.createComponentManager("[Interlock]").createHarnessLogger(), stateManager);
    StripeInstaller stripeInstaller = new StripeInstaller(interlock, stateManager, stripeVerboseManager);

    DefaultLegacyConfigBuilder config = new DefaultLegacyConfigBuilder();
    config.withNamespaceFragment(harnessOptions.namespaceFragment);
    config.withServiceFragment(harnessOptions.serviceFragment);
    config.withStripeConfiguration(stripeConfig);
    Path tcConfig = config.createConfig(stripeInstallationDir);

// Configure and install each server in the stripe.
    for (int i = 0; i < stripeSize; ++i) {
      String serverName = stripeConfig.getServerNames().get(i);
      Path serverWorkingDir = stripeInstallationDir.resolve(serverName);
      Path kitLocation = harnessOptions.kitOriginPath;
      Path kitLocationRelative = relativize(serverWorkingDir, kitLocation);
      System.out.println("working: " + serverWorkingDir + "\nkitLocation: " + kitLocation +
              "\nkitLocationRelative:" + kitLocationRelative);
      // Determine if we want a debug port.
      int debugPort = stripeConfig.getServerDebugPorts().get(i);

      Files.createDirectories(serverWorkingDir);
      StartupCommandBuilder builder = new DefaultStartupCommandBuilder()
          .stripeConfiguration(stripeConfig)
          .serverName(serverName)
          .stripeName(stripeName)
          .stripeConfiguration(stripeConfig)
          .stripeWorkingDir(stripeInstallationDir)
          .serverWorkingDir(serverWorkingDir)
          .logConfigExtension("logback-ext.xml");

      if (serverHeapSize <= 0) {
        System.setProperty("com.tc.tc.messages.packup.enabled", "false");
      }

      ServerInstance serverProcess = serverHeapSize > 0 ?
        new ServerProcess(serverName, kitLocation, serverWorkingDir, serverHeapSize, debugPort, harnessOptions.serverProperties, null, builder.build())
              :
        new InlineServer(serverName, kitLocation, serverWorkingDir, harnessOptions.serverProperties, null, builder.build());

      stripeInstaller.installNewServer(serverProcess);
    }
    ReadyStripe oneStripe = ReadyStripe.configureAndStartStripe(interlock, verboseManager, stripeConfig, stripeInstaller);
    // We just want to unwrap this, directly.
    IMultiProcessControl processControl = oneStripe.getStripeControl();
    String connectUri = oneStripe.getStripeUri();
    ClusterInfo clusterInfo = oneStripe.getClusterInfo();
    Assert.assertTrue(null != processControl);
    Assert.assertTrue(null != connectUri);

    // The cluster is now running so install and run the clients.
    BasicClientArgumentBuilder argBuilder = new BasicClientArgumentBuilder(harnessOptions.testClassName, harnessOptions.errorClassName);
    ClientsConfiguration clientsConfiguration = new ClientsConfiguration(harnessOptions.configTestDir, harnessOptions.clientClassPath,
        harnessOptions.clientsToCreate, argBuilder, connectUri, 1, stripeSize, debugOptions.setupClientDebugPort,
        debugOptions.destroyClientDebugPort, debugOptions.testClientDebugPortStart, harnessOptions.failOnLog, clusterInfo);
    VerboseManager clientsVerboseManager = verboseManager.createComponentManager("[Clients]");

    new ClientSubProcessManager(interlock, stateManager, clientsVerboseManager, clientsConfiguration, processControl).start();
    // NOTE:  waitForFinish() throws GalvanFailureException on failure.
    try {
      stateManager.waitForFinish();
    } finally {
      // No matter what happened, shut down the test.
      try {
        interlock.forceShutdown();
      } finally {
        serverPortRefs.forEach(PortManager.PortRef::close);
        groupPortRefs.forEach(PortManager.PortRef::close);
        debugPortRefs.stream().filter(Objects::nonNull).forEach(PortManager.PortRef::close);
      }
    }
  }

  private Path relativize(Path root, Path other) {
    return root.toAbsolutePath().relativize(other.toAbsolutePath());
  }
}
