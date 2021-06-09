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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.joining;
import org.terracotta.ipceventbus.event.Event;
import org.terracotta.ipceventbus.event.EventBus;
import org.terracotta.ipceventbus.event.EventListener;
import org.terracotta.ipceventbus.proc.AnyProcess;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.common.SimpleEventingStream;
import static org.terracotta.testing.demos.TestHelpers.isWindows;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseOutputStream;


public abstract class AbstractServerProcess implements IGalvanServer {
  private final StateInterlock stateInterlock;
  private final ITestStateManager stateManager;
  private final ContextualLogger harnessLogger;
  private final ContextualLogger serverLogger;
  private final String serverName;
  // make sure only one caller is messing around on the process
  private final Semaphore oneUser = new Semaphore(1);

  private UUID userToken;

  // When we are going to bring down the server, we need to record that we expected the crash so we don't conclude the test failed.
  private boolean isCrashExpected;

  private ServerMode currentState = ServerMode.TERMINATED;

  public AbstractServerProcess(StateInterlock stateInterlock, ITestStateManager stateManager, ContextualLogger harnessLogger, ContextualLogger serverLogger, String serverName) {
    this.stateInterlock = stateInterlock;
    this.stateManager = stateManager;
    this.harnessLogger = harnessLogger;
    this.serverLogger = serverLogger;
    this.serverName = serverName;
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
  public abstract void start() throws IOException;

  private synchronized boolean isCrashExpected() {
    return this.isCrashExpected;
  }

  public synchronized void setCrashExpected(boolean expect) {
    this.isCrashExpected = expect;
  }

  private synchronized ServerMode setCurrentState(ServerMode mode) {
    ServerMode previous = currentState;
    currentState = mode;
    notifyAll();
    return previous;
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

  @Override
  public synchronized ServerMode getCurrentState() {
    return currentState;
  }

  @Override
  public String toString() {
    return "Server " + this.serverName + " (" + getCurrentState() + ")";
  }
}
