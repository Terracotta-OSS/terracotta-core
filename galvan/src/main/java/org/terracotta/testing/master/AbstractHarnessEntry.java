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
package org.terracotta.testing.master;

import org.terracotta.testing.api.ITestClusterConfiguration;
import org.terracotta.testing.api.ITestMaster;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseManager;
import org.terracotta.utilities.test.net.PortManager;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static org.terracotta.testing.config.ConfigConstants.DEFAULT_SERVER_HEAP_MB;
import static org.terracotta.testing.config.ConfigConstants.DEFAULT_VOTER_COUNT;


public abstract class AbstractHarnessEntry<C extends ITestClusterConfiguration> {

  /*
   * Holds a strong reference to ports assigned through {@link #chooseRandomPortRange}.
   * These ports are released when this {@code AbstractHarnessEntry} becomes weakly-reachable.
   */
  private final List<PortManager.PortRef> assignedPorts = new ArrayList<>(0);

  public void runTestHarness(EnvironmentOptions environmentOptions, ITestMaster<C> master, DebugOptions debugOptions,
                             VerboseManager verboseManager) throws IOException, GalvanFailureException {
    // Before anything, set the default exception handler - since we create threads to manage the sub-processes.
    Thread.setDefaultUncaughtExceptionHandler(new GalvanExceptionHandler());

    // We wrap the actual call in a try-catch since the normal SureFire runner discards all exception data and we want to
    // see our assertion failures.
    try {
      // Note that we will throw GalvanFailureException, on failure.
      internalRunTestHarness(environmentOptions, master, debugOptions, verboseManager);
    } catch (AssertionError e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Allocate the specified number of consecutive TCP ports from a randomly chosen first port.
   * The port range is held allocated until this {@code AbstractHarnessEntry} instance becomes
   * weakly reachable.
   * @deprecated Use {@code org.terracotta.utilities.test.net.PortManager.reservePorts}
   *          in {@code org.terracotta:terracotta-utilities-port-chooser}
   *          taking care to adopt the new usage semantics
   */
  @Deprecated
  public int chooseRandomPortRange(int number) {
    if (number <= 0) {
      throw new IllegalArgumentException("Requested port count must be positive");
    }

    int maxAttempts = 10;
    final PortManager portManager = PortManager.getInstance();
    List<PortManager.PortRef> portRefs = new ArrayList<>();
    synchronized (portManager) {
      for (int attemptCount = 0; attemptCount < maxAttempts && portRefs.size() != number; attemptCount++) {
        PortManager.PortRef portRef = portManager.reservePort();
        if (portRef.port() + number > 65535) {
          // Too close to the end of the allocable range; skip this one and try again
          portRefs.add(portRef);
          continue;
        }

        /*
         * Close and discard any previously obtained PortRefs now.
         */
        portRefs.forEach(ref -> ref.close(EnumSet.of(PortManager.PortRef.CloseOption.NO_RELEASE_CHECK)));
        portRefs.clear();

        portRefs.add(portRef);

        /*
         * Attempt to allocate the next (number - 1) ports following the one we just obtained.
         */
        for (int candidatePort = portRef.port() + 1, i = 1; i < number; candidatePort++, i++) {
          try {
            Optional<PortManager.PortRef> reservation = portManager.reserve(candidatePort);
            if (reservation.isPresent()) {
              portRefs.add(reservation.get());
            } else {
              break;
            }
          } catch (IllegalStateException | IllegalArgumentException e) {
            // "Normal" reasons for failure to reserve a port; abandon this cycle and try again
            break;
          }
        }
      }

      if (portRefs.size() != number) {
        portRefs.forEach(ref -> ref.close(EnumSet.of(PortManager.PortRef.CloseOption.NO_RELEASE_CHECK)));
        portRefs.clear();
        throw new IllegalStateException("Failed to obtain " + number + " consecutive ports within " + maxAttempts + " attempts");
      }

      assignedPorts.addAll(portRefs);
    }

    return portRefs.get(0).port();
  }

  private void internalRunTestHarness(EnvironmentOptions environmentOptions, ITestMaster<C> master, DebugOptions debugOptions,
                                      VerboseManager verboseManager) throws IOException, GalvanFailureException {
    // Validate the parameters.
    Assert.assertTrue(environmentOptions.isValid());

    // Create a logger to describe the test configuration.
    ContextualLogger configurationLogger = verboseManager.createComponentManager("[Configuration]").createHarnessLogger();
    configurationLogger.output("Client class path: " + environmentOptions.clientClassPath);
    configurationLogger.output("Kit installation directory: " + environmentOptions.serverInstallDirectory);
    configurationLogger.output("Test parent directory: " + environmentOptions.testParentDirectory);

    // Create a copy of the server installation.
    ContextualLogger fileHelperLogger = verboseManager.createFileHelpersLogger();
    FileHelpers.ensureDirectoryExists(fileHelperLogger, environmentOptions.testParentDirectory);
    FileHelpers.cleanDirectory(fileHelperLogger, environmentOptions.testParentDirectory);
    // Put together the config for the stripe.
    String testClassName = master.getTestClassName();
    // Note that we want to uniquify the JAR list since different packaging options might result in the test config
    // redundantly specifying JARs.
    // First, however, make sure that there are no nulls in the list (since that implies we failed to look something up).
    Set<Path> extraJarPaths = master.getExtraServerJarPaths();
    for (Path path : extraJarPaths) {
      Assert.assertNotNull(path);
    }
    String namespaceFragment = master.getConfigNamespaceSnippet();
    String serviceFragment = master.getServiceConfigXMLSnippet();
    int clientReconnectWindowTime = master.getClientReconnectWindowTime();
    int failoverPriorityVoterCount = master.getFailoverPriorityVoterCount();
    boolean consistent = master.getConsistentStartup();
    Properties tcProperties = master.getTcProperties();
    Properties serverProperties = master.getServerProperties();
    List<C> runConfigurations = master.getRunConfigurations();
    int clientsToCreate = master.getClientsToStart();
    for (C runConfiguration : runConfigurations) {
      String configurationName = runConfiguration.getName();
      // We want to create a sub-directory per-configuration.
      Path configTestDirectory = FileHelpers.createTempEmptyDirectory(environmentOptions.testParentDirectory, configurationName);

      // Create the common configuration structure.
      CommonHarnessOptions harnessOptions = new CommonHarnessOptions();
      harnessOptions.kitOriginPath = environmentOptions.serverInstallDirectory;
      harnessOptions.configTestDir = configTestDirectory;
      harnessOptions.clientClassPath = environmentOptions.clientClassPath;
      harnessOptions.clientsToCreate = clientsToCreate;
      harnessOptions.failOnLog = true;
      harnessOptions.testClassName = testClassName;
      harnessOptions.errorClassName = master.getClientErrorHandlerClassName();
      harnessOptions.extraJarPaths = extraJarPaths;
      harnessOptions.namespaceFragment = namespaceFragment;
      harnessOptions.serviceFragment = serviceFragment;
      harnessOptions.clientReconnectWindow = clientReconnectWindowTime;
      harnessOptions.voterCount = failoverPriorityVoterCount;
      harnessOptions.consistent = consistent;
      harnessOptions.tcProperties = tcProperties;
      harnessOptions.serverProperties = serverProperties;

      // NOTE:  runOneConfiguration() throws GalvanFailureException on failure.
      runOneConfiguration(verboseManager, debugOptions, harnessOptions, runConfiguration);
    }
  }

  /**
   * Runs a single test configuration.
   *
   * @param verboseManager       A description of the verbose options for the framework and test run.
   * @param debugOptions         The options for any sub-processes which should wait for debugger connections.
   * @param commonHarnessOptions Information describing the resources the harness needs to create sub-processes.
   * @param runConfiguration     The description of the configuration to run.
   * @throws IOException            An error in the test run.
   * @throws GalvanFailureException A failure in the test run.
   */
  protected abstract void runOneConfiguration(VerboseManager verboseManager, DebugOptions debugOptions, CommonHarnessOptions commonHarnessOptions, C runConfiguration) throws IOException, GalvanFailureException;


  /**
   * We install a default uncaught exception handler but we should only ever end up here as a result of a bug in Galvan, proper.
   * We bring down the entire harness process, but could leave stale sub-processes running.
   */
  private static class GalvanExceptionHandler implements UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
      // Log the error.
      System.err.println("XXXXX FATAL GALVAN EXCEPTION IN HARNESS!  TERMINATING PROCESS!  WARNING:  Stale java processes may remain!");
      e.printStackTrace();
      // Bring down the process.
      System.exit(99);
    }
  }


  protected static class CommonHarnessOptions {
    public Path kitOriginPath;
    public Path configTestDir;
    public String clientClassPath;
    public int clientsToCreate;
    public boolean failOnLog;
    public String testClassName;
    public String errorClassName;
    public int serverHeapInM;
    public Set<Path> extraJarPaths;
    public int clientReconnectWindow;
    public int voterCount;
    public boolean consistent;
    public String namespaceFragment;
    public String serviceFragment;
    public Properties tcProperties;
    public Properties serverProperties;

    /**
     * This constructor only exists to set convenient defaults.
     */
    public CommonHarnessOptions() {
      serverHeapInM = DEFAULT_SERVER_HEAP_MB;
      voterCount = DEFAULT_VOTER_COUNT;
    }
  }
}
