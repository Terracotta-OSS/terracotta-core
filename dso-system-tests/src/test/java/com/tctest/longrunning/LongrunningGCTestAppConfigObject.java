/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.longrunning;

import com.tc.net.proxy.TCPProxy;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;

public class LongrunningGCTestAppConfigObject implements LongrunningGCTestAppConfig {

  private long     loopSleepTime      = 250;
  private String[] applicationClasses = new String[0];

  public void setLoopSleepTime(long time) {
    this.loopSleepTime = time;
  }

  public long getLoopSleepTime() {
    return loopSleepTime;
  }

  public String getApplicationClassname() {
    return LongrunningGCTestApp.class.getName();
  }

  public String[] getApplicationClasses() {
    return this.applicationClasses;
  }

  public void setApplicationClasses(String[] classes) {
    this.applicationClasses = classes;
  }

  public void setAttribute(String key, String value) {
    //
  }

  public String getAttribute(String key) {
    return null;
  }

  public int getIntensity() {
    throw new AssertionError();
  }

  public int getGlobalParticipantCount() {
    throw new AssertionError();
  }

  public ApplicationConfig copy() {
    throw new AssertionError();
  }

  public ServerControl getServerControl() {
    throw new UnsupportedOperationException("not implemented, should not be used");
  }

  public int getValidatorCount() {
    throw new AssertionError();
  }

  public int getGlobalValidatorCount() {
    throw new AssertionError();
  }

  public TCPProxy[] getProxies() {
    throw new AssertionError();
  }

  public ServerControl[] getServerControls() {
    throw new AssertionError();
  }

  public Object getAttributeObject(String key) {
    throw new AssertionError();
  }
}
