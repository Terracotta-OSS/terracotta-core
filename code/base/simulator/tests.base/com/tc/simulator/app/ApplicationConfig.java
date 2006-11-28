/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
