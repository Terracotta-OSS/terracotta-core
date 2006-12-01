/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.app;

public interface ApplicationConfig {
  
  public String getApplicationClassname();

  public void setAttribute(String key, String value);

  public String getAttribute(String key);
  
  public int getGlobalParticipantCount();
  
  public int getIntensity();

  public ApplicationConfig copy();
}
