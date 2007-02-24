/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.app;

import com.tc.objectserver.control.ServerControl;


public interface ApplicationConfig {
  
  public String getApplicationClassname();

  public void setAttribute(String key, String value);

  public String getAttribute(String key);
  
  public int getGlobalParticipantCount();
  
  public int getIntensity();
  
  public ServerControl getServerControl();
  
  public ApplicationConfig copy();
}
