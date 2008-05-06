/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.object.ObjectID;

public interface ServerTransactionManagerEventListener {
  /**
   * Called when a new root is created
   * 
   * @param name the root name
   * @param id the ObjectID of the new root
   */
  public void rootCreated(String name, ObjectID id);

}
