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

import org.terracotta.testing.common.Assert;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.testing.logging.VerboseOutputStream;



public class InlineServer extends ServerInstance {
  private static final Logger LOGGER = LoggerFactory.getLogger(InlineServer.class);
  private final Path serverInstall;
  private final Path serverWorkingDir;
  private final Properties serverProperties;
  private final OutputStream parentOutput;
  private final String[] cmd;


  private final int heap;
  private final int debug;
  private boolean leakDetected = false;
  private ServerThread serverThread;

  public InlineServer(String serverName, Path serverInstall, Path serverWorkingDir, int heap, int debug, Properties serverProperties, OutputStream out, String[] cmd) {
    super(serverName);
    // We need to specify a positive integer as the heap size.
    this.serverInstall = serverInstall;
    this.serverWorkingDir = serverWorkingDir;
    this.serverProperties = serverProperties;
    this.parentOutput = out;
    this.cmd = cmd;
    this.debug = debug;
    this.heap = heap;
  }

  public InlineServer(String serverName, Path serverInstall, Path serverWorkingDir, Properties serverProperties, OutputStream out, String[] cmd) {
    this(serverName, serverInstall, serverWorkingDir, 512, 0, serverProperties, out, cmd);
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
      Assert.assertTrue(serverThread == null || serverThread.shutdown());
      setCurrentState(ServerMode.STARTUP);
      serverThread = new ServerThread();
      serverThread.start();
    } finally {
      exit(token);
    }
  }

  @Override
  protected synchronized void reset() {
    super.reset();
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
        serverLogger.output("Crashing server process: " + serverThread);
        // Mark this as expected.
        this.setCrashExpected(true);
        boolean result;
        do {
          result = serverThread.shutdown();
        } while (!result);
        serverLogger.output("Server Stop Command Result: " + result);
      }
    } finally {
      exit(token);
    }
  }

  private static String invokeOnServerMBean(Object server, String target, String call, String arg) {
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
    server = Objects.requireNonNull(server);
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

  private class ServerThread extends Thread {

    private Object server;
    private boolean running = false;

    public ServerThread() {
      setName("ServerManagementThread - " + serverName);
    }

    @Override
    public void run() {
      boolean returnValue = true;
      while (returnValue) {
        try (OutputStream rawOut = parentOutput != null ? parentOutput : Files.newOutputStream(serverWorkingDir.resolve("stdout.txt"), CREATE, APPEND)) {
          VerboseOutputStream stdout = new VerboseOutputStream(rawOut, serverLogger, false);
          try (OutputStream events = buildEventingStream(stdout)) {
            if (initializeServer(events)) {
              serverLogger.output("starting server");
              returnValue = (Boolean)invokeOnObject(server, "waitUntilShutdown") && isRunning();
              didTerminateWithStatus(returnValue);
              serverLogger.output("server process exit. restarting:" + returnValue);
            } else {
              returnValue = false;
            }
          } catch (Exception e) {
            serverLogger.output("server process exit. error:" + e.getMessage());
            didTerminateWithException(e);
            returnValue = false;
          }
        } catch (IOException io) {
          LOGGER.warn("error", io);
          throw new UncheckedIOException(io);
        }
      }
    }

    private Object getServerObject() {
      return server;
    }

    private synchronized boolean isRunning() {
      return running;
    }

    private synchronized boolean initializeServer(OutputStream out) throws Exception {
      server = startIsolatedServer(serverWorkingDir, serverInstall, out, cmd, serverProperties);
      running = server != null;
      return running;
    }

    public synchronized boolean shutdown() {
      if (running) {
        running = false;
        String result = invokeOnServerMBean(server, "Server","stopAndWait",null);
        serverLogger.output("stopping. restart:" + result);
        detectThreadLeaks();
        return !Boolean.parseBoolean(result);
      } else {
        return true;
      }
    }
  }

  private void detectThreadLeaks() {
    ThreadGroup grp = (ThreadGroup)invokeOnObject(serverThread.getServerObject(), "getServerThreadGroup");
    List<Thread> threads = threads(grp);
    if (!threads.isEmpty()) {
      LOGGER.warn("Inline server threads are leaking:");
      LOGGER.warn("server name:{} path:{}", serverName, serverWorkingDir);
      for (Thread t : threads) {
        LOGGER.warn("thread group: {}", climbThreadGroupChain(t.getThreadGroup()));
        LOGGER.warn(getThreadDump(t));
      }
      grp.interrupt();
      leakDetected = false;
      boolean interrupted = false;
      try {
        for (Thread t : threads) {
          try {
            t.join(500L);
            leakDetected = t.isAlive() | leakDetected;
          } catch (InterruptedException ie) {
            interrupted = true;
          }
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private List<Thread> threads(ThreadGroup grp) {
    if (grp != null) {
      int ac = grp.activeCount();
      Thread[] list = new Thread[ac];
      grp.enumerate(list, true);
      return Arrays.stream(list).filter(t -> t != null && t.isAlive()).collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
  }

  private List<ThreadGroup> climbThreadGroupChain(ThreadGroup tg) {
    List<ThreadGroup> list = new ArrayList<>();
    while (tg != null) {
      list.add(tg);
      tg = tg.getParent();
    }
    Collections.reverse(list);
    return list;
  }

  private static String getThreadDump(Thread t) {
    final StringBuilder sb = new StringBuilder(100 * 1024);
    sb.append("name=");
    sb.append(t.getName());
    sb.append(" id=");
    sb.append(t.getId());
    sb.append('\n');

    final StackTraceElement[] stea = t.getStackTrace();
    for (StackTraceElement element : stea) {
      sb.append("\tat ");
      sb.append(element.toString());
      sb.append('\n');
    }
    sb.append('\n');

    return sb.toString();
  }

  @Override
  public IGalvanServer newInstance() {
    if (leakDetected) {
      LOGGER.warn("Leak detected switching to process mode");
      ServerProcess process = new ServerProcess(serverName, serverInstall, serverWorkingDir, heap,
          debug, serverProperties, parentOutput, cmd);
      process.installIntoStripe(stateInterlock, stateManager, this.serverLogger);
      return process;
    } else {
      return this;
    }
  }

  private static Object startIsolatedServer(Path serverWorking, Path server, OutputStream out, String[] cmd, Properties serverProperties) {
    ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
    Path tc = server.resolve("tc.jar");
    try {
      if (serverProperties != null) {
          for (String key : serverProperties.stringPropertyNames()) {
              System.setProperty(key, serverProperties.getProperty(key));
          }
      }
      Thread.currentThread().setContextClassLoader(null);
      URL url = tc.toUri().toURL();
      URL resource = serverWorking.toUri().toURL();
      ClassLoader loader = new IsolatedClassLoader(new URL[] {resource, url}, InlineServer.class.getClassLoader());
      Method m = Class.forName("com.tc.server.TCServerMain", true, loader).getMethod("createServer", List.class, OutputStream.class);
      return m.invoke(null, Arrays.asList(cmd), out);
    } catch (RuntimeException mal) {
      throw mal;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      Thread.currentThread().setContextClassLoader(oldLoader);
    }
  }
}
