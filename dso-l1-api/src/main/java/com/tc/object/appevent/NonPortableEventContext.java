/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

/**
 * Event context for a {@link NonPortableObjectEvent}
 */
public class NonPortableEventContext extends AbstractApplicationEventContext {

  private static final long      serialVersionUID = 4788562594133534828L;

  public NonPortableEventContext(Object pojo, String threadName, String clientId) {
    super(pojo, threadName, clientId);
  }

  /**
   * Enhance the reason with some additional details specific to this context
   * @param reason The reason, which will be modified
   */
  public void addDetailsTo(NonPortableReason reason) {
    reason.addDetail("Thread", getThreadName());
    reason.addDetail("JVM ID", getClientId());
  }
}
