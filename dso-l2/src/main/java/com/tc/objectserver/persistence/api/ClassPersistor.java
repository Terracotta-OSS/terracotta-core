/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import java.util.Map;

public interface ClassPersistor {

  public void storeClass(int clazzId, byte clazzBytes[]);

  public byte[] retrieveClass(int clazzId);

  /**
   * @return a map containing all the stored classes (Integer -> byte[])
   */
  public Map retrieveAllClasses();
  
}
