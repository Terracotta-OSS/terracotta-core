/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
