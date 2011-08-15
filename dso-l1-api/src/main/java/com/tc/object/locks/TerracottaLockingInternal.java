/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

public interface TerracottaLockingInternal extends TerracottaLocking {

  public LockID generateLockIdentifier(long l);

}
