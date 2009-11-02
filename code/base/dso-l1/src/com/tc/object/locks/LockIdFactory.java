/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.TCObject;
import com.tc.object.bytecode.Manager;

public class LockIdFactory {

  private final Manager mgr;
  
  public LockIdFactory(Manager mgr) {
    this.mgr = mgr;
  }
  
  public LockID generateLockIdentifier(Object obj) {
    if (obj instanceof String) {
      return generateLockIdentifier((String) obj);
    } else if (mgr.isLiteralAutolock(obj)) {
      return new DsoLiteralLockID(mgr, obj);
    } else {
      TCObject tco = mgr.lookupExistingOrNull(obj);
      if ((tco == null) || tco.autoLockingDisabled()) {
        return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
      } else {
        return new DsoLockID(tco.getObjectID());
      }
    }
  }
  
  public LockID generateLockIdentifier(Object obj, String fieldName) {
    TCObject tco;
    if (obj instanceof TCObject) {
      tco = (TCObject) obj;
    } else {
      tco = mgr.lookupExistingOrNull(obj);
    }
    
    if ((tco == null) || tco.autoLockingDisabled()) {
      return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
    } else {
      return new DsoVolatileLockID(tco.getObjectID(), fieldName);
    }
  }
  
  public LockID generateLockIdentifier(String str) {
    return new StringLockID(str);
  }  
}
