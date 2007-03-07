/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx.optimistic;

import java.util.Map;

public interface OptimisticTransactionManager {

  public void begin();

  public void objectFieldChanged(TCObjectClone clone, String classname, String fieldname, Object newValue, int index);

  public void logicalInvoke(TCObjectClone clone, int method, String methodName, Object[] parameters);

  public void commit() throws ClassNotFoundException;

  public void rollback();

  public void addClonesToTransaction(Map cloned);

  public Object convertToParameter(Object clone);

}
