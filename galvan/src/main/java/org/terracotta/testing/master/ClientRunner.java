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

import org.terracotta.ipceventbus.proc.AnyProcess;
import org.terracotta.ipceventbus.proc.AnyProcessBuilder;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ILogger;


public class ClientRunner extends Thread {
  private final ILogger logger;
  private final IMultiProcessControl control;
  private final File clientWorkingDirectory;
  private final String clientClassPath;
  private final String clientClassName;
  private final String clientTask;
  private final String testClassName;
  private final String connectUri;
  private final int debugPort;
  
  private FileOutputStream stdoutLog;
  private FileOutputStream stderrLog;
  private AnyProcess process;
  
  // Data which we need to pass back to the other thread.
  private Object waitMonitor = new Object();
  private FileNotFoundException setupFilesException;
  private IOException closeException;
  private long pid = -1;
  private int result = -1;

  public ClientRunner(ILogger logger, IMultiProcessControl control, File clientWorkingDirectory, String clientClassPath, String clientClassName, String clientTask, String testClassName, String connectUri, int debugPort) {
    this.logger = logger;
    this.control = control;
    this.clientWorkingDirectory = clientWorkingDirectory;
    this.clientClassPath = clientClassPath;
    this.clientClassName = clientClassName;
    this.clientTask = clientTask;
    this.testClassName = testClassName;
    this.connectUri = connectUri;
    this.debugPort = debugPort;
  }

  @Override
  public void run() {
    // We over-ride the Thread.run() since we want to provide a few synchronization points, thus requiring that we _are_ a Thread instead of just a Runnable.
    // First, we set up the log files.
    boolean canContinue = false;
    try {
      setupStandardLogFiles();
      canContinue = true;
    } catch (FileNotFoundException e) {
      synchronized(this.waitMonitor) {
        this.setupFilesException = e;
        this.waitMonitor.notifyAll();
      }
    }
    if (canContinue) {
      // Start the process, passing back the pid.
      long thePid = startProcess();
      synchronized(this.waitMonitor) {
        this.pid = thePid;
        this.waitMonitor.notifyAll();
      }
      
      // Note that the ClientEventManager will synthesize actual events from the output stream within ITS OWN THREAD.
      // That means that here we just need to wait on termination.
      
      // Wait for the process to complete, passing back the return value.
      int theResult = -1;
      IOException failure = null;
      try {
        theResult = waitForTermination();
      } catch (IOException e) {
        failure = e;
      }
      // Whatever happened, synchronize and terminate.
      synchronized(this.waitMonitor) {
        this.result = theResult;
        this.closeException = failure;
        this.waitMonitor.notifyAll();
      }
    }
  }

  public long waitForPid() throws FileNotFoundException, InterruptedException {
    long pid = -1;
    synchronized(this.waitMonitor) {
      while ((-1 == this.pid) && (null == this.setupFilesException)) {
        this.waitMonitor.wait();
      }
      if (null != this.setupFilesException) {
        throw this.setupFilesException;
      } else {
        pid = this.pid;
      }
    }
    // Report our PID.
    this.logger.log("PID: " + pid);
    return pid;
  }

  public int waitForJoinResult() throws FileNotFoundException, IOException, InterruptedException {
    // We can just join on the thread and then read the state without the waitMonitor.
    this.join();
    // Now, read the result.
    int result = -1;
    if (null != this.setupFilesException) {
      throw this.setupFilesException;
    } else if (null != this.closeException) {
        throw this.closeException;
    } else {
      result = this.result;
    }
    if (0 == result) {
      this.logger.log("Return value (normal): " + result);
    } else {
      this.logger.fatal("Return value (ERROR): " + result);
    }
    return result;
  }

  /**
   * Called to force the client process to terminate and then wait for the thread managing the runner to exit.
   */
  public void forceTerminate() {
    // Force the process to terminate.
    this.process.destroyForcibly();
    // We still want to wait for the thread managing the runner to terminate gracefully.
    try {
      this.join();
    } catch (InterruptedException e) {
      // This is really not expected since this is already in the interruption path.
      Assert.unexpected(e);
    }
  }

  private void setupStandardLogFiles() throws FileNotFoundException {
    Assert.assertNull(this.stdoutLog);
    Assert.assertNull(this.stderrLog);
    
    // We want to create an output log file for both STDOUT and STDERR.
    this.stdoutLog = new FileOutputStream(new File(this.clientWorkingDirectory, "stdout.log"));
    this.stderrLog = new FileOutputStream(new File(this.clientWorkingDirectory, "stderr.log"));
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
    // Java args:
    // [0] - client task (SETUP, TEST, or DESTROY)
    // [1] - full name of test class
    // [2] - connect URI
    // Figure out if we want to enable debug.
    if (0 != this.debugPort) {
      // Enable debug.
      String serverLine = "-Xrunjdwp:transport=dt_socket,server=y,address=" + this.debugPort;
      processBuilder.command("java", "-Xdebug", serverLine, "-cp", this.clientClassPath, this.clientClassName, this.clientTask, this.testClassName, this.connectUri);
      this.logger.log("Starting: " + condenseCommandLine("java", "-Xdebug", serverLine, "-cp", this.clientClassPath, this.clientClassName, this.clientTask, this.testClassName, this.connectUri));
      // Specifically point out that we are starting with debug.
      this.logger.log("NOTE:  Starting client with debug port: " + this.debugPort);
    } else {
      // No debug.
      processBuilder.command("java", "-cp", this.clientClassPath, this.clientClassName, this.clientTask, this.testClassName, this.connectUri);
      this.logger.log("Starting: " + condenseCommandLine("java", "-cp", this.clientClassPath, this.clientClassName, this.clientTask, this.testClassName, this.connectUri));
    }
    this.process = processBuilder
        .workingDir(this.clientWorkingDirectory)
        .pipeStdin(readingEnd)
        .pipeStdout(outputStream)
        .pipeStderr(this.stderrLog)
        .build();
    this.logger.log("Client running");
    return this.process.getPid();
  }

  private int waitForTermination() throws IOException {
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
    this.process = null;
    this.stdoutLog.close();
    this.stdoutLog = null;
    this.stderrLog.close();
    this.stderrLog = null;
    return retVal;
  }

  private static String condenseCommandLine(String... args) {
    // We always start with the raw command.
    String command = args[0];
    for (int i = 1; i < args.length; ++i) {
      command += " \"" + args[i] + "\"";
    }
    return command;
  }
}
