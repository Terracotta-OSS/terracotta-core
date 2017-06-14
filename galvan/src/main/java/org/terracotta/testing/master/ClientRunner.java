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
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Vector;

import org.terracotta.ipceventbus.proc.AnyProcess;
import org.terracotta.ipceventbus.proc.AnyProcessBuilder;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseManager;
import org.terracotta.testing.logging.VerboseOutputStream;


public class ClientRunner extends Thread {
  private final ContextualLogger harnessLogger;
  private final ContextualLogger clientProcessLogger;
  private final IMultiProcessControl control;
  private final File clientWorkingDirectory;
  private final String clientClassPath;
  private final int debugPort;
  private final String clientClassName;
  private final List<String> extraArguments;
  
  // TODO:  Manage these files at a higher-level, much like ServerProcess does, so that open/close isn't done here.
  private FileOutputStream logFileOutput;
  private FileOutputStream logFileError;
  private VerboseOutputStream stdoutLog;
  private VerboseOutputStream stderrLog;
  private AnyProcess process;
  private Listener listener;
  private boolean initialized;

  public ClientRunner(VerboseManager clientVerboseManager, IMultiProcessControl control, File clientWorkingDirectory, String clientClassPath, int debugPort, String clientClassName, List<String> extraArguments) {
    // We just want to create the harness logger and the one for the inferior process but then discard the verbose manager.
    this.harnessLogger = clientVerboseManager.createHarnessLogger();
    this.clientProcessLogger = clientVerboseManager.createClientLogger();
    
    this.control = control;
    this.clientWorkingDirectory = clientWorkingDirectory;
    this.clientClassPath = clientClassPath;
    this.debugPort = debugPort;
    this.clientClassName = clientClassName;
    this.extraArguments = extraArguments;
  }

  public void openStandardLogFiles() throws FileNotFoundException {
    Assert.assertNull(this.logFileOutput);
    Assert.assertNull(this.logFileError);
    
    // We want to create an output log file for both STDOUT and STDERR.
    this.logFileOutput = new FileOutputStream(new File(this.clientWorkingDirectory, "stdout.log"));
    this.logFileError = new FileOutputStream(new File(this.clientWorkingDirectory, "stderr.log"));
  }

  public void closeStandardLogFiles() throws IOException {
    Assert.assertNull(this.stderrLog);
    Assert.assertNull(this.stdoutLog);
    Assert.assertNotNull(this.logFileOutput);
    Assert.assertNotNull(this.logFileError);
    
    this.logFileOutput.close();
    this.logFileOutput = null;
    this.logFileError.close();
    this.logFileError = null;
  }

  @Override
  public void run() {
    // We over-ride the Thread.run() since we want to provide a few synchronization points, thus requiring that we _are_ a Thread instead of just a Runnable.
    // We assume that the listener must have been set, prior to starting the thread.
    Assert.assertNotNull(this.listener);
    
    // First step is we need to set up the verbose output stream to point at the log files.
    Assert.assertNull(this.stderrLog);
    Assert.assertNull(this.stdoutLog);
    Assert.assertNotNull(this.logFileOutput);
    Assert.assertNotNull(this.logFileError);
    this.stdoutLog = new VerboseOutputStream(this.logFileOutput, this.clientProcessLogger, false);
    this.stderrLog = new VerboseOutputStream(this.logFileError, this.clientProcessLogger, true);
    
    // Start the process, passing back the pid.
    long thePid = startProcess();
    notifyInitializationCompletion();
    // Report our PID.
    this.harnessLogger.output("PID: " + thePid);
    
    // Note that the ClientEventManager will synthesize actual events from the output stream within ITS OWN THREAD.
    // That means that here we just need to wait on termination.
    
    // Wait for the process to complete, passing back the return value.
    int theResult = waitForTermination();
    if (0 == theResult) {
      this.harnessLogger.output("Return value (normal): " + theResult);
    } else {
      this.harnessLogger.error("Return value (ERROR): " + theResult);
    }
    
    // Drop our verbose output stream shims.
    this.stdoutLog = null;
    this.stderrLog = null;
    
    // Report the termination details, before exiting (the receiver will also know that they can join on us).
    this.listener.clientDidTerminate(this, theResult);
  }

  /**
   * Called to force the client process to terminate.
   * 
   * NOTE:  The caller is still expected to join on the thread shutting down.
   */
  public void forceTerminate() {
    // Force the process to terminate.
    // (if the process already terminated, this will have no effect).
    waitForInitializationCompletion();
    this.process.destroyForcibly();
  }
  
  public synchronized void setListener(Listener listener) {
    Assert.assertNull(this.listener);
    this.listener = listener;
  }

  // Returns the PID.
  private long startProcess() {
    Assert.assertNotNull(this.stdoutLog);
    Assert.assertNotNull(this.stderrLog);
    
    PipedInputStream readingEnd = new PipedInputStream();
    // Note that ClientEventManager will be responsible for closing writingEnd.
    PipedOutputStream writingEnd = null;
    try {
      writingEnd = new PipedOutputStream(readingEnd);
    } catch (IOException e) {
      // An error here would mean an bug in the code.
      Assert.unexpected(e);
    }
    ClientEventManager eventManager = new ClientEventManager(this.control, writingEnd, this.stdoutLog);
    OutputStream outputStream = eventManager.getEventingStream();
    AnyProcessBuilder<?> processBuilder = AnyProcess.newBuilder();
    // Figure out if we want to enable debug.
    if (0 != this.debugPort) {
      // Enable debug.
      String debugArg = "-Xrunjdwp:transport=dt_socket,server=y,address=" + this.debugPort;
      String[] commandLine = buildCommandLine(debugArg);
      processBuilder.command(commandLine);
      this.harnessLogger.output("Starting: " + condenseCommandLine(commandLine));
      // Specifically point out that we are starting with debug.
      this.harnessLogger.output("NOTE:  Starting client with debug port: " + this.debugPort);
    } else {
      // No debug.
      String debugArg = null;
      String[] commandLine = buildCommandLine(debugArg);
      processBuilder.command(commandLine);
      this.harnessLogger.output("Starting: " + condenseCommandLine(commandLine));
    }
    this.process = processBuilder
        .workingDir(this.clientWorkingDirectory)
        .pipeStdin(readingEnd)
        .pipeStdout(outputStream)
        .pipeStderr(this.stderrLog)
        .build();
    this.harnessLogger.output("Client running");
    return this.process.getPid();
  }

  private String[] buildCommandLine(String debugArg) {
    List<String> fullCommandLine = new Vector<String>();
    fullCommandLine.add("java");
    // Note that we will currently set the clients at 64m.
    // TODO:  Find a way to expose the heap sizing options to the test configuration.
    fullCommandLine.add("-Xms64m");
    fullCommandLine.add("-Xmx64m");
    if (null != debugArg) {
      fullCommandLine.add("-Xdebug");
      fullCommandLine.add(debugArg);
    }
    fullCommandLine.add("-cp");
    fullCommandLine.add(this.clientClassPath);
    fullCommandLine.add(this.clientClassName);
    fullCommandLine.addAll(this.extraArguments);
    return fullCommandLine.toArray(new String[fullCommandLine.size()]);
  }

  private int waitForTermination() {
    // Terminate the process.
    int retVal = -1;
    while (-1 == retVal) {
      try {
        retVal = this.process.waitFor();
      } catch (java.util.concurrent.CancellationException e) {
        retVal = this.process.exitValue();
      } catch (InterruptedException e) {
        // TODO:  Figure out how we want to handle the interruption, here.  For now, we will just spin since we are background.
        e.printStackTrace();
      }
    }
    return retVal;
  }

  private static String condenseCommandLine(String[] args) {
    // We always start with the raw command.
    String command = args[0];
    for (int i = 1; i < args.length; ++i) {
      command += " \"" + args[i] + "\"";
    }
    return command;
  }


  private synchronized void waitForInitializationCompletion() {
    while (!this.initialized) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private synchronized void notifyInitializationCompletion() {
    this.initialized = true;
    this.notifyAll();
  }


  /**
   * NOTE:  Messages related to the client life-cycle are all posted within the client's thread.
   */
  public static interface Listener {
    void clientDidTerminate(ClientRunner clientRunner, int theResult);
  }
}
