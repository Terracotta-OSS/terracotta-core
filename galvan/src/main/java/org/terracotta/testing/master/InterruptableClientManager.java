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
import org.terracotta.testing.logging.VerboseManager;


/**
 * A class which just contains the common logic of the process of running the client-side setup/tearDown/test logic for a
 * run.  It runs this task in a background thread and exposes an interruption mechanism, in case the test needs to be forced
 * to stop.
 * It extends Thread since it is just an additional helper to coordinate external interruption.
 */
public class InterruptableClientManager extends Thread implements IComponentManager {
  private final ITestStateManager stateManager;
  private final VerboseManager verboseManager;
  private final String testParentDirectory;
  private final String clientClassPath;
  private final DebugOptions debugOptions;
  private final int clientsToCreate;
  private final String testClassName;
  private final IMultiProcessControl processControl;
  private final String connectUri;
  private boolean interruptRequested;

  public InterruptableClientManager(ITestStateManager stateManager, VerboseManager verboseManager, String testParentDirectory, String clientClassPath, DebugOptions debugOptions, int clientsToCreate, String testClassName, IMultiProcessControl processControl, String connectUri) {
    this.stateManager = stateManager;
    this.verboseManager = verboseManager;
    this.testParentDirectory = testParentDirectory;
    this.clientClassPath = clientClassPath;
    this.debugOptions = debugOptions;
    this.clientsToCreate = clientsToCreate;
    this.testClassName = testClassName;
    this.processControl = processControl;
    this.connectUri = connectUri;
  }

  @Override
  public void forceTerminateComponent() {
    // We need to set that an interrupt is requested in order for our asserts, within, to trigger.
    this.interruptRequested = true;
    // We now interrupt the thread, which should cause it to shut everything down, forcefully, and fail out.
    this.interrupt();
  }

  @Override
  public void run() {
    // All clients use the same entry-point stub.
    String clientClassName = "org.terracotta.testing.client.TestClientStub";
    VerboseManager clientsVerboseManager = this.verboseManager.createComponentManager("[Clients]");
    ClientInstaller clientInstaller = new ClientInstaller(clientsVerboseManager, this.processControl, this.testParentDirectory, this.clientClassPath, clientClassName, this.testClassName, this.connectUri);
    
    ContextualLogger harnessLogger = clientsVerboseManager.createHarnessLogger();
    
    // Run the setup client, synchronously.
    ClientRunner setupClient = clientInstaller.installClient("client_setup", "SETUP", this.debugOptions.setupClientDebugPort, this.clientsToCreate, 0);
    try {
      setupClient.openStandardLogFiles();
    } catch (IOException e) {
      // We don't expect this here.
      Assert.unexpected(e);
    }
    int setupExitValue = -1;
    try {
      setupExitValue = runClientSynchronous(setupClient);
    } catch (InterruptedException e) {
      // We can only be interrupted if an interruption is expected.
      Assert.assertTrue(this.interruptRequested);
      // Terminate the client.
      setupClient.forceTerminate();
      // Mark this as a failure so we fall out.
      setupExitValue = -1;
    } catch (IOException e) {
      // We don't expect this here.
      Assert.unexpected(e);
    }
    try {
      setupClient.closeStandardLogFiles();
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
          try {
            oneClient.openStandardLogFiles();
          } catch (IOException e) {
            // We don't expect this here.
            Assert.unexpected(e);
          }
          oneClient.start();
          oneClient.waitForPid();
        }
        // Now, wait for them to finish.
        for (ClientRunner oneClient : concurrentTests) {
          int result = oneClient.waitForJoinResult();
          try {
            oneClient.closeStandardLogFiles();
          } catch (IOException e) {
            // We don't expect this here.
            Assert.unexpected(e);
          }
          didRunCleanly &= (0 == result);
        }
      } catch (InterruptedException e) {
        // We can only be interrupted if an interruption is expected.
        Assert.assertTrue(this.interruptRequested);
        // Terminate the clients.
        for (ClientRunner oneClient : concurrentTests) {
          oneClient.forceTerminate();
        }
        // Mark this as a failure so we fall out.
        didRunCleanly = false;
      } catch (IOException e) {
        // We don't expect this here.
        Assert.unexpected(e);
      }
      if (!didRunCleanly) {
        harnessLogger.error("ERROR encountered in test client.  Destroy will be attempted but this is a failure");
      }
      
      // Run the destroy client, synchronously.
      ClientRunner destroyClient = clientInstaller.installClient("client_destroy", "DESTROY", this.debugOptions.destroyClientDebugPort, this.clientsToCreate, 0);
      try {
        destroyClient.openStandardLogFiles();
      } catch (IOException e) {
        // We don't expect this here.
        Assert.unexpected(e);
      }
      int destroyExitValue = -1;
      try {
        destroyExitValue = runClientSynchronous(destroyClient);
      } catch (InterruptedException e) {
        // We can only be interrupted if an interruption is expected.
        Assert.assertTrue(this.interruptRequested);
        // Terminate the client.
        destroyClient.forceTerminate();
        // Mark this as a failure so we fall out.
        destroyExitValue = -1;
      } catch (IOException e) {
        // We don't expect this here.
        Assert.unexpected(e);
      }
      try {
        destroyClient.closeStandardLogFiles();
      } catch (IOException e) {
        // We don't expect this here.
        Assert.unexpected(e);
      }
      destroyWasClean = (0 == destroyExitValue);
      if (!destroyWasClean) {
        harnessLogger.error("ERROR encountered in destroy.  This is a failure");
      }
    } else {
      harnessLogger.error("FATAL ERROR IN SETUP CLIENT!  Exit code " + setupExitValue + ".  NOT running tests!");
    }
    if (setupWasClean && didRunCleanly && destroyWasClean) {
      this.stateManager.testDidPass();
    } else {
      this.stateManager.testDidFail();
    }
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
      int debugPort = (0 != debugOptions.testClientDebugPortStart)
          ? (debugOptions.testClientDebugPortStart + i)
          : 0;
      testClients[i] = clientInstaller.installClient(clientName, "TEST", debugPort, clientsToCreate, i);
    }
    return testClients;
  }
}
