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
package org.terracotta.testing.master;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

import org.terracotta.testing.api.ITestClusterConfiguration;
import org.terracotta.testing.api.ITestMaster;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.common.PortChooser;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseManager;


public abstract class AbstractHarnessEntry<C extends ITestClusterConfiguration> {
  private final PortChooser chooser = new PortChooser();
  
  public void runTestHarness(EnvironmentOptions environmentOptions, ITestMaster<C> master, DebugOptions debugOptions, VerboseManager verboseManager) throws IOException, GalvanFailureException {
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
  
  public int chooseRandomPort() {
    return chooser.chooseRandomPort();
  }
  
  public int chooseRandomPortRange(int number) {
    return chooser.chooseRandomPorts(number);
  }

  private void internalRunTestHarness(EnvironmentOptions environmentOptions, ITestMaster<C> master, DebugOptions debugOptions, VerboseManager verboseManager) throws IOException, GalvanFailureException {
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
    boolean isRestartable = master.isRestartable();
    // Note that we want to uniquify the JAR list since different packaging options might result in the test config
    // redundantly specifying JARs.
    List<String> extraJarPaths = CommonIdioms.uniquifyList(master.getExtraServerJarPaths());
    String namespaceFragment = master.getConfigNamespaceSnippet();
    String serviceFragment = master.getServiceConfigXMLSnippet();
    String entityFragment = master.getEntityConfigXMLSnippet();
    List<C> runConfigurations = master.getRunConfigurations();
    int clientsToCreate = master.getClientsToStart();
    for (C runConfiguration : runConfigurations) {
      String configurationName = runConfiguration.getName();
      // We want to create a sub-directory per-configuration.
      String configTestDirectory = FileHelpers.createTempEmptyDirectory(environmentOptions.testParentDirectory, configurationName);
      
      // Create the common configuration structure.
      CommonHarnessOptions harnessOptions = new CommonHarnessOptions();
      harnessOptions.kitOriginPath = environmentOptions.serverInstallDirectory;
      harnessOptions.configTestDirectory = configTestDirectory;
      harnessOptions.clientClassPath = environmentOptions.clientClassPath;
      harnessOptions.clientsToCreate = clientsToCreate;
      harnessOptions.testClassName = testClassName;
      harnessOptions.errorClassName = master.getClientErrorHandlerClassName();
      harnessOptions.isRestartable = isRestartable;
      harnessOptions.extraJarPaths = extraJarPaths;
      harnessOptions.namespaceFragment = namespaceFragment;
      harnessOptions.serviceFragment = serviceFragment;
      harnessOptions.entityFragment = entityFragment;
      
      // NOTE:  runOneConfiguration() throws GalvanFailureException on failure.
      runOneConfiguration(verboseManager, debugOptions, harnessOptions, runConfiguration);
    }
  }

  /**
   * Runs a single test configuration.
   * 
   * @param verboseManager A description of the verbose options for the framework and test run.
   * @param debugOptions The options for any sub-processes which should wait for debugger connections.
   * @param commonHarnessOptions Information describing the resources the harness needs to create sub-processes.
   * @param runConfiguration The description of the configuration to run.
   * @throws IOException An error in the test run.
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
    public String kitOriginPath;
    public String configTestDirectory;
    public String clientClassPath;
    public int clientsToCreate;
    public String testClassName;
    public String errorClassName;
    public int serverHeapInM;
    public boolean isRestartable;
    public List<String> extraJarPaths;
    public String namespaceFragment;
    public String serviceFragment;
    public String entityFragment;
    
    /**
     * This constructor only exists to set convenient defaults.
     */
    public CommonHarnessOptions() {
      // By default, we will configure a server with a 128M heap.
      this.serverHeapInM = 128;
    }
  }
}
