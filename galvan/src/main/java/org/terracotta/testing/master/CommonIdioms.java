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
import java.util.Vector;

import org.terracotta.testing.logging.VerboseManager;


/**
 * This class is meant to be a container of helpers and testing idioms which are common to different entry-points into the framework.
 * It exists purely to avoid duplication.
 */
public class CommonIdioms {
  public static ReadyStripe setupConfigureAndStartStripe(ITestStateManager stateManager, VerboseManager verboseManager, StripeConfiguration stripeConfiguration) throws IOException, FileNotFoundException {
    VerboseManager stripeVerboseManager = verboseManager.createComponentManager("[" + stripeConfiguration.stripeName + "]");
    // We want to create a sub-directory per-stripe.
    String stripeParentDirectory = FileHelpers.createTempEmptyDirectory(stripeConfiguration.testParentDirectory, stripeConfiguration.stripeName);
    return ReadyStripe.configureAndStartStripe(stateManager, stripeVerboseManager, stripeConfiguration.kitOriginPath, stripeParentDirectory, stripeConfiguration.serversToCreate, stripeConfiguration.serverStartPort, stripeConfiguration.serverDebugPortStart, stripeConfiguration.serverStartNumber, stripeConfiguration.isRestartable, stripeConfiguration.extraJarPaths, stripeConfiguration.namespaceFragment, stripeConfiguration.serviceFragment, stripeConfiguration.entityFragment);
  }
  /**
   * Note that the clients will be run in another thread, logging to the given logger and returning their state in stateManager.
   */
  public static void installAndRunClients(ITestStateManager stateManager, VerboseManager verboseManager, ClientsConfiguration clientsConfiguration, IMultiProcessControl processControl) throws InterruptedException, IOException, FileNotFoundException {
    InterruptableClientManager manager = new InterruptableClientManager(stateManager, verboseManager, processControl, clientsConfiguration.testParentDirectory, clientsConfiguration.clientClassPath, clientsConfiguration.setupClientDebugPort, clientsConfiguration.destroyClientDebugPort, clientsConfiguration.testClientDebugPortStart, clientsConfiguration.clientsToCreate, clientsConfiguration.clientArgumentBuilder, clientsConfiguration.connectUri);
    // We want to shut down clients "first".
    stateManager.addComponentToShutDown(manager, true);
    manager.start();
  }

  public static <T> List<T> uniquifyList(List<T> list) {
    Vector<T> newList = new Vector<>();
    for (T element : list) {
      if (!newList.contains(element)) {
        newList.add(element);
      }
    }
    return newList;
  }


  /**
   * This class is essentially a struct containing the data which describes how the servers in the stripe are to be
   * configured, where they are sourced, and how they should be run.
   * It exists to give context to the parameters in CommonIdioms.
   */
  public static class StripeConfiguration {
    public String kitOriginPath;
    public String testParentDirectory;
    public int serversToCreate;
    public int serverStartPort;
    public int serverDebugPortStart;
    public int serverStartNumber;
    public boolean isRestartable;
    public List<String> extraJarPaths;
    public String namespaceFragment;
    public String serviceFragment;
    public String entityFragment;
    public String stripeName;
  }


  /**
   * This class is essentially a struct containing the data which describes how the client for a test are to be
   * configured and how they should be run.
   * It exists to give context to the parameters in CommonIdioms.
   */
  public static class ClientsConfiguration {
    public String testParentDirectory;
    public String clientClassPath;
    public int clientsToCreate;
    public IClientArgumentBuilder clientArgumentBuilder;
    public String connectUri;
    
    // Debug options specific to clients.
    public int setupClientDebugPort;
    public int destroyClientDebugPort;
    public int testClientDebugPortStart;
  }
}
