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

import org.terracotta.passthrough.Assert;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseLogger;


/**
 * A container of much of the client operation logic for a test.
 * Note that the current state of this class is a stop-gap to transition to a different design which will do asynchronous
 * client execution and management.
 */
public class InterruptableClientManager {
  private final VerboseLogger logger;
  private final String testParentDirectory;
  private final String clientClassPath;
  private final DebugOptions debugOptions;
  private final int clientsToCreate;
  private final String testClassName;
  private final IMultiProcessControl processControl;
  private final String connectUri;

  public InterruptableClientManager(ITestStateManager stateManager, VerboseLogger logger, String testParentDirectory, String clientClassPath, DebugOptions debugOptions, int clientsToCreate, String testClassName, IMultiProcessControl processControl, String connectUri) {
    this.logger = logger;
    this.testParentDirectory = testParentDirectory;
    this.clientClassPath = clientClassPath;
    this.debugOptions = debugOptions;
    this.clientsToCreate = clientsToCreate;
    this.testClassName = testClassName;
    this.processControl = processControl;
    this.connectUri = connectUri;
  }

  public boolean runClientSequence() {
    // All clients use the same entry-point stub.
    String clientClassName = "org.terracotta.testing.client.TestClientStub";
    ContextualLogger clientsLogger = new ContextualLogger(this.logger, "[Clients]");
    ClientInstaller clientInstaller = new ClientInstaller(clientsLogger, this.processControl, this.testParentDirectory, this.clientClassPath, clientClassName, this.testClassName, this.connectUri);
    
    // Run the setup client, synchronously.
    ClientRunner setupClient = clientInstaller.installClient("client_setup", "SETUP", this.debugOptions.setupClientPort);
    int setupExitValue = -1;
    try {
      setupExitValue = runClientSynchronous(setupClient);
    } catch (InterruptedException e) {
      // We don't have a definition of this case.
      Assert.unexpected(e);
    } catch (IOException e) {
      // We don't expect this here.
      Assert.unexpected(e);
    }
    
    boolean setupWasClean = (0 == setupExitValue);
    boolean didRunCleanly = true;
    boolean destroyWasClean = true;
    if (setupWasClean) {
      ClientRunner[] concurrentTests = installTestClients(this.debugOptions, this.clientsToCreate, clientInstaller);
      try {
        // Start them.
        for (ClientRunner oneClient : concurrentTests) {
          oneClient.start();
          oneClient.waitForPid();
        }
        // Now, wait for them to finish.
        for (ClientRunner oneClient : concurrentTests) {
          int result = oneClient.waitForJoinResult();
          didRunCleanly &= (0 == result);
        }
      } catch (InterruptedException e) {
        // We don't have a definition of this case.
        Assert.unexpected(e);
      } catch (IOException e) {
        // We don't expect this here.
        Assert.unexpected(e);
      }
      if (!didRunCleanly) {
        logger.fatal("ERROR encountered in test client.  Destroy will be attempted but this is a failure");
      }
      
      // Run the destroy client, synchronously.
      ClientRunner destroyClient = clientInstaller.installClient("client_destroy", "DESTROY", this.debugOptions.destroyClientPort);
      int destroyExitValue = -1;
      try {
        destroyExitValue = runClientSynchronous(destroyClient);
      } catch (InterruptedException e) {
        // We don't have a definition of this case.
        Assert.unexpected(e);
      } catch (IOException e) {
        // We don't expect this here.
        Assert.unexpected(e);
      }
      destroyWasClean = (0 == destroyExitValue);
      if (!destroyWasClean) {
        this.logger.fatal("ERROR encountered in destroy.  This is a failure");
      }
    } else {
      this.logger.fatal("FATAL ERROR IN SETUP CLIENT!  Exit code " + setupExitValue + ".  NOT running tests!");
    }
    return setupWasClean && didRunCleanly && destroyWasClean;
  }

  private int runClientSynchronous(ClientRunner client) throws InterruptedException, IOException {
    client.start();
    client.waitForPid();
    return client.waitForJoinResult();
  }

  private ClientRunner[] installTestClients(DebugOptions debugOptions, int clientsToCreate, ClientInstaller clientInstaller) {
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
    return testClients;
  }
}
