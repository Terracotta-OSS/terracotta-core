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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.terracotta.ipceventbus.event.Event;
import org.terracotta.ipceventbus.event.EventBus;
import org.terracotta.ipceventbus.event.EventListener;
import org.terracotta.ipceventbus.proc.AnyProcess;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.common.SimpleEventingStream;
import org.terracotta.testing.demos.TestHelpers;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseManager;
import org.terracotta.testing.logging.VerboseOutputStream;


public class ServerProcess {
  private final GalvanStateInterlock stateInterlock;
  private final ITestStateManager stateManager;
  private final ContextualLogger harnessLogger;
  private final ContextualLogger serverLogger;
  private final String serverName;
  private final int heapInM;
  private final int debugPort;
  private final File serverWorkingDirectory;
  private final String eyeCatcher;
// make sure only one caller is messing around on the process
  private final Semaphore oneUser = new Semaphore(1);
  
  private UUID userToken;
  
  //  flag if the server was zapped so it can be logged
  private boolean wasZapped;
  
  private boolean isRunning;
  // The PID of the actual server, underneath the start script.  This is 0 until we are killable and can tell the interlock that we are running.
  private long pid;
  // When we are going to bring down the server, we need to record that we expected the crash so we don't conclude the test failed.
  private boolean isCrashExpected;
  
  // OutputStreams to close when the server is down.
  private OutputStream outputStream;
  private OutputStream errorStream;

  public ServerProcess(GalvanStateInterlock stateInterlock, ITestStateManager stateManager, VerboseManager serverVerboseManager, ServerInstallation underlyingInstallation, String serverName, File serverWorkingDirectory, int heapInM, int debugPort) {
    this.stateInterlock = stateInterlock; 
    this.stateManager = stateManager;
    // We just want to create the harness logger and the one for the inferior process but then discard the verbose manager.
    this.harnessLogger = serverVerboseManager.createHarnessLogger();
    this.serverLogger = serverVerboseManager.createServerLogger();
    
    this.serverName = serverName;
    // We need to specify a positive integer as the heap size.
    Assert.assertTrue(heapInM > 0);
    this.heapInM = heapInM;
    this.debugPort = debugPort;
    this.serverWorkingDirectory = serverWorkingDirectory;
    // Create our eye-catcher for looking up sub-processes.
    this.eyeCatcher = UUID.randomUUID().toString();
    
    // We start up in the shutdown state so notify the interlock.
    this.stateInterlock.registerNewServer(this);
  }
  /**
   * enter/exit is used by start and stop to make sure those methods are called one
   * at a time.  
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
   * 
   * Note that the start attempt returns, immediately, and the server will report its state transition through the
   * GalvanStateInterlock.  Specifically, the server starts as "terminated" when it is first registered.  Once the
   * sub-process is able to start up and we know its PID, it will become "unknownRunning".
   * Note that it is possible for the server to fail to start up, signalling a test failure in the interlock.
   * 
   * Note that we synchronize this call so that so that no events can come in, asynchronously, while we are starting this up (since all events which originate in this server should be in a well-defined order).
   * 
   * @throws FileNotFoundException The logs couldn't be created since the server's working directory is missing.
   */
  public void start() throws FileNotFoundException {
    UUID token = enter();
    try {
    // First thing we need to do is make sure that we aren't already running.
    Assert.assertFalse(this.stateInterlock.isServerRunning(this));
    
    // Now, open the log files.
    // We want to create an output log file for both STDOUT and STDERR.
    // rawOut closed by stdout
    @SuppressWarnings("resource")
    FileOutputStream rawOut = new FileOutputStream(new File(this.serverWorkingDirectory, "stdout.log"));
    // rawErr closed by stderr
    @SuppressWarnings("resource")
    FileOutputStream rawErr = new FileOutputStream(new File(this.serverWorkingDirectory, "stderr.log"));
    // We also want to stream output going to these files to the server's logger.
    // stdout closed by outputStream
    @SuppressWarnings("resource")
    VerboseOutputStream stdout = new VerboseOutputStream(rawOut, this.serverLogger, false);
    VerboseOutputStream stderr = new VerboseOutputStream(rawErr, this.serverLogger, true);
    
    // Additionally, any information going through the stdout needs to be watched by the eventing stream for events.
    SimpleEventingStream outputStream = buildEventingStream(stdout);
    
    // Check to see if we need to explicitly set the JAVA_HOME environment variable or it if already exists.
    String javaHome = getJavaHome();
    
    // Put together any additional options we wanted to pass to the VM under the start script.
    String javaArguments = getJavaArguments(this.debugPort);
    
    // Get the command to invoke the script.
    String startScript = getStartScriptCommand();
    
    // Start the inferior process.
    AnyProcess process = AnyProcess.newBuilder()
        .command(startScript, "-n", this.serverName, this.eyeCatcher)
        .workingDir(this.serverWorkingDirectory)
        .env("JAVA_HOME", javaHome)
        .env("JAVA_OPTS", javaArguments)
        .pipeStdout(outputStream)
        .pipeStderr(stderr)
        .build();
    
    reset(true);
    this.stateInterlock.serverDidStartup(this);
    Assert.assertNull(this.outputStream);
    this.outputStream = outputStream;
    Assert.assertNull(this.errorStream);
    this.errorStream = stderr;
    
    // The "build()" starts the process so wrap it in an exit waiter.  We can then drop it since we will can't explicitly terminate it until it reports our PID (at which point we will declare it "running").
    ExitWaiter exitWaiter = new ExitWaiter(process);
    exitWaiter.start();
    } finally {
      exit(token);
    }
  }
  
  private synchronized boolean isCrashExpected() {
    return this.isCrashExpected;
  }
  
  private synchronized void setCrashExpected() {
    Assert.assertFalse(this.isCrashExpected);
    this.isCrashExpected = true;
  }
  
  private synchronized long waitForPid(){
    try {
      while (this.isRunning && this.pid == 0) {
        this.wait();
      }
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    }
    return this.pid;
  }
  
  private synchronized void reset(boolean running) {
    // We aren't expecting a crash and we are now running.
    this.isCrashExpected = false;
    this.pid = 0;
    this.wasZapped = this.isRunning;
    this.isRunning = running;
    notifyAll();
  }

  private String getStartScriptCommand() {
    String startScript;
    if (TestHelpers.isWindows()){
      //There are illegal characters "(" and ")" in folder names that cause the path to be truncated and the server won't start.
      //So we need to wrap double quotes around startScript absolute file path.
      startScript = "\"" + new File(this.serverWorkingDirectory,"server\\bin\\start-tc-server.bat").getAbsolutePath() + "\"";
    }else {
      startScript = "server/bin/start-tc-server.sh";
    }
    return startScript;
  }

  private String getJavaArguments(int debugPort) {
    // We want to bootstrap the variable with whatever is in our current environment.
    String javaOpts = System.getenv("JAVA_OPTS");
    if (null == javaOpts) {
      javaOpts = "";
    }
    javaOpts += " -Xms" + this.heapInM + "m -Xmx" + this.heapInM + "m";
    if (debugPort > 0) {
      // Set up the client to block while waiting for connection.
      javaOpts += " -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=" + debugPort;
      // Log that debug is enabled.
      this.harnessLogger.output("NOTE:  Starting server \"" + this.serverName + "\" with debug port: " + debugPort);
    }
    return javaOpts;
  }

  private String getJavaHome() {
    String javaHome = System.getenv("JAVA_HOME");
    if (null == javaHome) {
      // Use the existing JRE path from the java.home in the current JVM instance as the JAVA_HOME.
      javaHome = System.getProperty("java.home");
      // This better exist.
      Assert.assertNotNull(javaHome);
      // Log that we did this.
      this.harnessLogger.output("WARNING:  JAVA_HOME not set!  Defaulting to \"" + javaHome + "\"");
    }
    return javaHome;
  }

  private SimpleEventingStream buildEventingStream(VerboseOutputStream stdout) {
    // Now, set up the event bus we will use to scrape the state from the sub-process.
    EventBus serverBus = new EventBus.Builder().id("server-bus").build();
    String pidEventName = "PID";
    String activeReadyName = "ACTIVE";
    String passiveReadyName = "PASSIVE";
    String zapEventName = "ZAP";
    Map<String, String> eventMap = new HashMap<String, String>();
    eventMap.put("PID is", pidEventName);
    eventMap.put("Terracotta Server instance has started up as ACTIVE node", activeReadyName);
    eventMap.put("console - Moved to State[ PASSIVE-STANDBY ]", passiveReadyName);
    eventMap.put("Restarting the server", zapEventName);
    
    // We will attach the event stream to the stdout.
    SimpleEventingStream outputStream = new SimpleEventingStream(serverBus, eventMap, stdout);
    serverBus.on(pidEventName, new EventListener() {
      @Override
      public void onEvent(Event event) throws Throwable {
        String line = event.getData(String.class);
        Matcher m = Pattern.compile("PID is ([0-9]*)").matcher(line);
        if (m.find()) {
          try {
            String pid = m.group(1);
            ServerProcess.this.didStartWithPid(Long.parseLong(pid));
          } catch (NumberFormatException format) {
            Assert.unexpected(format);
          }
        } else {
          // This is a little unusual, since it is a partial match, so at least log it in case something is wrong.
          ServerProcess.this.harnessLogger.error("Unexpected PID-like line from server: " + line);
        }
      }});
    serverBus.on(activeReadyName, new EventListener() {
      @Override
      public void onEvent(Event event) throws Throwable {
        ServerProcess.this.didBecomeActive(true);
      }});
    serverBus.on(passiveReadyName, new EventListener() {
      @Override
      public void onEvent(Event event) throws Throwable {
        ServerProcess.this.didBecomeActive(false);
      }});
    serverBus.on(zapEventName, new EventListener() {
      @Override
      public void onEvent(Event event) throws Throwable {
        ServerProcess.this.instanceWasZapped();
      }});
    return outputStream;
  }

  /**
   * Called by the inline EventListener implementations when the server becomes either active or passive.
   * 
   * @param isActive True if active, false if passive.
   */
  private void didBecomeActive(boolean isActive) {
    if (isActive) {
      this.stateInterlock.serverBecameActive(this);
    } else {
      this.stateInterlock.serverBecamePassive(this);
    }
  }

  /**
   * Called by the inline EventListener when the instance goes down for a restart due to ZAP.
   * This is really just a special case of a shut-down (we accept it, even if we weren't expecting it).
   */
  private void instanceWasZapped() {
    this.harnessLogger.output("Server restarted due to ZAP");
    reset(true);
    this.stateInterlock.serverWasZapped(this);
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

  /**
   * Called by the exit waiter when the underlying process terminates.
   * 
   * @param exitStatus The exit code of the underlying process.
   */
  private void didTerminateWithStatus(int exitStatus) {
    // See if we have a PID yet or if this was a failure, much earlier (hence, if we told the interlock that we are even running).
    GalvanFailureException failureException = null;
    long originalPid = this.waitForPid();
//  no matter what, the server is gone so report it to interlock
    this.stateInterlock.serverDidShutdown(this);
    if (!this.isCrashExpected() && originalPid == 0) {
// didn't have time to get the PID, this is not expected
      Assert.assertFalse(this.isCrashExpected()); // can't expect a crash without a PID
      failureException = new GalvanFailureException("Server crashed before reporting PID: " + this);
    }
    if (!this.isCrashExpected() && (null == failureException)) {
      failureException = new GalvanFailureException("Unexpected server crash: " + this + " (PID " + originalPid + ") status: " + exitStatus);
    }
    
    if (null != failureException) {
      this.stateManager.testDidFail(failureException);
    }
    // In either case, we are not running.
    reset(false);
    // Close the log files.
    try {
      this.outputStream.close();
      this.outputStream = null;
      this.errorStream.close();
      this.errorStream = null;
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
    // Can't stop something now running.
    Assert.assertTrue(this.stateInterlock.isServerRunning(this));
    // Can't stop something unless we determined the PID.
    long localPid = waitForPid();
    Assert.assertTrue(localPid > 0);
    // Log the intent.
    this.harnessLogger.output("Crashing server process: " + this + " (PID " + localPid + ")");
    // Mark this as expected.
    this.setCrashExpected();
    
    Process process = null;
    // Destroy the process.
    if (TestHelpers.isWindows()){
      //kill process using taskkill command as process.destroy() doesn't terminate child processes on windows.
      process = killProcessWindows(this.pid);
    } else {
      process = killProcessUnix(this.pid);
    }
    while (process.isAlive()) {
      harnessLogger.output("Waiting for server to exit PID:" + localPid);
 //  give up the synchronized lock while waiting for the kill process to 
 //  do it's job.  This can deadlock since the event bus will need this lock 
 //  to log events
      process.waitFor(1, TimeUnit.SECONDS);
    }
    int result = process.exitValue();
    harnessLogger.output("Attempt to kill server process resulted in:" + result);
    harnessLogger.output("server process killed");
    } finally {
      exit(token);
    }
  }

  private Process killProcessWindows(long pid) throws InterruptedException {
    harnessLogger.output("killing windows process");
    Process p = startStandardProcess("taskkill", "/F", "/t", "/pid", String.valueOf(pid));
    // We don't care about the output but we want to make sure that the process can be terminated.
    discardProcessOutput(p);
    
    //not checking exit code here..taskkill may faill if server process was crashed during the test.
    harnessLogger.output("killed server with PID " + pid);
    return p;
  }

  private Process killProcessUnix(long pid) throws InterruptedException {
    Process killProcess = startStandardProcess("kill", String.valueOf(pid));
    // We don't care about the output but we want to make sure that the process can be terminated.
    discardProcessOutput(killProcess);

    harnessLogger.output("killed server with PID " + pid);
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
    private AnyProcess process;
    
    public ExitWaiter(AnyProcess process) {
      this.process = process;
    }
    @Override
    public void run() {
      int returnValue = -1;
      try {
        returnValue = this.process.waitFor();
        harnessLogger.output("server process died with rc=" + returnValue);
      } catch (java.util.concurrent.CancellationException e) {
        returnValue = this.process.exitValue();
      } catch (InterruptedException e) {
        // We don't expect interruption in this part of the test - we need to wait for the termination.
        Assert.unexpected(e);
      }
      ServerProcess.this.didTerminateWithStatus(returnValue);
    }
  }

  @Override
  public String toString() {
    return "Server \"" + this.serverName + "\": " + this.eyeCatcher;
  }
}
