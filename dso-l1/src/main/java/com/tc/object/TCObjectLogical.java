/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.abortable.AbortedOperationException;

public class TCObjectLogical extends TCObjectImpl {

  public TCObjectLogical(final ObjectID id, final Object peer, final TCClass tcc, final boolean isNew) {
    super(id, peer, tcc, isNew);
  }

  @Override
  public void logicalInvoke(final int method, final String methodName, final Object[] parameters) {
    getObjectManager().getTransactionManager().logicalInvoke(this, method, methodName, parameters);
  }

  public boolean logicalInvokeWithResult(final int method, final String methodName, final Object[] parameters)
      throws AbortedOperationException {
    return getObjectManager().getTransactionManager().logicalInvokeWithResult(this, method, methodName, parameters);
  }

  @Override
  public void unresolveReference(final String fieldName) {
    throw new AssertionError();
  }
}
