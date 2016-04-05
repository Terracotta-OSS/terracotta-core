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

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.terracotta.ipceventbus.event.Event;
import org.terracotta.ipceventbus.event.EventBus;
import org.terracotta.ipceventbus.event.EventListener;
import org.terracotta.ipceventbus.proc.AnyProcess;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.common.SimpleEventingStream;


public class ServerProcess {
  private static enum ServerState {
    UNKNOWN,
    ACTIVE,
    PASSIVE,
  };
  private final ServerInstallation underlyingInstallation;
  private final String serverName;
  private final File serverWorkingDirectory;
  private final FileOutputStream stdoutLog;
  private final FileOutputStream stderrLog;
  private ServerState state;
  private boolean isScriptReady;
  private final ExitWaiter exitWaiter;

  public ServerProcess(ITestStateManager stateManager, ServerInstallation underlyingInstallation, String serverName, File serverWorkingDirectory, FileOutputStream stdoutLog, FileOutputStream stderrLog) {
    this.underlyingInstallation = underlyingInstallation;
    this.serverName = serverName;
    this.serverWorkingDirectory = serverWorkingDirectory;
    this.stdoutLog = stdoutLog;
    this.stderrLog = stderrLog;
    // Start in the unknown state and we will wait for the stream scraping to determine our actual state.
    this.state = ServerState.UNKNOWN;
    // Because a server can crash at any time, not just when we are expecting it to, we need a thread to wait on this operation and notify stateManager if the
    // crash was not expected.
    this.exitWaiter = new ExitWaiter(stateManager);
  }

  public ServerInstallation getUnderlyingInstallation() {
    return this.underlyingInstallation;
  }

  /**
   * Starts the server, in the background, using its constructed name to find its config in the stripe's config file.
   * Note that the server is unlikely to have entered a specific state when this function returns (waitForReady(boolean) must be called).
   * 
   * @return The PID of the server process
   */
  public long start() {
    Assert.assertNotNull(this.stdoutLog);
    Assert.assertNotNull(this.stderrLog);
    
    EventBus serverBus = new EventBus.Builder().id("server-bus").build();
    String startedReadyName = "STARTED";
    String activeReadyName = "ACTIVE";
    String passiveReadyName = "PASSIVE";
    Map<String, String> eventMap = new HashMap<String, String>();
    eventMap.put("Server started as ", startedReadyName);
    eventMap.put("Terracotta Server instance has started up as ACTIVE node", activeReadyName);
    eventMap.put("Moved to State[ PASSIVE-STANDBY ]", passiveReadyName);
    
    SimpleEventingStream outputStream = new SimpleEventingStream(serverBus, eventMap, this.stdoutLog);
    serverBus.on(startedReadyName, new EventListener(){
      @Override
      public void onEvent(Event arg0) throws Throwable {
        synchronized(ServerProcess.this) {
          ServerProcess.this.isScriptReady = true;
          ServerProcess.this.notifyAll();
        }
      }});
    serverBus.on(activeReadyName, new ActivePassiveEventWaiter(ServerState.ACTIVE));
    serverBus.on(passiveReadyName, new ActivePassiveEventWaiter(ServerState.PASSIVE));
    
    this.isScriptReady = false;
    AnyProcess process = AnyProcess.newBuilder()
        .command("server/bin/start-tc-server.sh", "-n", this.serverName)
        .workingDir(this.serverWorkingDirectory)
        .pipeStdout(outputStream)
        .pipeStderr(this.stderrLog)
        .build();
    // Wait for the server to enter started.
    synchronized (this) {
      while (!this.isScriptReady) {
        try {
          wait();
        } catch (InterruptedException e) {
          // We can't recover from interruption here.
          Assert.unexpected(e);
        }
      }
    }
    // We will now hand off the process to the ExitWaiter.
    this.exitWaiter.startBackgroundWait(process);
    return process.getPid();
  }

  /**
   * Waits until the server enters either a passive or active state.  This will return, immediately, if the server is already in one of these states.
   * 
   * @return True if the server is active, false if passive.
   */
  public synchronized boolean waitForStartIsActive() throws InterruptedException {
    while (ServerState.UNKNOWN == this.state) {
      wait();
    }
    return (ServerState.ACTIVE == this.state);
  }

  public int stop() throws InterruptedException {
    return this.exitWaiter.bringDownServer();
  }


  /**
   * Called by a background thread processing the server's output stream to notify us that the server has entered a specific state.
   * @param stateToEnter
   */
  private synchronized void enterState(ServerState stateToEnter) {
    this.state = stateToEnter;
    // Notify anyone who was waiting on this state change.
    notifyAll();
  }


  private class ActivePassiveEventWaiter implements EventListener {
    private final ServerState stateToEnter;
    
    public ActivePassiveEventWaiter(ServerState stateToEnter) {
      this.stateToEnter = stateToEnter;
    }
    @Override
    public synchronized void onEvent(Event e) throws Throwable {
      ServerProcess.this.enterState(stateToEnter);
    }
  }

  private class ExitWaiter extends Thread {
    private final ITestStateManager stateManager;
    private AnyProcess process;
    private boolean isCrashExpected;
    private int returnValue;
    
    public ExitWaiter(ITestStateManager stateManager) {
      this.stateManager = stateManager;
      this.returnValue = -1;
    }
    public void startBackgroundWait(AnyProcess process) {
      Assert.assertNull(this.process);
      this.process = process;
      this.start();
    }
    @Override
    public void run() {
      try {
        this.returnValue = this.process.waitFor();
      } catch (java.util.concurrent.CancellationException e) {
        this.returnValue = this.process.exitValue();
      } catch (InterruptedException e) {
        // We don't expect interruption in this part of the test - we need to wait for the termination.
        Assert.unexpected(e);
      }
      // If we send the failure, we don't want to do it under lock.
      boolean shouldSendFailure = false;
      synchronized(this) {
        // See if this crash was expected.
        if (this.isCrashExpected) {
          // This means that someone is waiting for us.
          this.notifyAll();
        } else {
          // We weren't expecting this so we need to notify the stateManager.
          shouldSendFailure = true;
        }
      }
      if (shouldSendFailure) {
        this.stateManager.testDidFail();
      }
    }
    public synchronized int bringDownServer() throws InterruptedException {
      // Mark this as expected.
      this.isCrashExpected = true;
      // Destroy the process.
      this.process.destroy();
      // Wait until we get a return value.
      while (-1 == this.returnValue) {
        wait();
      }
      return this.returnValue;
    }
  }
}
