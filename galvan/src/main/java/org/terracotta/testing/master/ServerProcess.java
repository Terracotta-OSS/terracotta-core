/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.terracotta.ipceventbus.proc.AnyProcess;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.VerboseOutputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.APPEND;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardOpenOption.CREATE;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import static org.terracotta.testing.demos.TestHelpers.isWindows;


public class ServerProcess extends ServerInstance {
  private final int heapInM;
  private final int debugPort;
  private final Properties serverProperties;
  private final Path serverInstall;
  private final Path serverWorkingDir;
  private final String[] startupCommand;
  private final OutputStream parentOutput;
  
  // OutputStreams to close when the server is down.
  private OutputStream outputStream;
  private OutputStream errorStream;
  
  private static final Set<AnyProcess> running = ConcurrentHashMap.newKeySet();
  
  static {
    Runtime.getRuntime().addShutdownHook(new Thread(()->{
        running.forEach(AnyProcess::destroy);
      })
    );
  }

  public ServerProcess(String serverName, Path serverInstall, Path serverWorkingDir, int heapInM, 
          int debugPort, Properties serverProperties, OutputStream out,
                       String[] startupCommand) {
    super(serverName);
    // We need to specify a positive integer as the heap size.
    this.heapInM = heapInM;
    this.debugPort = debugPort;
    this.serverProperties = serverProperties;
    this.serverInstall = serverInstall;
    this.serverWorkingDir = serverWorkingDir;
    this.startupCommand = startupCommand;
    this.parentOutput = out;
    // We start up in the shutdown state so notify the interlock.
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
      if (!isServerRunning()) {
        // Now, open the log files.
        // We want to create an output log file for both STDOUT and STDERR.
        // rawOut closed by stdout
        OutputStream rawOut = parentOutput == null ? Files.newOutputStream(serverWorkingDir.resolve("stdout.log"), CREATE, APPEND) : parentOutput;
        OutputStream rawErr = parentOutput == null ? Files.newOutputStream(serverWorkingDir.resolve("stderr.log"), CREATE, APPEND) : parentOutput;
        // We also want to stream output going to these files to the server's logger.
        // stdout closed by outputStream
        VerboseOutputStream stdout = new VerboseOutputStream(rawOut, this.serverLogger, false);
        VerboseOutputStream stderr = new VerboseOutputStream(rawErr, this.serverLogger, true);

        // Additionally, any information going through the stdout needs to be watched by the eventing stream for events.
        OutputStream out = buildEventingStream(stdout);

        // Check to see if we need to explicitly set the JAVA_HOME environment variable or it if already exists.
        String javaHome = getJavaHome();

        // Put together any additional options we wanted to pass to the VM under the start script.
        // Start the inferior process.

        setCurrentState(ServerMode.STARTUP);

        reset();
        
        setStreams(out, stderr);
        // The "build()" starts the process so wrap it in an exit waiter.  We can then drop it since we will can't explicitly terminate it until it reports our PID (at which point we will declare it "running").
        ExitWaiter exitWaiter = new ExitWaiter(()->AnyProcess.newBuilder()
            .command(createCommand(javaHome, serverInstall, startupCommand))
            .workingDir(this.serverWorkingDir.toFile())
            .env("JAVA_HOME", javaHome)
            .pipeStdin()
            .pipeStdout(out)
            .pipeStderr(stderr)
            .build());
        exitWaiter.start();
      }
    } finally {
      exit(token);
    }
  }
  
  private String[] createCommand(String javaHome, Path serverPath, String[] args) {
    Path sjar = serverPath.resolve("tc.jar");
    serverProperties.put("logback.configurationFile", "logback-test.xml");
    serverProperties.setProperty("tc.install-root", serverPath.toString());
    List<String> cmd = new ArrayList<>();
    cmd.add(javaHome + "/bin/java");
    cmd.addAll(Arrays.asList(getJavaArguments(debugPort)));
    cmd.add("-jar");
    cmd.add(sjar.toString());
    cmd.addAll(Arrays.asList(args));
    return cmd.toArray(String[]::new);
  }

  private synchronized void setStreams(OutputStream out, OutputStream err) {
    if (outputStream != null) {
      Assert.assertNull(this.outputStream);
    }
    this.outputStream = out;
    Assert.assertNull(this.errorStream);
    this.errorStream = err;
  }


  private String[] getJavaArguments(int debugPort) {
    // We want to bootstrap the variable with whatever is in our current environment.
    String javaOpts = System.getenv("JAVA_OPTS");
    if (null == javaOpts) {
      javaOpts = "";
    }
    List<String> ops = new ArrayList<>();
    ops.add("-Xms" + this.heapInM + "m");
    ops.add("-Xmx" + this.heapInM + "m");
    if (debugPort > 0) {
      ops.add("-Xdebug");
      ops.add("-Xrunjdwp:transport=dt_socket,server=y,address=" + debugPort);
      serverLogger.output("NOTE:  Starting server \"" + this.serverName + "\" with debug port: " + debugPort);
    }
    serverProperties.entrySet().stream()
        .map(e -> "-D" + e.getKey() + "=" + e.getValue()).forEach(ops::add);
        
    return ops.toArray(String[]::new);
  }

  private String getJavaHome() {
    String javaHome = System.getProperty("java.home");
    if (null == javaHome) {
      // Use the existing JRE path from the java.home in the current JVM instance as the JAVA_HOME.
      javaHome = System.getenv("JAVA_HOME");
      // This better exist.
      Assert.assertNotNull(javaHome);
      // Log that we did this.
      this.serverLogger.output("WARNING:  JAVA_HOME not set!  Defaulting to \"" + javaHome + "\"");
    }
    return javaHome;
  }

  /**
   * Called by the exit waiter when the underlying process terminates.
   *
   * @param exitStatus The exit code of the underlying process.
   */
  private void didTerminateWithStatus(int exitStatus) {
    // See if we have a PID yet or if this was a failure, much earlier (hence, if we told the interlock that we are even running).
    GalvanFailureException failureException = null;

    setCurrentState(ServerMode.TERMINATED);

    long originalPid = this.waitForPid();
//  no matter what, the server is gone so report it to interlock
    if (!this.isCrashExpected() && originalPid == 0) {
// didn't have time to get the PID, this is not expected
      Assert.assertFalse(this.isCrashExpected()); // can't expect a crash without a PID
      failureException = new GalvanFailureException("Server crashed before reporting PID: " + this);
    }
    if (!this.isCrashExpected() && (null == failureException)) {
      failureException = new GalvanFailureException("Unexpected server crash: " + this + " (PID " + originalPid + ") status: " + exitStatus);
    }

    closeStreams();
    
    if (null != failureException) {
      this.stateManager.testDidFail(failureException);
    }

    reset();
  }

  private synchronized void closeStreams() {
    // Close the log files.
    try {
      if (this.outputStream != null) {
        this.outputStream.flush();
        this.outputStream.close();
        this.outputStream = null;
      }
      if (this.errorStream != null) {
        this.errorStream.flush();
        this.errorStream.close();
        this.errorStream = null;
      }
    } catch (IOException e) {
      // Not expected in this framework.
      Assert.unexpected(e);
    }
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
        // Can't stop something unless we determined the PID.
        long localPid = 0;
        while (localPid == 0) {
          localPid = waitForPid();
          if (localPid == 0 && !isServerRunning()) {
            //  PID could be zero if the server stops running while waiting for the PID
            return;
          }
        }
        // Log the intent.
        this.serverLogger.output("Crashing server process: " + this + " (PID " + localPid + ")");
        // Mark this as expected.
        this.setCrashExpected(true);

        Process process = null;

        // Destroy the process.
        if (isWindows()) {
          //kill process using taskkill command as process.destroy() doesn't terminate child processes on windows.
          process = killProcessWindows(localPid);
        } else {
          process = killProcessUnix(localPid);
        }
        while (process.isAlive()) {
          serverLogger.output("Waiting for server to exit PID:" + localPid);
          //  give up the synchronized lock while waiting for the kill process to
          //  do it's job.  This can deadlock since the event bus will need this lock
          //  to log events
          process.waitFor(5, TimeUnit.SECONDS);
        }
        int result = process.exitValue();
        serverLogger.output("Attempt to kill server process resulted in:" + result);
        serverLogger.output("server process killed");
      }
    } finally {
      exit(token);
    }
  }

  private Process killProcessWindows(long pid) throws InterruptedException {
    Assert.assertTrue(pid != 0);
    serverLogger.output("killing windows process");
    Process p = startStandardProcess("taskkill", "/F", "/t", "/pid", String.valueOf(pid));
    // We don't care about the output but we want to make sure that the process can be terminated.
    discardProcessOutput(p);

    //not checking exit code here..taskkill may faill if server process was crashed during the test.
    serverLogger.output("killed server with PID " + pid);
    return p;
  }

  private Process killProcessUnix(long pid) throws InterruptedException {
    Assert.assertTrue(pid != 0);
    Process killProcess = startStandardProcess("kill", String.valueOf(pid));
    // We don't care about the output but we want to make sure that the process can be terminated.
    discardProcessOutput(killProcess);

    serverLogger.output("killed server with PID " + pid);
    // (note that the server may have raced to die so we can't assert that the kill succeeded)
    return killProcess;
  }

  private void discardProcessOutput(Process process) {
    BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    try {
      while (null != outputReader.readLine()) {
        // Read until EOF.
      }
    } catch (IOException e) {
      // We don't expect an IOException when reading an inter-process pipe.
      Assert.unexpected(e);
    }
  }

  private Process startStandardProcess(String... commandLine) {
    ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
    processBuilder.redirectErrorStream(true);
    Process process = null;
    try {
      process = processBuilder.start();
    } catch (IOException e) {
      // This is unexpected for our uses (such a low-level command).
      Assert.unexpected(e);
    }
    return process;
  }

  private class ExitWaiter extends Thread {
    private final Supplier<AnyProcess> process;

    public ExitWaiter(Supplier<AnyProcess> process) {
      this.process = process;
    }

    @Override
    public void run() {
      int returnValue = 11;
      while (returnValue == 11) {
        AnyProcess instance = this.process.get();
        try {
          running.add(instance);          
          returnValue = instance.waitFor();
          serverLogger.output("server process died with rc=" + returnValue);
        } catch (java.util.concurrent.CancellationException e) {
          returnValue = instance.exitValue();
        } catch (InterruptedException e) {
          // We don't expect interruption in this part of the test - we need to wait for the termination.
          Assert.unexpected(e);
          returnValue = 1;
        } catch (Throwable t) {
          Assert.unexpected(t);
          returnValue = 1;
        } finally {
          running.remove(instance);
        }
      }
      ServerProcess.this.didTerminateWithStatus(returnValue);
    }
  }
}
