/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

public class LockIdFactory {

  public LockIdFactory() {
    //
  }

  public LockID generateLockIdentifier(Object obj) {
    if (obj instanceof LockID) {
      return (LockID) obj;
    }
    
    if (obj instanceof Long) {
      return generateLockIdentifier(((Long) obj).longValue());
    } else if (obj instanceof String) {
      return generateLockIdentifier((String) obj);
    } else {
      throw new AssertionError("unsupported type: " + obj.getClass());
    }
  }
  
  private LockID generateLockIdentifier(long l) {
    return new LongLockID(l);
  }

  private LockID generateLockIdentifier(String str) {
    return new StringLockID(str);
  }
}
