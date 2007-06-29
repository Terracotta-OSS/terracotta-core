/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.simulator.app;

import com.tc.net.proxy.TCPProxy;
import com.tc.objectserver.control.ServerControl;

public interface ApplicationConfig {

  String getApplicationClassname();

  void setAttribute(String key, String value);

  String getAttribute(String key);

  Object getAttributeObject(String key);

  int getGlobalParticipantCount();

  int getIntensity();

  ServerControl getServerControl();

  ApplicationConfig copy();

  int getValidatorCount();

  int getGlobalValidatorCount();

  ServerControl[] getServerControls();

  TCPProxy[] getProxies();
}
