/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx.optimistic;

import com.tc.object.TCObject;

import java.util.Map;

/** 
 * Transaction manager, just one in a client VM.  Manages transactions per thread.  Nested transactions
 * are not supported.
 */
public interface OptimisticTransactionManager {

  /**
   * Begin a transaction for the current thread.
   */
  public void begin();

  /**
   * Indicate an object field change in the transaction
   * @param clone The object wrapper
   * @param className Class
   * @param fieldName Field
   * @param newValue New field value
   * @param index Index if newValue is an array
   */
  public void objectFieldChanged(TCObject clone, String classname, String fieldname, Object newValue, int index);

  /**
   * Indicate a logical invocation in the transaction
   * @param clone The object wrapper
   * @param method The method, as defined in {@link com.tc.object.SerializationUtil}
   * @param methodName Method
   * @param parameters Parameter values
   */
  public void logicalInvoke(TCObject clone, int method, String methodName, Object[] parameters);

  /**
   * Commit the transaction for the current thread.
   * @throws ClassNotFoundException If class not found while faulting in object
   */
  public void commit() throws ClassNotFoundException;

  /**
   * Rollback the transaction in the current thread.
   */
  public void rollback();

  /**
   * Add a map of original->cloned objects to the transactions
   * @param cloned, original->clone
   */
  public void addClonesToTransaction(Map cloned);

  /**
   * Convert to an ObjectID or literal value
   * @param clone The object
   * @return ObjectID or literal value for this object
   */
  public Object convertToParameter(Object clone);

}
