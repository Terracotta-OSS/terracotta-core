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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.terracotta.ipceventbus.event.Event;
import org.terracotta.ipceventbus.event.EventBus;
import org.terracotta.ipceventbus.event.EventListener;
import org.terracotta.ipceventbus.proc.AnyProcess;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.common.SimpleEventingStream;


public class ServerRunner {
  private static enum ServerState {
    UNKNOWN,
    ACTIVE,
    PASSIVE,
  };
  private final String serverName;
  private final File serverWorkingDirectory;
  private FileOutputStream stdoutLog;
  private FileOutputStream stderrLog;
  private AnyProcess process;
  private ServerState state;
  private boolean isScriptReady;

  public ServerRunner(String serverName, File serverWorkingDirectory) {
    this.serverName = serverName;
    this.serverWorkingDirectory = serverWorkingDirectory;
    // Start in the unknown state and we will wait for the stream scraping to determine our actual state.
    this.state = ServerState.UNKNOWN;
  }

  public void overwriteConfig(String config) throws IOException {
    File installPath = this.serverWorkingDirectory;
    File configPath = new File(installPath, "tc-config.xml");
    File oldConfigPath = new File(installPath, "tc-config.xml-old");
    configPath.renameTo(oldConfigPath);
    FileOutputStream stream = new FileOutputStream(configPath);
    byte[] toWrite = config.getBytes();
    stream.write(toWrite);
    stream.close();
  }

  public void setupStandardLogFiles() throws FileNotFoundException {
    Assert.assertNull(this.stdoutLog);
    Assert.assertNull(this.stderrLog);
    
    // We want to create an output log file for both STDOUT and STDERR.
    this.stdoutLog = new FileOutputStream(new File(this.serverWorkingDirectory, "stdout.log"));
    this.stderrLog = new FileOutputStream(new File(this.serverWorkingDirectory, "stderr.log"));
  }

  public void closeStandardLogFiles() throws IOException {
    this.stdoutLog.close();
    this.stdoutLog = null;
    this.stderrLog.close();
    this.stderrLog = null;
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
        synchronized(ServerRunner.this) {
          ServerRunner.this.isScriptReady = true;
          ServerRunner.this.notifyAll();
        }
      }});
    serverBus.on(activeReadyName, new ActivePassiveEventWaiter(ServerState.ACTIVE));
    serverBus.on(passiveReadyName, new ActivePassiveEventWaiter(ServerState.PASSIVE));
    
    this.isScriptReady = false;
    this.process = AnyProcess.newBuilder()
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
    return this.process.getPid();
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
    int retVal = -1;
    this.process.destroy();
    try {
      retVal = this.process.waitFor();
    } catch (java.util.concurrent.CancellationException e) {
      retVal = this.process.exitValue();
    }
    this.process = null;
    return retVal;
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
      ServerRunner.this.enterState(stateToEnter);
    }
  }
}
