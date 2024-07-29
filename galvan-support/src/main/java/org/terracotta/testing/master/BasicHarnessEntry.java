/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import org.terracotta.testing.config.TcConfigBuilder;
import org.terracotta.testing.logging.VerboseManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.terracotta.testing.config.ConfigConstants.DEFAULT_SERVER_HEAP_MB;
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
    Files.createDirectory(stripeInstallationDir);

    Path tcConfig = createTcConfig(serverNames, serverPorts, serverGroupPorts, stripeInstallationDir, harnessOptions);
    VerboseManager stripeVerboseManager = verboseManager.createComponentManager("[" + stripeName + "]");

    StripeConfiguration stripeConfig = new StripeConfiguration(serverDebugPorts, serverPorts, serverGroupPorts, serverNames,
        stripeName, DEFAULT_SERVER_HEAP_MB, "logback-ext.xml", harnessOptions.serverProperties);
    TestStateManager stateManager = new TestStateManager();
    StateInterlock interlock = new StateInterlock(verboseManager.createComponentManager("[Interlock]").createHarnessLogger(), stateManager);
    StripeInstaller stripeInstaller = new StripeInstaller(interlock, stateManager, stripeVerboseManager, stripeConfig);
    // Configure and install each server in the stripe.
    for (int i = 0; i < stripeSize; ++i) {
      String serverName = stripeConfig.getServerNames().get(i);
      Path serverWorkingDir = stripeInstallationDir.resolve(serverName);
      Path tcConfigRelative = relativize(serverWorkingDir, tcConfig);
      Path kitLocationRelative = relativize(serverWorkingDir, harnessOptions.kitOriginPath);
      // Determine if we want a debug port.
      int debugPort = stripeConfig.getServerDebugPorts().get(i);
      StartupCommandBuilder builder = new DefaultStartupCommandBuilder()
          .tcConfig(tcConfigRelative)
          .serverName(serverName)
          .stripeName(stripeName)
          .serverWorkingDir(serverWorkingDir)
          .kitDir(kitLocationRelative)
          .logConfigExtension("logback-ext.xml")
          .consistentStartup(false);

      stripeInstaller.installNewServer(serverName, serverWorkingDir, debugPort, builder::build);
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

  private Path createTcConfig(List<String> serverNames, List<Integer> serverPorts, List<Integer> serverGroupPorts,
                              Path stripeInstallationDir, CommonHarnessOptions harnessOpts) {
    TcConfigBuilder configBuilder = new TcConfigBuilder(stripeInstallationDir, serverNames, serverPorts, serverGroupPorts, harnessOpts.tcProperties,
        harnessOpts.namespaceFragment, harnessOpts.serviceFragment, harnessOpts.clientReconnectWindow, harnessOpts.voterCount);
    String tcConfig = configBuilder.build();
    try {
      Path tcConfigPath = Files.createFile(stripeInstallationDir.resolve("tc-config.xml"));
      Files.write(tcConfigPath, tcConfig.getBytes(UTF_8));
      return tcConfigPath;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
