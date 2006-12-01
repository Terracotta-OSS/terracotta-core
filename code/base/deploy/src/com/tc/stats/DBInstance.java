/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

public class DBInstance implements DBInstanceMBean {
  private String  description;
  private boolean isActive;
  
  public DBInstance(String description, boolean isActive) {
    this.description = description;
    this.isActive    = isActive;
  }
  
  public String getDescription() {
    return description;
  }

  public void setActive(boolean isActive) {
    this.isActive = isActive;
  }
  
  public boolean isActive() {
    return isActive;
  }
  
  public void refresh() {
    // nothing yet
  }
}
