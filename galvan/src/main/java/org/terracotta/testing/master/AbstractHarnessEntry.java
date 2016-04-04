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
import org.terracotta.testing.logging.VerboseLogger;


public abstract class AbstractHarnessEntry<C extends ITestClusterConfiguration> {
  public static final int SERVER_START_PORT = 9000;

  public boolean runTestHarness(EnvironmentOptions environmentOptions, ITestMaster<C> master, DebugOptions debugOptions, boolean enableVerbose) throws IOException, FileNotFoundException, InterruptedException {
    boolean didPass = false;
    // We wrap the actual call in a try-catch since the normal SureFire runner discards all exception data and we want to
    // see our assertion failures.
    try {
      didPass = internalRunTestHarness(environmentOptions, master, debugOptions, enableVerbose);
    } catch (AssertionError e) {
      e.printStackTrace();
      throw e;
    }
    return didPass;
  }

  private boolean internalRunTestHarness(EnvironmentOptions environmentOptions, ITestMaster<C> master, DebugOptions debugOptions, boolean enableVerbose) throws IOException, FileNotFoundException, InterruptedException {
    // Validate the parameters.
    Assert.assertTrue(environmentOptions.isValid());
    
    VerboseLogger logger = new VerboseLogger(enableVerbose ? System.out : null, System.err);
    // Create a logger to describe the test configuration.
    ContextualLogger configurationLogger = new ContextualLogger(logger, "[Configuration] ");
    configurationLogger.log("Client class path: " + environmentOptions.clientClassPath);
    configurationLogger.log("Kit installation directory: " + environmentOptions.serverInstallDirectory);
    configurationLogger.log("Test parent directory: " + environmentOptions.testParentDirectory);
    
    // Create a copy of the server installation.
    ContextualLogger fileHelperLogger = new ContextualLogger(logger, "[FileHelpers] ");
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
      String configurationName = runConfiguration.getName();
      // We want to create a sub-directory per-configuration.
      String configTestDirectory = FileHelpers.createTempEmptyDirectory(environmentOptions.testParentDirectory, configurationName);
      boolean runWasSuccess = runOneConfiguration(logger, fileHelperLogger, environmentOptions.serverInstallDirectory, configTestDirectory, environmentOptions.clientClassPath, debugOptions, clientsToCreate, testClassName, isRestartable, extraJarPaths, namespaceFragment, serviceFragment, runConfiguration);
      if (!runWasSuccess) {
        wasCompleteSuccess = false;
        break;
      }
    }
    return wasCompleteSuccess;
  }

  // Run the one configuration.
  protected abstract boolean runOneConfiguration(VerboseLogger logger, ContextualLogger fileHelperLogger, String kitOriginPath, String configTestDirectory, String clientClassPath, DebugOptions debugOptions, int clientsToCreate, String testClassName, boolean isRestartable, List<String> extraJarPaths, String namespaceFragment, String serviceFragment, C runConfiguration) throws IOException, FileNotFoundException, InterruptedException;
}
