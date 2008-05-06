/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

/**
 * Event fires when trying to share a non-portable object.
 */
public class NonPortableObjectEvent extends AbstractApplicationEvent {

  private static final long serialVersionUID = 1323477247234324L;
  private NonPortableReason nonPortableReason;

  public NonPortableObjectEvent(NonPortableEventContext context, NonPortableReason nonPortableReason) {
    super(context);
    this.nonPortableReason = nonPortableReason;
  }

  /**
   * @return Event-specific context
   */
  public NonPortableEventContext getNonPortableEventContext() {
    return (NonPortableEventContext) getApplicationEventContext();
  }

  /**
   * @return Get reason why object is non-portable
   */
  public NonPortableReason getNonPortableEventReason() {
    return nonPortableReason;
  }

  public String getMessage() {
    return getNonPortableEventReason().getMessage();
  }
}
