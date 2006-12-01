/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx.optimistic;

import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.change.TCChangeBuffer;
import com.tc.object.change.TCChangeBufferImpl;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class OptimisticTransaction {
  private final Map objectChanges = new HashMap();
  private final Map clones        = new IdentityHashMap();

  public boolean hasClone(Object obj) {
    return clones.containsValue(obj);
  }

  public TCObject getTCObjectFor(Object obj) {
    if (obj.getClass().isArray()) {
      return ManagerUtil.getObject(obj);
    } else {
      return ((Manageable) obj).__tc_managed();
    }
  }

  public void addAll(Map newClones) {
    clones.putAll(newClones);
  }

  public boolean hasChanges() {
    return !objectChanges.isEmpty();
  }

  public Map getChangeBuffers() {
    return this.objectChanges;
  }

  public void objectFieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    getOrCreateChangeBuffer(source).fieldChanged(classname, fieldname, newValue, index);
  }

  public void logicalInvoke(TCObject source, int method, Object[] parameters) {
    getOrCreateChangeBuffer(source).logicalInvoke(method, parameters);
  }

  private TCChangeBuffer getOrCreateChangeBuffer(TCObject object) {

    TCChangeBuffer cb = (TCChangeBuffer) objectChanges.get(object);
    if (cb == null) {
      cb = new TCChangeBufferImpl(object);
      objectChanges.put(object, cb);
    }

    return cb;
  }
}
