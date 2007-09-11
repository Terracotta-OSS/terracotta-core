/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

public class NonPortableObjectEvent extends AbstractApplicationEvent {

  private static final long serialVersionUID = 1323477247234324L;
  private NonPortableReason nonPortableReason;

  public NonPortableObjectEvent(NonPortableEventContext context, NonPortableReason nonPortableReason) {
    super(context);
    this.nonPortableReason = nonPortableReason;
  }

  public NonPortableEventContext getNonPortableEventContext() {
    return (NonPortableEventContext) getApplicationEventContext();
  }

  public NonPortableReason getNonPortableEventReason() {
    return nonPortableReason;
  }

  public String getMessage() {
    return getNonPortableEventReason().getMessage();
  }
}
