/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.dna.impl;

public class EnumInstance {
  private final ClassInstance classInstance;
  private final Object enumName;
  
  public EnumInstance(ClassInstance classInstance, Object enumName) {
    this.classInstance = classInstance;
    this.enumName = enumName;
  }

  public ClassInstance getClassInstance() {
    return classInstance;
  }

  public Object getEnumName() {
    return enumName;
  }
}
