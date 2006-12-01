/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

import java.io.Serializable;

public class NonPortableEventContext implements Serializable {

  private static final long         serialVersionUID = 4788562594133534828L;

  private final String              threadName;
  private final String              clientId;
  private final String              targetClassName;

  public NonPortableEventContext(String targetClassName, String threadName, String clientId) {
    this.targetClassName = targetClassName;
    this.threadName = threadName;
    this.clientId = clientId;
  }

  public String getTargetClassName() {
    return targetClassName;
  }

  public String getThreadName() {
    return threadName;
  }

  public String getClientId() {
    return clientId;
  }

  public void addDetailsTo(NonPortableReason reason) {
    reason.addDetail("Thread", threadName);
    reason.addDetail("JVM ID", clientId);
  }


}
