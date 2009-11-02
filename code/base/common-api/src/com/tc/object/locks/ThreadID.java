/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.util.AbstractIdentifier;

/**
 * Thread identifier
 */
public class ThreadID extends AbstractIdentifier {

  /** Null identifier */
  public static final ThreadID   NULL_ID = new ThreadID();
  /** VM identifier */
  public static final ThreadID   VM_ID   = new ThreadID(Long.MIN_VALUE);
  private transient final String name;

  /**
   * New thread id
   * 
   * @param id Identifier
   */
  public ThreadID(long id) {
    this(id, null);
  }

  public ThreadID(long id, String name) {
    super(id);
    this.name = name;
  }

  private ThreadID() {
    super();
    this.name = "thread_NULL_ID";
  }

  public String getIdentifierType() {
    return "ThreadID";
  }

  public String toString() {
    if (name == null) {
      return super.toString();
    } else {
      return "Thread(" + name + ") ID[" + toLong() + "]";
    }
  }

}
