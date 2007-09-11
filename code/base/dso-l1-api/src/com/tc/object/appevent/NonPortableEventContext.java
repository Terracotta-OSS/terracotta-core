/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

public class NonPortableEventContext extends AbstractApplicationEventContext {

  private static final long      serialVersionUID = 4788562594133534828L;

  public NonPortableEventContext(Object pojo, String threadName, String clientId) {
    super(pojo, threadName, clientId);
  }

  public void addDetailsTo(NonPortableReason reason) {
    reason.addDetail("Thread", getThreadName());
    reason.addDetail("JVM ID", getClientId());
  }
}
