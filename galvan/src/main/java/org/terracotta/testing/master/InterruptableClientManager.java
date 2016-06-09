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
import java.util.Vector;

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
  private final IMultiProcessControl processControl;
  private final String testParentDirectory;
  private final String clientClassPath;
  private final int setupClientDebugPort;
  private final int destroyClientDebugPort;
  private final int testClientDebugPortStart;
  private final int clientsToCreate;
  private final IClientArgumentBuilder clientArgumentBuilder;
  private final String connectUri;
  
  private boolean interruptRequested;

  public InterruptableClientManager(ITestStateManager stateManager, VerboseManager verboseManager, IMultiProcessControl processControl, String testParentDirectory, String clientClassPath, int setupClientDebugPort, int destroyClientDebugPort, int testClientDebugPortStart, int clientsToCreate, IClientArgumentBuilder clientArgumentBuilder, String connectUri) {
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
  public void forceTerminateComponent() {
    // We need to set that an interrupt is requested in order for our asserts, within, to trigger.
    this.interruptRequested = true;
    // We now interrupt the thread, which should cause it to shut everything down, forcefully, and fail out.
    this.interrupt();
  }

  @Override
  public void run() {
    VerboseManager clientsVerboseManager = this.verboseManager.createComponentManager("[Clients]");
    ClientInstaller clientInstaller = new ClientInstaller(clientsVerboseManager, this.processControl, this.testParentDirectory, this.clientClassPath, this.clientArgumentBuilder.getMainClassName());
    
    ContextualLogger harnessLogger = clientsVerboseManager.createHarnessLogger();
    
    // Run the setup client, synchronously.
    List<String> extraSetupArguments = this.clientArgumentBuilder.getArgumentsForSetupRun(this.connectUri);
    ClientRunner setupClient = clientInstaller.installClient("client_setup", this.setupClientDebugPort, extraSetupArguments);
    int setupExitValue = runClientLifeCycle(setupClient);
    
    boolean setupWasClean = (0 == setupExitValue);
    boolean didRunCleanly = true;
    boolean destroyWasClean = true;
    if (setupWasClean) {
      ClientRunner[] concurrentTests = installTestClients(this.testClientDebugPortStart, this.clientsToCreate, clientInstaller);
      try {
        // Create a listener.
        ClientListener listener = new ClientListener();
        // Start them.
        for (ClientRunner oneClient : concurrentTests) {
          oneClient.setListener(listener);
          try {
            oneClient.openStandardLogFiles();
          } catch (IOException e) {
            // We don't expect this here.
            Assert.unexpected(e);
          }
          oneClient.start();
        }
        // Now, wait for them to finish.
        int pendingResults = concurrentTests.length;
        // Run until we get all the results or a test reported failure.
        // (we need to eagerly kill all processes if any test exited with an error since someone may be waiting for it)
        while ((pendingResults > 0) && didRunCleanly) {
          int result = listener.waitForNextResult();
          didRunCleanly &= (0 == result);
          pendingResults -= 1;
        }
        
        shutDownAndCleanUpClients(!didRunCleanly, concurrentTests);
      } catch (InterruptedException e) {
        // We can only be interrupted if an interruption is expected.
        Assert.assertTrue(this.interruptRequested);
        // Mark this as a failure so we fall out.
        didRunCleanly = false;
        // Terminate and clean up.
        shutDownAndCleanUpClients(!didRunCleanly, concurrentTests);
      }
      if (!didRunCleanly) {
        harnessLogger.error("ERROR encountered in test client.  Destroy will be attempted but this is a failure");
      }
      
      // Run the destroy client, synchronously.
      List<String> extraDestroyArguments = this.clientArgumentBuilder.getArgumentsForDestroyRun(this.connectUri);
      ClientRunner destroyClient = clientInstaller.installClient("client_destroy", this.destroyClientDebugPort, extraDestroyArguments);
      int destroyExitValue = runClientLifeCycle(destroyClient);
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

  private int runClientLifeCycle(ClientRunner synchronousClient) {
    try {
      synchronousClient.openStandardLogFiles();
    } catch (IOException e) {
      // We don't expect this here.
      Assert.unexpected(e);
    }
    int setupExitValue = -1;
    try {
      setupExitValue = runClientSynchronous(synchronousClient);
    } catch (InterruptedException e) {
      // We can only be interrupted if an interruption is expected.
      Assert.assertTrue(this.interruptRequested);
      // Terminate the client.
      synchronousClient.forceTerminate();
      // We may need to join after requesting the termination.
      try {
        synchronousClient.join();
      } catch (InterruptedException e1) {
        // We don't expect an interruption within the interruption.
        Assert.unexpected(e1);
      }
      // Mark this as a failure so we fall out.
      setupExitValue = -1;
    }
    try {
      synchronousClient.closeStandardLogFiles();
    } catch (IOException e) {
      // We don't expect this here.
      Assert.unexpected(e);
    }
    return setupExitValue;
  }

  private int runClientSynchronous(ClientRunner client) throws InterruptedException {
    ClientListener listener = new ClientListener();
    client.setListener(listener);
    client.start();
    int result = listener.waitForNextResult();
    client.join();
    return result;
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
    private final List<Integer> results = new Vector<Integer>();
    
    public synchronized int waitForNextResult() throws InterruptedException {
      while (this.results.isEmpty()) {
        wait();
      }
      return this.results.remove(0);
    }
    
    @Override
    public synchronized void clientDidTerminate(ClientRunner clientRunner, int theResult) {
      this.results.add(theResult);
      notifyAll();
    }
  }
}
