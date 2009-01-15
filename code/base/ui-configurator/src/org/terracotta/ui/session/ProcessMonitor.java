/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

/**
 * This is for monitoring a process where something else is draining the output and error streams, such as a
 * ProcessWaiter or a ProcessOutputView.
 */

public class ProcessMonitor extends Thread {
  private Process                    process;
  private ProcessTerminationListener terminationListener;

  public ProcessMonitor(Process process, ProcessTerminationListener terminationListener) {
    super();

    this.process = process;
    this.terminationListener = terminationListener;

    start();
  }

  public void run() {
    while (true) {
      try {
        process.waitFor();
        terminationListener.processTerminated(process.exitValue());
        return;
      } catch (InterruptedException ie) {/**/
      }
    }
  }
}
