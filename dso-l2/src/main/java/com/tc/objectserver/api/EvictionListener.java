/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;

/**
 *
 * @author mscott
 */
public interface EvictionListener {
  /**
   * 
   * @param oid
   * @return true detaches the listener 
   */
  boolean evictionStarted(ObjectID oid);
  /**
   * 
   * @param oid
   * @return true detaches the listener 
   */
  boolean evictionCompleted(ObjectID oid);
}
