/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.object.bytecode.Clearable;
import com.tc.util.Assert;

public class TCObjectLogical extends TCObjectImpl {

  public TCObjectLogical(final ObjectID id, final Object peer, final TCClass tcc, final boolean isNew) {
    super(id, peer, tcc, isNew);
  }

  public void logicalInvoke(final int method, final String methodName, final Object[] parameters) {
    getObjectManager().getTransactionManager().logicalInvoke(this, method, methodName, parameters);
  }

  @Override
  protected boolean isEvictable() {
    Object peer;
    if ((peer = getPeerObject()) instanceof Clearable) {
      return ((Clearable) peer).isEvictionEnabled();
    } else {
      return false;
    }
  }

  @Override
  protected int clearReferences(final Object pojo, final int toClear) {
    if (!(pojo instanceof Clearable)) {
      Assert.fail("TCObjectLogical.clearReferences expected Clearable but got "
                  + (pojo == null ? "null" : pojo.getClass().getName()));
    }
    final Clearable clearable = (Clearable) pojo;
    return clearable.__tc_clearReferences(toClear);
  }

  public void unresolveReference(final String fieldName) {
    throw new AssertionError();
  }
}
