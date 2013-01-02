/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.transactions;

/**
 * A {@link TransactionalObject} that doesn't actually check anything.
 */
public class NullTransactionalObject implements TransactionalObject {

  private static class NullContext implements TransactionalObject.Context {
    // Nothing here.
  }

  private static final Context CONTEXT = new NullContext();

  @Override
  public Context startWrite(Object value) {
    return CONTEXT;
  }

  @Override
  public Context startWrite(Object value, long now) {
    return CONTEXT;
  }

  @Override
  public void endWrite(Context rawWrite) {
    // Nothing here.
  }

  @Override
  public void endWrite(Context rawWrite, long now) {
    // Nothing here.
  }

  @Override
  public Context startRead() {
    return CONTEXT;
  }

  @Override
  public Context startRead(long now) {
    return CONTEXT;
  }

  @Override
  public void endRead(Context rawRead, Object result) {
    // Nothing here.
  }

  @Override
  public void endRead(Context rawRead, Object result, long now) {
    // Nothing here.
  }

}
