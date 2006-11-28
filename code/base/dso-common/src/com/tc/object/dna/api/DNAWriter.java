/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.dna.api;

import com.tc.object.ObjectID;


/**
 * Interface for writing DNA
 */
public interface DNAWriter {

  void addLogicalAction(int method, Object[] parameters);

  void addPhysicalAction(String fieldName, Object value);
  
  void addPhysicalAction(String fieldName, Object value, boolean canBeReferenced);
  
  void addArrayElementAction(int index, Object value);
  
  void addSubArrayAction(int start, Object array);
  
  void addClassLoaderAction(String classLoaderFieldName, Object value);

  void addEntireArray(Object value);
  
  void addLiteralValue(Object value);

  void finalizeDNA();

  void setParentObjectID(ObjectID id);

  void setArrayLength(int length);

}
