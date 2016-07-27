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
  private static enum ServerState {
    UNKNOWN,
    UNEXPECTED_CRASH,
    ACTIVE,
    PASSIVE,
  };
  private final ContextualLogger harnessLogger;
  private final ContextualLogger serverLogger;
  private final ServerInstallation underlyingInstallation;
  private final String serverName;
  private final File serverWorkingDirectory;
  private OutputStream stdoutLog;
  private OutputStream stderrLog;
  private ServerState state;
  private final ExitWaiter exitWaiter;
  private final int debugPort;
  private final String eyeCatcher;

  public ServerProcess(VerboseManager serverVerboseManager, ITestStateManager stateManager, ServerInstallation underlyingInstallation, String serverName, File serverWorkingDirectory, int debugPort) {
    // We just want to create the harness logger and the one for the inferior process but then discard the verbose manager.
    this.harnessLogger = serverVerboseManager.createHarnessLogger();
    this.serverLogger = serverVerboseManager.createServerLogger();
    
    this.underlyingInstallation = underlyingInstallation;
    this.serverName = serverName;
    this.serverWorkingDirectory = serverWorkingDirectory;
    // Start in the unknown state and we will wait for the stream scraping to determine our actual state.
    this.state = ServerState.UNKNOWN;
    // Because a server can crash at any time, not just when we are expecting it to, we need a thread to wait on this operation and notify stateManager if the
    // crash was not expected.
    // We also pass in something to wait on its state to notify us, in the event of an unexpected crash, so that nobody keeps waiting, out here.
    this.exitWaiter = new ExitWaiter(stateManager, new ActivePassiveEventWaiter(ServerState.UNEXPECTED_CRASH));
    this.debugPort = debugPort;
    // Create our eye-catcher for looking up sub-processes.
    this.eyeCatcher = UUID.randomUUID().toString();
  }

  public ServerInstallation getUnderlyingInstallation() {
    return this.underlyingInstallation;
  }

  public void openLogs() throws FileNotFoundException {
    Assert.assertNull(this.stdoutLog);
    Assert.assertNull(this.stderrLog);
    
    // rawOut closed by stdoutLog
    @SuppressWarnings("resource")
    FileOutputStream rawOut = new FileOutputStream(new File(this.serverWorkingDirectory, "stdout.log"));
    // rawErr closed by stderrLog
    @SuppressWarnings("resource")
    FileOutputStream rawErr = new FileOutputStream(new File(this.serverWorkingDirectory, "stderr.log"));
    // We also want to stream output going to these files to the server's logger.
    this.stdoutLog = new VerboseOutputStream(rawOut, this.serverLogger, false);
    this.stderrLog = new VerboseOutputStream(rawErr, this.serverLogger, true);

  }

  public void closeLogs() {
    Assert.assertNotNull(this.stdoutLog);
    Assert.assertNotNull(this.stderrLog);
    
    try {
      this.stdoutLog.close();
      this.stdoutLog = null;
      this.stderrLog.close();
      this.stderrLog = null;
    } catch (IOException e) {
      // Not expected in this framework.
      Assert.unexpected(e);
    }
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
    String activeReadyName = "ACTIVE";
    String passiveReadyName = "PASSIVE";
    Map<String, String> eventMap = new HashMap<String, String>();
    eventMap.put("Terracotta Server instance has started up as ACTIVE node", activeReadyName);
    eventMap.put("Moved to State[ PASSIVE-STANDBY ]", passiveReadyName);
    
    SimpleEventingStream outputStream = new SimpleEventingStream(serverBus, eventMap, this.stdoutLog);
    serverBus.on(activeReadyName, new ActivePassiveEventWaiter(ServerState.ACTIVE));
    serverBus.on(passiveReadyName, new ActivePassiveEventWaiter(ServerState.PASSIVE));
    
    // Check to see if we need to explicitly set the JAVA_HOME environment variable or it if already exists.
    String javaHome = getJavaHome();
    
    // Put together any additional options we wanted to pass to the VM under the start script.
    String javaArguments = getJavaArguments(this.debugPort);
    
    // Get the command to invoke the script.
    String startScript = getStartScriptCommand();
    
    PidCapture capture = new PidCapture();
    eventMap.put("PID is", "PID");
    serverBus.on("PID", capture);
    
    AnyProcess process = AnyProcess.newBuilder()
        .command(startScript, "-n", this.serverName, this.eyeCatcher)
        .workingDir(this.serverWorkingDirectory)
        .env("JAVA_HOME", javaHome)
        .env("JAVA_OPTS", javaArguments)
        .pipeStdout(outputStream)
        .pipeStderr(this.stderrLog)
        .build();
    // We will now hand off the process to the ExitWaiter.
    long sPid = capture.waitPid();
    if (sPid == 0) {
      throw new RuntimeException("unable to find PID in scraped section " + capture.getDebuggingSection());
    }
    this.exitWaiter.startBackgroundWait(process, sPid);
    return process.getPid();
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
    // Note that we currently want to scale down our heap to 128M since our tests are simple.
    // TODO:  Find a way to expose this to the test being run.
    javaOpts += " -Xms128m -Xms128m";
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
  
  private class PidCapture implements EventListener {
    
    private long serverPid = -1;
    private String section;

    @Override
    public void onEvent(Event event) throws Throwable {
        String line = event.getData(String.class);
        Matcher m = Pattern.compile("PID is ([0-9]*)").matcher(line);
        if (m.find()) {
          section = m.group();
          try {
            String pid = m.group(1);
            setPid(Long.parseLong(pid));          
          } catch (NumberFormatException format) {
            setPid(0);          
          }
        } else {
          setPid(0);          
        }
    }
    
    private synchronized void setPid(long pid) {
      serverPid = pid;
      this.notifyAll();
    }
    
    public synchronized long waitPid() {
      try {
        while (this.serverPid < 0) {
          this.wait();
        }
        return this.serverPid;
      } catch (InterruptedException ie) {
        throw new RuntimeException(ie);
      }
    }
    
    public String getDebuggingSection() {
      return section;
    }
    
  }

  private class ExitWaiter extends Thread {
    private final ITestStateManager stateManager;
    private final ActivePassiveEventWaiter crashWaiter;
    private long serverPid;
    private AnyProcess process;
    private boolean isCrashExpected;
    private int returnValue;
    private boolean didExit;
    
    public ExitWaiter(ITestStateManager stateManager, ActivePassiveEventWaiter crashWaiter) {
      this.stateManager = stateManager;
      this.crashWaiter = crashWaiter;
      this.returnValue = -1;
      this.didExit = false;
    }
    public void startBackgroundWait(AnyProcess process, long serverPid) {
      Assert.assertNull(this.process);
      this.process = process;
      this.serverPid = serverPid;
      this.start();
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
      // If we send the failure, we don't want to do it under lock.
      boolean shouldSendFailure = false;
      synchronized(this) {
        this.returnValue = returnValue;
        this.didExit = true;
        // See if this crash was expected.
        if (this.isCrashExpected) {
          // This means that someone is waiting for us.
          this.notifyAll();
        } else {
          // We weren't expecting this so we need to notify the crash waiter and stateManager.
          try {
            this.crashWaiter.onEvent(null);
          } catch (Throwable e) {
            // Not expected in this case.
            Assert.unexpected(e);
          }
          shouldSendFailure = true;
        }
      }
      if (shouldSendFailure) {
        this.stateManager.testDidFail();
      }
    }
    public synchronized int bringDownServer() throws InterruptedException {
      harnessLogger.output("bringing down server process");
      // Mark this as expected.
      this.isCrashExpected = true;
      // Destroy the process.
      if (TestHelpers.isWindows()){
        //kill process using taskkill command as process.destroy() doesn't terminate child processes on windows.
        killProcessWindows();
      }else {
        try {
          killProcessUnix(serverPid);
        } catch (IOException e) {
          Assert.unexpected(e);
        }
      }
      harnessLogger.output("server process killed");

      // Wait until we get a return value.
      harnessLogger.output("waiting for server process to get down");
      while (!this.didExit) {
        wait();
      }
      harnessLogger.output("server process is down");
      return this.returnValue;
    }

    private void killProcessWindows() throws InterruptedException {
      harnessLogger.output("killing windows process");
      try {
        Process p = startStandardProcess("taskkill", "/F", "/t", "/pid", String.valueOf(process.getPid()));
        // We don't care about the output but we want to make sure that the process can be terminated.
        discardProcessOutput(p);
        
        //not checking exit code here..taskkill may faill if server process was crashed during the test.
        p.waitFor();
        harnessLogger.output("killed server with PID " + process.getPid());
      }catch (IOException ex){
        Assert.unexpected(ex);
      }
    }

    private void killProcessUnix(long pid) throws InterruptedException, IOException {        
      Process killProcess = startStandardProcess("kill", String.valueOf(pid));
      // We don't care about the output but we want to make sure that the process can be terminated.
      discardProcessOutput(killProcess);
      int result = killProcess.waitFor();
      harnessLogger.output("Attempt to kill server process resulted in:" + result);
// can't assert on this any longer.  The process may have already died for other reasons
//      Assert.assertTrue(0 == result);
    }

    private void discardProcessOutput(Process process) throws IOException {
      BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      while (null != outputReader.readLine()) {
        // Read until EOF.
      }
    }

    private Process startStandardProcess(String... commandLine) throws IOException {
      ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
      processBuilder.redirectErrorStream(true);
      return processBuilder.start();
    }
  }
}
