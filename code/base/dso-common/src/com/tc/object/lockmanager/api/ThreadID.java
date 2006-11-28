/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.util.AbstractIdentifier;

public class ThreadID extends AbstractIdentifier {

  public static final ThreadID NULL_ID = new ThreadID();
  public static final ThreadID VM_ID   = new ThreadID(Long.MIN_VALUE);
  
  public ThreadID(long id) {
    super(id);
  }

  private ThreadID() {
    super();
  }

  public String getIdentifierType() {
    return "ThreadID";
  }

}
