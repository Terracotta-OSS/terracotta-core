/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package java.io;

import com.tc.object.TCObjectExternal;
import com.tc.object.bytecode.ManagerUtil;

public class ObjectStreamClassTC extends ObjectStreamClass {
  @Override
  void getObjFieldValues(Object obj, Object[] vals) {

    TCObjectExternal tco = ManagerUtil.lookupExistingOrNull(obj);
    if (tco != null) {
      synchronized (tco.getResolveLock()) {
        tco.resolveAllReferences();
        super.getObjFieldValues(obj, vals);
      }
    } else {
      super.getObjFieldValues(obj, vals);
    }
  }
}
