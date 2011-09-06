/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcsimulator;

import com.tc.net.proxy.TCPProxy;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;

import java.util.HashMap;
import java.util.Map;

public class SimpleApplicationConfig implements ApplicationConfig {

  private final Map attributes = new HashMap();

  public SimpleApplicationConfig() {
    // XXX: This is pretty retarded.
    setAttribute("sleepInterval", "0");
    setAttribute("throwException", Boolean.FALSE.toString());
  }

  public String getApplicationClassname() {
    return SimpleApplication.class.getName();
  }

  public void setAttribute(String key, String value) {
    attributes.put(key, value);
  }

  public String getAttribute(String key) {
    return (String) attributes.get(key);
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
    throw new AssertionError();
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
