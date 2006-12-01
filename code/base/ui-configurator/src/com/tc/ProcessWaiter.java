/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import com.tc.admin.common.InputStreamDrainer;

public class ProcessWaiter extends Thread {
  private Process            m_process;
  private Runnable           m_waiter;
  private InputStreamDrainer m_outReader;
  private InputStreamDrainer m_errReader;

  public ProcessWaiter(Process process, Runnable waiter) {
    super();
    
    if(process == null)
      throw new IllegalArgumentException("process is null");
    
    m_process = process;
    m_waiter  = waiter;
  }

  public ProcessWaiter(Process process) {
    this(process, null);
  }
  
  public void start(Runnable waiter) {
    m_waiter = waiter;
    start();
  }
  
  public void run() {
    m_outReader = new InputStreamDrainer(m_process.getInputStream());
    m_errReader = new InputStreamDrainer(m_process.getErrorStream());
    
    m_outReader.start();
    m_errReader.start();
    
    while(true) {
      try {
        if(m_outReader.isAlive()) {
          m_outReader.join();
        }
        if(m_errReader.isAlive()) {
          m_errReader.join();
        }
        break;
      } catch(InterruptedException ie) {/**/}
      
      try {sleep(1000);} catch(InterruptedException ie) {/**/}
    }
   
    if(m_waiter != null) {
      m_waiter.run();
    }
  }
  
  public Process getProcess() {
    return m_process;
    
  }
  
  public String getOutputBuffer() {
    return m_outReader.getBufferContent();
  }

  public String getErrorBuffer() {
    return m_errReader != null ? m_errReader.getBufferContent() : null;
  }
}
