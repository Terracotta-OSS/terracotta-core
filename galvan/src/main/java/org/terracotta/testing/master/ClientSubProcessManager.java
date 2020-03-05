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

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.config.ClientsConfiguration;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseManager;

import java.io.IOException;
import java.util.List;


/**
 * This class contains the logic for how the test runs client processes in the usual sequence:
 * -1 setup client
 * -n test clients
 * -1 destroy client
 * An internal thread is used to manage the progression from one to another, instead of relying on purely asynchronous
 * machinery to describe where the test is, in that sequence.
 * The client process states are managed within the given IGalvanStateInterlock, however.
 */
public class ClientSubProcessManager extends Thread {
  private final IGalvanStateInterlock stateInterlock;
  private final ITestStateManager stateManager;
  private final VerboseManager verboseManager;
  private final IMultiProcessControl processControl;
  private final ClientsConfiguration clientsConfig;

  public ClientSubProcessManager(IGalvanStateInterlock stateInterlock, ITestStateManager stateManager, VerboseManager verboseManager,
                                 IMultiProcessControl processControl, ClientsConfiguration clientsConfig) {
    this.stateInterlock = stateInterlock;
    this.stateManager = stateManager;
    this.verboseManager = verboseManager;
    this.processControl = processControl;
    this.clientsConfig = clientsConfig;
  }

  @Override
  public void run() {
    VerboseManager clientsVerboseManager = this.verboseManager.createComponentManager("[Clients]");
    ClientInstaller clientInstaller = new ClientInstaller(clientsVerboseManager, this.processControl, clientsConfig.getTestParentDirectory(),
        clientsConfig.getClientClassPath(), clientsConfig.getClientArgumentBuilder().getMainClassName());

    ContextualLogger harnessLogger = clientsVerboseManager.createHarnessLogger();

    // Run the setup client, synchronously.
    List<String> extraSetupArguments = clientsConfig.getClientArgumentBuilder().getArgumentsForSetupRun(
        clientsConfig.getConnectUri(), clientsConfig.getClusterInfo(), clientsConfig.getNumberOfStripes(), clientsConfig.getNumberOfServersPerStripe(), clientsConfig.getClientsToCreate());
    ClientRunner setupClient = clientInstaller.installClient("client_setup", clientsConfig.getSetupClientDebugPort(), clientsConfig.isFailOnLog(), extraSetupArguments);
    boolean setupWasClean = runClientLifeCycle(setupClient);

    boolean didRunCleanly = true;
    boolean destroyWasClean = true;
    String errorMessage = null;
    if (setupWasClean) {
      didRunCleanly = runTestClients(clientInstaller);
      if (didRunCleanly) {
        // Run the destroy client, synchronously.
        List<String> extraDestroyArguments = clientsConfig.getClientArgumentBuilder().getArgumentsForDestroyRun(
            clientsConfig.getConnectUri(), clientsConfig.getClusterInfo(), clientsConfig.getNumberOfStripes(), clientsConfig.getNumberOfServersPerStripe(), clientsConfig.getClientsToCreate());
        ClientRunner destroyClient = clientInstaller.installClient("client_destroy", clientsConfig.getDestroyClientDebugPort(), clientsConfig.isFailOnLog(), extraDestroyArguments);
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

  private boolean runTestClients(ClientInstaller clientInstaller) {
    ClientRunner[] concurrentTests = installTestClients(clientsConfig.getTestClientDebugPortStart(), clientsConfig.getClientsToCreate(), clientInstaller);

    // Create a listener.
    ClientListener listener = new ClientListener(this.stateInterlock, this.stateManager);
    // Start them.
    boolean didRegisterAndStart = true;
    for (ClientRunner oneClient : concurrentTests) {
      oneClient.setListener(listener);
      try {
        oneClient.openStandardLogFiles();
      } catch (IOException e) {
        // We don't expect this here.
        Assert.unexpected(e);
      }
      try {
        this.stateInterlock.registerRunningClient(oneClient);
        oneClient.start();
      } catch (GalvanFailureException e) {
        // It is possible to end up here if the test failed before we even got to the point of starting clients.
        didRegisterAndStart = false;
      }
    }
    // Now, wait for them to finish.
    boolean didTerminateWithoutError;
    try {
      this.stateInterlock.waitForClientTermination();
      didTerminateWithoutError = true;
    } catch (Exception e) {
      didTerminateWithoutError = false;
    }

    boolean didRunCleanly = didRegisterAndStart && didTerminateWithoutError;
    shutDownAndCleanUpClients(!didRunCleanly, concurrentTests);
    return didRunCleanly;
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
    boolean didRegisterAndStart = false;
    try {
      this.stateInterlock.registerRunningClient(client);
      client.start();
      didRegisterAndStart = true;
    } catch (GalvanFailureException e) {
      didRegisterAndStart = false;
    }
    boolean didFailEarly = false;
    try {
      this.stateInterlock.waitForClientTermination();
    } catch (Exception e) {
      didFailEarly = true;
    }
    client.join();
    return didRegisterAndStart && !didFailEarly;
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
      List<String> extraArguments = clientsConfig.getClientArgumentBuilder().getArgumentsForTestRun(
          clientsConfig.getConnectUri(), clientsConfig.getClusterInfo(), clientsConfig.getNumberOfStripes(), clientsConfig.getNumberOfServersPerStripe(), clientsToCreate, i);
      testClients[i] = clientInstaller.installClient(clientName, debugPort, clientsConfig.isFailOnLog(), extraArguments);
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
