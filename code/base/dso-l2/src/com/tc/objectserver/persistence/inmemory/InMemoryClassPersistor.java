/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.inmemory;

import com.tc.exception.TCRuntimeException;
import com.tc.objectserver.persistence.api.ClassPersistor;

import java.util.HashMap;
import java.util.Map;

public class InMemoryClassPersistor implements ClassPersistor {

  Map clazzes = new HashMap();

  public InMemoryClassPersistor() {
    super();
  }

  public void storeClass(int clazzId, byte[] clazzBytes) {
    clazzes.put(new Integer(clazzId), clazzBytes);
  }

  public byte[] retrieveClass(int clazzId) {
    byte[] clazzbytes = (byte[]) clazzes.get(new Integer(clazzId));
    if (clazzbytes == null) { throw new TCRuntimeException("Class bytes not found : " + clazzId); }
    return clazzbytes;
  }

  public Map retrieveAllClasses() {
    return new HashMap(clazzes);
  }

}
