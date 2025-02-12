/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.terracotta.ipceventbus.event.Event;
import org.terracotta.ipceventbus.event.EventBus;
import org.terracotta.ipceventbus.event.EventListener;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.common.SimpleEventingStream;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseManager;
import static org.terracotta.testing.master.ServerMode.ACTIVE;


public abstract class ServerInstance implements IGalvanServer {
  protected StateInterlock stateInterlock;
  protected ITestStateManager stateManager;
//  protected final ContextualLogger harnessLogger;
  protected ContextualLogger serverLogger;
  protected final String serverName;
  // make sure only one caller is messing around on the process
  private final Semaphore oneUser = new Semaphore(1);

  private UUID userToken;
  // The PID of the actual server, underneath the start script.  This is 0 until we are killable and can tell the interlock that we are running.
  private long pid;
  // When we are going to bring down the server, we need to record that we expected the crash so we don't conclude the test failed.
  private boolean isCrashExpected;

  private ServerMode currentState = ServerMode.TERMINATED;

  public ServerInstance(String serverName) {
    this.serverName = serverName;
  }
  
  public void installIntoStripe(StateInterlock stateInterlock, ITestStateManager stateManager, VerboseManager logging) {
    this.stateInterlock = stateInterlock;
    this.stateManager = stateManager;
    this.serverLogger = logging.createServerLogger();
    this.stateInterlock.registerNewServer(this);
  }

  /**
   * enter/exit is used by start and stop to make sure those methods are called one
   * at a time.
   *
   * @return a unique token used to make sure the starter is the finisher (passed to exit)
   */
  protected UUID enter() {
    try {
      oneUser.acquire();
      userToken = UUID.randomUUID();
      return this.userToken;
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    }
  }

  protected void exit(UUID token) {
    Assert.assertTrue(token.equals(this.userToken));
    oneUser.release();
  }

  /**
   * Starts the server, in the background, using its constructed name to find its config in the stripe's config file.
   * <p>
   * Note that the start attempt returns, immediately, and the server will report its state transition through the
   * GalvanStateInterlock.  Specifically, the server starts as "terminated" when it is first registered.  Once the
   * sub-process is able to start up and we know its PID, it will become "unknownRunning".
   * Note that it is possible for the server to fail to start up, signalling a test failure in the interlock.
   * <p>
   * Note that we synchronize this call so that so that no events can come in, asynchronously, while we are starting this up (since all events which originate in this server should be in a well-defined order).
   *
   * @throws IOException The logs couldn't be created since the server's working directory is missing.
   */
  public abstract void start() throws IOException;

  protected synchronized boolean isCrashExpected() {
    return this.isCrashExpected;
  }

  public synchronized void setCrashExpected(boolean expect) {
    this.isCrashExpected = expect;
  }

  protected synchronized ServerMode setCurrentState(ServerMode mode) {
    ServerMode previous = currentState;
    currentState = mode;
    notifyAll();
    return previous;
  }

  protected OutputStream buildEventingStream(OutputStream out) {
    // Now, set up the event bus we will use to scrape the state from the sub-process.
    EventBus serverBus = new EventBus.Builder().id("server-bus").build();
    String starting = "BOOTSTRAPPED";
    String pidEventName = "PID";
    String activeReadyName = "ACTIVE";
    String passiveReadyName = "PASSIVE";
    String diagnosticReadyName = "DIAGNOSTIC";
    String zapEventName = "ZAP";
    String warn = "WARN";
    String err = "ERROR";
    Map<String, String> eventMap = new HashMap<>();
    eventMap.put("PID is", pidEventName);
    eventMap.put("Terracotta Server instance has started diagnostic listening", starting);
    eventMap.put("Terracotta Server instance has started up as ACTIVE node", activeReadyName);
    eventMap.put("Moved to State[ PASSIVE-STANDBY ]", passiveReadyName);
    eventMap.put("Moved to State[ DIAGNOSTIC ]", diagnosticReadyName);
    eventMap.put("Restarting the server", zapEventName);
    eventMap.put("Requesting restart", zapEventName);
    eventMap.put("WARN", warn);
    eventMap.put("ERROR", err);

      serverBus.on(pidEventName, new EventListener() {
      @Override
      public void onEvent(Event event) throws Throwable {
        String line = event.getData(String.class);
        Matcher m = Pattern.compile("PID is ([0-9]*)").matcher(line);
        if (m.find()) {
          try {
            String pid = m.group(1);
            didStartWithPid(Long.parseLong(pid));
          } catch (NumberFormatException format) {
            Assert.unexpected(format);
          }
        } else {
          // This is a little unusual, since it is a partial match, so at least log it in case something is wrong.
          serverLogger.error("Unexpected PID-like line from server: " + line);
        }
      }
    });
    serverBus.on(starting, (event) -> setCurrentState(ServerMode.UNKNOWN));
    serverBus.on(activeReadyName, (event) -> didBecomeActive());
    serverBus.on(passiveReadyName, (event) -> setCurrentState(ServerMode.PASSIVE));
    serverBus.on(diagnosticReadyName, (event) -> setCurrentState(ServerMode.DIAGNOSTIC));
    serverBus.on(zapEventName, (event)-> instanceWasZapped());
    serverBus.on(warn, (event) -> handleWarnLog(event));
    serverBus.on(err, (event) -> handleErrorLog(event));

    return new SimpleEventingStream(serverBus, eventMap, out);
  }
  /**
   * Called from outside to asynchronously kill the underlying process.
   * Note that this does do some interruptable blocking, since it interacts with some sub-processes to discover the server process.
   * The termination of the actual server process, itself, is reported to the interlock, when it happens.
   *
   * @throws InterruptedException
   */
  public abstract void stop() throws InterruptedException;


  public boolean isServerRunning() {
    return getCurrentState() != ServerMode.TERMINATED;
  }

  public boolean isActive() {
    return getCurrentState() == ServerMode.ACTIVE;
  }

  protected synchronized long waitForPid() {
    try {
      while (isServerRunning() && this.pid == 0) {
        this.wait();
      }
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    }
    return this.pid;
  }
  /**
   * Called by the inline EventListener implementations when the server under the script reports its PID.
   *
   * @param pid The PID of the server process.
   */
  private synchronized void didStartWithPid(long pid) {
    Assert.assertTrue(pid > 0);
    this.pid = pid;
    notifyAll();
  }

  protected synchronized void reset() {
    this.pid = 0;
    notifyAll();
  }

  private void handleWarnLog(Event e) {

  }

  private void handleErrorLog(Event e) {

  }

  public synchronized ServerMode waitForRunning() {
    boolean loop = true;
    EnumSet<ServerMode> modes = EnumSet.of(ServerMode.ZAPPED, ServerMode.STARTUP);
    serverLogger.output("wait for running " + currentState);
    while (loop && modes.contains(currentState)) {
      loop = uninterruptableWait();
    }
    serverLogger.output("running " + currentState);
    return currentState;
  }

  public synchronized ServerMode waitForReady() {
    EnumSet<ServerMode> modes = EnumSet.of(ServerMode.ACTIVE, ServerMode.PASSIVE, ServerMode.DIAGNOSTIC);
    boolean loop = true;
    while (loop && !modes.contains(currentState)) {
      loop = uninterruptableWait();
    }
    serverLogger.output("ready " + currentState);
    return currentState;
  }

  public synchronized void waitForTermination() {
    boolean loop = true;
    while (loop && currentState != ServerMode.TERMINATED) {
      loop = uninterruptableWait();
    }
  }

  private synchronized boolean uninterruptableWait() {
    try {
      if (isServerRunning() && !this.stateInterlock.checkDidPass()) {
        wait();
        return true;
      } else {
        return false;
      }
    } catch (Exception ie) {
      return false;
    }
  }
  /**
   * Called by the inline EventListener implementations when the server becomes either active or passive.
   *
   * @param isActive True if active, false if passive.
   */
  private void didBecomeActive() {
    ServerMode previous = setCurrentState(ACTIVE);
    this.stateInterlock.serverBecameActive(this, previous);
  }
  /**
   * Called by the inline EventListener when the instance goes down for a restart due to ZAP.
   * This is really just a special case of a shut-down (we accept it, even if we weren't expecting it).
   */
  private void instanceWasZapped() {
    serverLogger.output("Server restarted due to ZAP");
    setCurrentState(ServerMode.ZAPPED);
    reset();
  }
  
  @Override
  public synchronized ServerMode getCurrentState() {
    return currentState;
  }

  @Override
  public synchronized void waitForState(ServerMode mode) throws InterruptedException {
    while (currentState != mode) {
      wait();
    }
  }

  @Override
  public String toString() {
    return "Server " + this.serverName + " (" + getCurrentState() + ")";
  }
}
