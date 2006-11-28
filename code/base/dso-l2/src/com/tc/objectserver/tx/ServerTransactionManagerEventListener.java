/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
