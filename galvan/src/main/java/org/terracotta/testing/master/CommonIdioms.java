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

import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseLogger;


/**
 * This class is meant to be a container of helpers and testing idioms which are common to different entry-points into the framework.
 * It exists purely to avoid duplication.
 */
public class CommonIdioms {
  public static ReadyStripe setupConfigureAndStartStripe(ITestStateManager stateManager, VerboseLogger logger, ContextualLogger fileHelperLogger, String serverInstallDirectory, String testParentDirectory, int serversToCreate, int serverStartPort, int serverStartNumber, boolean isRestartable, List<String> extraJarPaths, String namespaceFragment, String serviceFragment, String stripeName) throws IOException, FileNotFoundException {
    ContextualLogger stripeLogger = new ContextualLogger(logger, "[" + stripeName + "]");
    // We want to create a sub-directory per-stripe.
    String stripeParentDirectory = FileHelpers.createTempEmptyDirectory(testParentDirectory, stripeName);
    return ReadyStripe.configureAndStartStripe(stateManager, stripeLogger, fileHelperLogger, serverInstallDirectory, stripeParentDirectory, serversToCreate, serverStartPort, serverStartNumber, isRestartable, extraJarPaths, namespaceFragment, serviceFragment);
  }

  public static boolean installAndRunClients(ITestStateManager stateManager, VerboseLogger logger, String testParentDirectory, String clientClassPath, DebugOptions debugOptions, int clientsToCreate, String testClassName, IMultiProcessControl processControl, String connectUri) throws InterruptedException, IOException, FileNotFoundException {
    // All clients use the same entry-point stub.
    String clientClassName = "org.terracotta.testing.client.TestClientStub";
    ContextualLogger clientsLogger = new ContextualLogger(logger, "[Clients]");
    ClientInstaller clientInstaller = new ClientInstaller(clientsLogger, processControl, testParentDirectory, clientClassPath, clientClassName, testClassName, connectUri);
    
    // Run the setup client, synchronously.
    ClientRunner setupClient = clientInstaller.installClient("client_setup", "SETUP", debugOptions.setupClientPort);
    int setupExitValue = CommonIdioms.runClientSynchronous(setupClient);
    
    boolean setupWasClean = (0 == setupExitValue);
    boolean didRunCleanly = true;
    boolean destroyWasClean = true;
    if (setupWasClean) {
      didRunCleanly = CommonIdioms.runTestClientsConcurrently(debugOptions, clientsToCreate, clientInstaller, didRunCleanly);
      if (!didRunCleanly) {
        logger.fatal("ERROR encountered in test client.  Destroy will be attempted but this is a failure");
      }
      
      // Run the destroy client, synchronously.
      ClientRunner destroyClient = clientInstaller.installClient("client_destroy", "DESTROY", debugOptions.destroyClientPort);
      int destroyExitValue = CommonIdioms.runClientSynchronous(destroyClient);
      destroyWasClean = (0 == destroyExitValue);
      if (!destroyWasClean) {
        logger.fatal("ERROR encountered in destroy.  This is a failure");
      }
    } else {
      logger.fatal("FATAL ERROR IN SETUP CLIENT!  Exit code " + setupExitValue + ".  NOT running tests!");
    }
    return setupWasClean && didRunCleanly && destroyWasClean;
  }

  public static int runClientSynchronous(ClientRunner client) throws InterruptedException, IOException {
    client.start();
    client.waitForPid();
    return client.waitForJoinResult();
  }

  public static boolean runTestClientsConcurrently(DebugOptions debugOptions, int clientsToCreate, ClientInstaller clientInstaller, boolean didRunCleanly) throws FileNotFoundException, InterruptedException, IOException {
    // The setup was clean so run the test clients.
    // First, install them.
    ClientRunner[] testClients = new ClientRunner[clientsToCreate];
    for (int i = 0; i < clientsToCreate; ++i) {
      String clientName = "client" + i;
      // Determine if we need a debug port set.
      int debugPort = (0 != debugOptions.testClientsStartPort)
          ? (debugOptions.testClientsStartPort + i)
          : 0;
      testClients[i] = clientInstaller.installClient(clientName, "TEST", debugPort);
    }
    // Now, start them.
    for (int i = 0; i < clientsToCreate; ++i) {
      testClients[i].start();
      testClients[i].waitForPid();
    }
    // Now, wait for them.
    for (int i = 0; i < clientsToCreate; ++i) {
      int result = testClients[i].waitForJoinResult();
      didRunCleanly &= (0 == result);
    }
    return didRunCleanly;
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
}
