/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

/**
 * This is for monitoring a process where something else is draining the output and
 * error streams, such as a ProcessWaiter or a ProcessOutputView. 
 */

public class ProcessMonitor extends Thread {
  private Process                    m_process;
  private ProcessTerminationListener m_terminationListener;
  
  public ProcessMonitor(Process process,
                        ProcessTerminationListener terminationListener)
  {
    super();

    m_process             = process;
    m_terminationListener = terminationListener;
    
    start();
  }

  public void run() {
    while(true) {
      try {
        m_process.waitFor();
        m_terminationListener.processTerminated(m_process.exitValue());
        return;
      } catch(InterruptedException ie) {/**/}
    }
  }
}
