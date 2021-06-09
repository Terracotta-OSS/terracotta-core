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

import org.terracotta.ipceventbus.event.Event;
import org.terracotta.ipceventbus.event.EventBus;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseManager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.testing.common.SimpleEventingStream;
import static org.terracotta.testing.master.ServerMode.ACTIVE;



public class InlineServerProcess implements IGalvanServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(InlineServerProcess.class);
  private final StateInterlock stateInterlock;
  private final ITestStateManager stateManager;
  private final ContextualLogger harnessLogger;
  private final String serverName;
  private final Path serverWorkingDir;
  private final Function<OutputStream, Object> serverStart;
  // make sure only one caller is messing around on the process
  private final Semaphore oneUser = new Semaphore(1);

  private UUID userToken;

  //  flag if the server was zapped so it can be logged
  private boolean wasZapped;

  // When we are going to bring down the server, we need to record that we expected the crash so we don't conclude the test failed.
  private boolean isCrashExpected;
  private Object server;

  private ServerMode currentState = ServerMode.TERMINATED;

  public InlineServerProcess(StateInterlock stateInterlock, ITestStateManager stateManager, VerboseManager serverVerboseManager,
                       String serverName, Path serverWorkingDir,
                       Function<OutputStream, Object> serverStart) {
    this.stateInterlock = stateInterlock;
    this.stateManager = stateManager;
    // We just want to create the harness logger and the one for the inferior process but then discard the verbose manager.
    this.harnessLogger = serverVerboseManager.createHarnessLogger();

    this.serverName = serverName;
    // We need to specify a positive integer as the heap size.
    this.serverWorkingDir = serverWorkingDir;
    this.serverStart = serverStart;
    // We start up in the shutdown state so notify the interlock.
    this.stateInterlock.registerNewServer(this);
  }

  /**
   * enter/exit is used by start and stop to make sure those methods are called one
   * at a time.
   *
   * @return a unique token used to make sure the starter is the finisher (passed to exit)
   */
  private UUID enter() {
    try {
      oneUser.acquire();
      userToken = UUID.randomUUID();
      return this.userToken;
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    }
  }

  private void exit(UUID token) {
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
  public void start() throws IOException {
    UUID token = enter();
    try {
      // First thing we need to do is make sure that we aren't already running.
      Assert.assertFalse(this.isServerRunning());
      setCurrentState(ServerMode.STARTUP);
      ExitWaiter exitWaiter = new ExitWaiter();
      exitWaiter.start();
    } finally {
      exit(token);
    }
  }

  private synchronized boolean isCrashExpected() {
    return this.isCrashExpected;
  }

  public synchronized void setCrashExpected(boolean expect) {
    this.isCrashExpected = expect;
  }

  private synchronized void reset() {
    this.wasZapped = isServerRunning();
    notifyAll();
  }

  private OutputStream buildEventingStream(OutputStream out) {
    // Now, set up the event bus we will use to scrape the state from the sub-process.
    EventBus serverBus = new EventBus.Builder().id("server-bus").build();
    String starting = "BOOTSTRAPPED";
    String activeReadyName = "ACTIVE";
    String passiveReadyName = "PASSIVE";
    String diagnosticReadyName = "DIAGNOSTIC";
    String zapEventName = "ZAP";
    String warn = "WARN";
    String err = "ERROR";
    Map<String, String> eventMap = new HashMap<>();
    eventMap.put("Terracotta Server instance has started diagnostic listening", starting);
    eventMap.put("Terracotta Server instance has started up as ACTIVE node", activeReadyName);
    eventMap.put("Moved to State[ PASSIVE-STANDBY ]", passiveReadyName);
    eventMap.put("Moved to State[ DIAGNOSTIC ]", diagnosticReadyName);
    eventMap.put("Restarting the server", zapEventName);
    eventMap.put("Requesting restart", zapEventName);
    eventMap.put("WARN", warn);
    eventMap.put("ERROR", err);

    serverBus.on(starting, (event) -> setCurrentState(ServerMode.UNKNOWN));
    serverBus.on(activeReadyName, (event) -> didBecomeActive());
    serverBus.on(passiveReadyName, (event) -> setCurrentState(ServerMode.PASSIVE));
    serverBus.on(diagnosticReadyName, (event) -> setCurrentState(ServerMode.DIAGNOSTIC));
    serverBus.on(zapEventName, (event)-> instanceWasZapped());
    serverBus.on(warn, (event) -> handleWarnLog(event));
    serverBus.on(err, (event) -> handleErrorLog(event));

    return new SimpleEventingStream(serverBus, eventMap, out);
  }

  private void handleWarnLog(Event e) {

  }

  private void handleErrorLog(Event e) {

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

  public synchronized ServerMode getCurrentState() {
    return currentState;
  }

  private synchronized ServerMode setCurrentState(ServerMode mode) {
    ServerMode previous = currentState;
    currentState = mode;
    notifyAll();
    return previous;
  }

  public synchronized ServerMode waitForRunning() {
    boolean loop = true;
    EnumSet<ServerMode> modes = EnumSet.of(ServerMode.ZAPPED, ServerMode.STARTUP);
    this.harnessLogger.output("wait for running " + currentState);
    while (loop && modes.contains(currentState)) {
      loop = uninterruptableWait();
    }
    return currentState;
  }

  public synchronized ServerMode waitForReady() {
    EnumSet<ServerMode> modes = EnumSet.of(ServerMode.ACTIVE, ServerMode.PASSIVE, ServerMode.DIAGNOSTIC);
    boolean loop = true;
    while (loop && !modes.contains(currentState)) {
      loop = uninterruptableWait();
    }
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

  public boolean isServerRunning() {
    return getCurrentState() != ServerMode.TERMINATED;
  }

  public boolean isActive() {
    return getCurrentState() == ServerMode.ACTIVE;
  }
  /**
   * Called by the inline EventListener when the instance goes down for a restart due to ZAP.
   * This is really just a special case of a shut-down (we accept it, even if we weren't expecting it).
   */
  private void instanceWasZapped() {
    this.harnessLogger.output("Server restarted due to ZAP");
    setCurrentState(ServerMode.ZAPPED);
    reset();
  }

  /**
   * Called by the exit waiter when the underlying process terminates.
   *
   * @param exitStatus The exit code of the underlying process.
   */
  private void didTerminateWithStatus(boolean restart) {
    // See if we have a PID yet or if this was a failure, much earlier (hence, if we told the interlock that we are even running).
    GalvanFailureException failureException = null;
//  no matter what, the server is gone so report it to interlock
    if (!restart && !this.isCrashExpected() && (null == failureException)) {
      failureException = new GalvanFailureException("Unexpected server crash: " + this + " restart: " + restart);
    }

    if (!restart) {
      setCurrentState(ServerMode.TERMINATED);
    }
    // In either case, we are not running.

    if (null != failureException) {
      this.stateManager.testDidFail(failureException);
    }

    reset();
  }
  /**
   * Called by the exit waiter when the underlying process terminates.
   *
   * @param exitStatus The exit code of the underlying process.
   */
  private void didTerminateWithException(Exception e) {
    // See if we have a PID yet or if this was a failure, much earlier (hence, if we told the interlock that we are even running).
    GalvanFailureException failureException = new GalvanFailureException("Unexpected server crash", e);
//  no matter what, the server is gone so report it to interlock
    if (this.isCrashExpected()) {
      failureException = null;
    }

    if (null != failureException) {
      this.stateManager.testDidFail(failureException);
    }
    
    setCurrentState(ServerMode.TERMINATED);
    // In either case, we are not running.
    reset();
  }
  /**
   * Called from outside to asynchronously kill the underlying process.
   * Note that this does do some interruptable blocking, since it interacts with some sub-processes to discover the server process.
   * The termination of the actual server process, itself, is reported to the interlock, when it happens.
   *
   * @throws InterruptedException
   */
  public void stop() throws InterruptedException {
    UUID token = enter();
    try {
      // Can't stop something not running.
      if (isServerRunning()) {
        // Log the intent.
        this.harnessLogger.output("Crashing server process: " + server);
        // Mark this as expected.
        this.setCrashExpected(true);

        harnessLogger.output("Server Stop Command Result: " + invokeOnServerMBean("Server","stop",null));

        harnessLogger.output("Attempt to kill server process resulted in:" + invokeOnObject(server, "waitUntilShutdown"));
        harnessLogger.output("server process killed");
      }
    } finally {
      exit(token);
    }
  }

  private String invokeOnServerMBean(String target, String call, String arg) {
    Object serverJMX = invokeOnObject(server, "getManagement");
    try {
      Method m = serverJMX.getClass().getMethod("call", String.class, String.class, String.class);
      m.setAccessible(true);
      return m.invoke(serverJMX, target, call, arg).toString();
    } catch (NoSuchMethodException |
            SecurityException |
            IllegalAccessException |
            IllegalArgumentException |
            InvocationTargetException s) {
      LOGGER.warn("unable to call", s);
      return "ERROR";
    }
  }

  private static Object invokeOnObject(Object server, String method, Object...args) {
    try {
      Class[] clazz = new Class[args.length];
      for (int x=0;x<args.length;x++) {
        Class sig = args[x] != null ? args[x].getClass() : null;
        clazz[x] = sig;
      }
      Method m = server.getClass().getMethod(method, clazz);
      m.setAccessible(true);
      return m.invoke(server, args);
    } catch (RuntimeException rt) {
      throw rt;
    } catch (Exception s) {
      LOGGER.warn("unable to invoke", s);
      throw new RuntimeException(s);
    }
  }

  private class ExitWaiter extends Thread {

    public ExitWaiter() {
    }

    @Override
    public void run() {
      boolean returnValue = true;
      while (returnValue) {
        try (OutputStream stdout = Files.newOutputStream(serverWorkingDir.resolve("stdout.txt"), CREATE, APPEND)) {
          try (OutputStream events = buildEventingStream(stdout)) {
            server = serverStart.apply(events);
            returnValue = (Boolean)invokeOnObject(server, "waitUntilShutdown");
            didTerminateWithStatus(returnValue);
            harnessLogger.output("server process exit.");
          } catch (Exception e) {
            didTerminateWithException(e);
          }
        } catch (IOException io) {
          throw new UncheckedIOException(io);
        }
      }
    }
  }

  @Override
  public String toString() {
    return "Server " + this.serverName + "-" + this.getCurrentState() + " (has been zapped: " + this.wasZapped + ")";
  }
}
