/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

public class ProcessStatus {
  private String  m_processName;
  private int     m_status;
  private boolean m_restarting;
  
  public static final int READY      = 1;
  public static final int WAITING    = 2;
  public static final int FAILED     = 3;
  public static final int INACTIVE   = 4;
  
  public ProcessStatus(String processName) {
    m_processName = processName;
    setStatus(INACTIVE);
  }
  
  public String getProcessName() {
    return m_processName;
  }
  
  public int getStatus() {
    return m_status;
  }
  
  public void setStatus(int status) {
    m_status = status;
  }
  
  public boolean isRestarting() {
    return m_restarting;
  }
  
  public void setRestarting(boolean restarting) {
    m_restarting = restarting;
  }
  
  public void setReady() {
    setStatus(READY);
    setRestarting(false);
  }

  public boolean isReady() {
    return getStatus() == READY;
  }
  
  public void setWaiting() {
    setStatus(WAITING);
  }
  
  public boolean isWaiting() {
    return getStatus() == WAITING;
  }
  
  public void setFailed() {
    setStatus(FAILED);
    setRestarting(false);
  }
  
  public boolean isFailed() {
    return getStatus() == FAILED;
  }
  
  public void setInactive() {
    setStatus(INACTIVE);
  }
  
  public boolean isInactive() {
    return getStatus() == INACTIVE;
  }
  
  public String getStatusString() {
    String s;
    
    switch(getStatus()) {
      case READY:    {s = "ready";          break;}
      case WAITING:  {s = "waiting";        break;}
      case FAILED:   {s = "failed";         break;}
      case INACTIVE: {s = "inactive";       break;}
      default:       {s = "invalid status"; break;}
    }
    
    if(isRestarting()) {
      s += ", restarting";
    }
    
    return s;
  }
  
  public String toString() {
    return getProcessName() + " is " + getStatusString();
  }
}
