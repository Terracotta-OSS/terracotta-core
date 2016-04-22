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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.terracotta.testing.api.ITestClusterConfiguration;
import org.terracotta.testing.api.ITestMaster;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseManager;


public abstract class AbstractHarnessEntry<C extends ITestClusterConfiguration> {
  public static final int SERVER_START_PORT = 9000;

  public boolean runTestHarness(EnvironmentOptions environmentOptions, ITestMaster<C> master, DebugOptions debugOptions, VerboseManager verboseManager) throws IOException, FileNotFoundException, InterruptedException {
    boolean didPass = false;
    // We wrap the actual call in a try-catch since the normal SureFire runner discards all exception data and we want to
    // see our assertion failures.
    try {
      didPass = internalRunTestHarness(environmentOptions, master, debugOptions, verboseManager);
    } catch (AssertionError e) {
      e.printStackTrace();
      throw e;
    }
    return didPass;
  }

  private boolean internalRunTestHarness(EnvironmentOptions environmentOptions, ITestMaster<C> master, DebugOptions debugOptions, VerboseManager verboseManager) throws IOException, FileNotFoundException, InterruptedException {
    // Validate the parameters.
    Assert.assertTrue(environmentOptions.isValid());
    
    // Create a logger to describe the test configuration.
    ContextualLogger configurationLogger = verboseManager.createComponentManager("[Configuration]").createHarnessLogger();
    configurationLogger.output("Client class path: " + environmentOptions.clientClassPath);
    configurationLogger.output("Kit installation directory: " + environmentOptions.serverInstallDirectory);
    configurationLogger.output("Test parent directory: " + environmentOptions.testParentDirectory);
    
    // Create a copy of the server installation.
    ContextualLogger fileHelperLogger = verboseManager.createFileHelpersLogger();
    FileHelpers.cleanDirectory(fileHelperLogger, environmentOptions.testParentDirectory);
    // Put together the config for the stripe.
    String testClassName = master.getTestClassName();
    boolean isRestartable = master.isRestartable();
    // Note that we want to uniquify the JAR list since different packaging options might result in the test config
    // redundantly specifying JARs.
    List<String> extraJarPaths = CommonIdioms.uniquifyList(master.getExtraServerJarPaths());
    String namespaceFragment = master.getConfigNamespaceSnippet();
    String serviceFragment = master.getServiceConfigXMLSnippet();
    List<C> runConfigurations = master.getRunConfigurations();
    boolean wasCompleteSuccess = true;
    int clientsToCreate = master.getClientsToStart();
    for (C runConfiguration : runConfigurations) {
      TestStateManager stateManager = new TestStateManager();
      String configurationName = runConfiguration.getName();
      // We want to create a sub-directory per-configuration.
      String configTestDirectory = FileHelpers.createTempEmptyDirectory(environmentOptions.testParentDirectory, configurationName);
      runOneConfiguration(stateManager, verboseManager, environmentOptions.serverInstallDirectory, configTestDirectory, environmentOptions.clientClassPath, debugOptions, clientsToCreate, testClassName, isRestartable, extraJarPaths, namespaceFragment, serviceFragment, runConfiguration);
      boolean runWasSuccess = stateManager.waitForFinish();
      if (!runWasSuccess) {
        wasCompleteSuccess = false;
        break;
      }
    }
    return wasCompleteSuccess;
  }

  // Run the one configuration.
  protected abstract void runOneConfiguration(ITestStateManager stateManager, VerboseManager verboseManager, String kitOriginPath, String configTestDirectory, String clientClassPath, DebugOptions debugOptions, int clientsToCreate, String testClassName, boolean isRestartable, List<String> extraJarPaths, String namespaceFragment, String serviceFragment, C runConfiguration) throws IOException, FileNotFoundException, InterruptedException;
}
