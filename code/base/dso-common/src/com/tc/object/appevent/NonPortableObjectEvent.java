/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

import java.io.Serializable;

public class NonPortableObjectEvent implements Serializable {

  private static final long             serialVersionUID = 1323477247234324L;

  private final NonPortableEventContext context;
  private final NonPortableReason       reason;

  public NonPortableObjectEvent(NonPortableEventContext context, NonPortableReason reason) {
    this.context = context;
    this.reason = reason;
  }

  public NonPortableEventContext getContext() {
    return context;
  }

  public NonPortableReason getReason() {
    return reason;
  }

}
