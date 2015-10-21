/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

  @Override
  public String getIdentifierType() {
    return "ThreadID";
  }

  @Override
  public String toString() {
    if (name == null) {
      return super.toString();
    } else {
      return "Thread(" + name + ") ID[" + toLong() + "]";
    }
  }

}
