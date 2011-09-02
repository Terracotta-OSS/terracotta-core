/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

public class GarbageLockException extends Exception {
  public static final GarbageLockException GARBAGE_LOCK_EXCEPTION = new GarbageLockException();
  
  private GarbageLockException() {
    //
  }
}
