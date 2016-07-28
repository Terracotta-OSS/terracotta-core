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
import java.util.List;

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseManager;


/**
 * A class which just contains the common logic of the process of running the client-side setup/tearDown/test logic for a
 * run.  It runs this task in a background thread and exposes an interruption mechanism, in case the test needs to be forced
 * to stop.
 * It extends Thread since it is just an additional helper to coordinate external interruption.
 */
public class InterruptableClientManager extends Thread {
  private final IGalvanStateInterlock stateInterlock;
  private final ITestStateManager stateManager;
  private final VerboseManager verboseManager;
  private final IMultiProcessControl processControl;
  private final String testParentDirectory;
  private final String clientClassPath;
  private final int setupClientDebugPort;
  private final int destroyClientDebugPort;
  private final int testClientDebugPortStart;
  private final int clientsToCreate;
  private final IClientArgumentBuilder clientArgumentBuilder;
  private final String connectUri;

  public InterruptableClientManager(IGalvanStateInterlock stateInterlock, ITestStateManager stateManager, VerboseManager verboseManager, IMultiProcessControl processControl, String testParentDirectory, String clientClassPath, int setupClientDebugPort, int destroyClientDebugPort, int testClientDebugPortStart, int clientsToCreate, IClientArgumentBuilder clientArgumentBuilder, String connectUri) {
    this.stateInterlock = stateInterlock;
    this.stateManager = stateManager;
    this.verboseManager = verboseManager;
    this.processControl = processControl;
    
    this.testParentDirectory = testParentDirectory;
    this.clientClassPath = clientClassPath;
    
    this.setupClientDebugPort = setupClientDebugPort;
    this.destroyClientDebugPort = destroyClientDebugPort;
    this.testClientDebugPortStart = testClientDebugPortStart;
    
    this.clientsToCreate = clientsToCreate;
    this.clientArgumentBuilder = clientArgumentBuilder;
    this.connectUri = connectUri;
  }

  @Override
  public void run() {
    VerboseManager clientsVerboseManager = this.verboseManager.createComponentManager("[Clients]");
    ClientInstaller clientInstaller = new ClientInstaller(clientsVerboseManager, this.processControl, this.testParentDirectory, this.clientClassPath, this.clientArgumentBuilder.getMainClassName());
    
    ContextualLogger harnessLogger = clientsVerboseManager.createHarnessLogger();
    
    // Run the setup client, synchronously.
    List<String> extraSetupArguments = this.clientArgumentBuilder.getArgumentsForSetupRun(this.connectUri, this.clientsToCreate);
    ClientRunner setupClient = clientInstaller.installClient("client_setup", this.setupClientDebugPort, extraSetupArguments);
    boolean setupWasClean = runClientLifeCycle(setupClient);
    
    boolean didRunCleanly = true;
    boolean destroyWasClean = true;
    String errorMessage = null;
    if (setupWasClean) {
      ClientRunner[] concurrentTests = installTestClients(this.testClientDebugPortStart, this.clientsToCreate, clientInstaller);
      
      // Create a listener.
      ClientListener listener = new ClientListener(this.stateInterlock, this.stateManager);
      // Start them.
      for (ClientRunner oneClient : concurrentTests) {
        oneClient.setListener(listener);
        try {
          oneClient.openStandardLogFiles();
        } catch (IOException e) {
          // We don't expect this here.
          Assert.unexpected(e);
        }
        this.stateInterlock.registerRunningClient(oneClient);
        oneClient.start();
      }
      // Now, wait for them to finish.
      try {
        this.stateInterlock.waitForClientTermination();
      } catch (Exception e) {
        didRunCleanly = false;
      }
      
      shutDownAndCleanUpClients(!didRunCleanly, concurrentTests);
      if (didRunCleanly) {
        // Run the destroy client, synchronously.
        List<String> extraDestroyArguments = this.clientArgumentBuilder.getArgumentsForDestroyRun(this.connectUri, this.clientsToCreate);
        ClientRunner destroyClient = clientInstaller.installClient("client_destroy", this.destroyClientDebugPort, extraDestroyArguments);
        destroyWasClean = runClientLifeCycle(destroyClient);
        if (!destroyWasClean) {
          errorMessage = "ERROR encountered in destroy client.  This is a failure";
        }
      } else {
        errorMessage = "ERROR encountered in test client.  This is a failure";
      }
    } else {
      errorMessage = "ERROR encountered in setup client.  This is a failure";
    }
    if (setupWasClean && didRunCleanly && destroyWasClean) {
      this.stateManager.setTestDidPassIfNotFailed();
    } else {
      harnessLogger.error(errorMessage);
      this.stateManager.testDidFail(new GalvanFailureException(errorMessage));
    }
  }

  private void shutDownAndCleanUpClients(boolean shouldForceTerminate, ClientRunner[] concurrentTests) {
    if (shouldForceTerminate) {
      // Terminate all the tests.
      for (ClientRunner oneClient : concurrentTests) {
        oneClient.forceTerminate();
      }
    }
    
    // Join all the threads and close log files.
    for (ClientRunner oneClient : concurrentTests) {
      try {
        oneClient.join();
      } catch (InterruptedException e) {
        // This is not expected in this join since we already expect that the thread terminated (since we were notified that it was shutting down).
        Assert.unexpected(e);
      }
      try {
        oneClient.closeStandardLogFiles();
      } catch (IOException e) {
        // We don't expect this here.
        Assert.unexpected(e);
      }
    }
  }

  private boolean runClientLifeCycle(ClientRunner synchronousClient) {
    try {
      synchronousClient.openStandardLogFiles();
    } catch (IOException e) {
      // We don't expect this here.
      Assert.unexpected(e);
    }
    boolean clientDidPass = false;
    try {
      clientDidPass = runClientSynchronous(synchronousClient);
    } catch (InterruptedException e) {
      // We don't expect interrupts, here.
      Assert.unexpected(e);
    }
    try {
      synchronousClient.closeStandardLogFiles();
    } catch (IOException e) {
      // We don't expect this here.
      Assert.unexpected(e);
    }
    return clientDidPass;
  }

  private boolean runClientSynchronous(ClientRunner client) throws InterruptedException {
    ClientListener listener = new ClientListener(this.stateInterlock, this.stateManager);
    client.setListener(listener);
    this.stateInterlock.registerRunningClient(client);
    client.start();
    boolean didFailEarly = false;
    try {
      this.stateInterlock.waitForClientTermination();
    } catch (Exception e) {
      didFailEarly = true;
    }
    client.join();
    return !didFailEarly;
  }

  private ClientRunner[] installTestClients(int testClientDebugPortStart, int clientsToCreate, ClientInstaller clientInstaller) {
    // The setup was clean so run the test clients.
    // First, install them.
    ClientRunner[] testClients = new ClientRunner[clientsToCreate];
    for (int i = 0; i < clientsToCreate; ++i) {
      String clientName = "client" + i;
      // Determine if we need a debug port set.
      int debugPort = (0 != testClientDebugPortStart)
          ? (testClientDebugPortStart + i)
          : 0;
      List<String> extraArguments = this.clientArgumentBuilder.getArgumentsForTestRun(this.connectUri, clientsToCreate, i);
      testClients[i] = clientInstaller.installClient(clientName, debugPort, extraArguments);
    }
    return testClients;
  }


  private static class ClientListener implements ClientRunner.Listener {
    private final IGalvanStateInterlock interlock;
    private final ITestStateManager stateManager;
    
    public ClientListener(IGalvanStateInterlock interlock, ITestStateManager stateManager) {
      this.interlock = interlock;
      this.stateManager = stateManager;
    }
    
    @Override
    public void clientDidTerminate(ClientRunner clientRunner, int theResult) {
      // NOTE:  We need to set the fail state before we terminate the client or the waiting thread may see the clients finish before it sees the error.
      if (0 != theResult) {
        // We want to force the test into failure.
        this.stateManager.testDidFail(new GalvanFailureException("Client returned: " + theResult));
      }
      this.interlock.clientDidTerminate(clientRunner);
    }
  }
}
