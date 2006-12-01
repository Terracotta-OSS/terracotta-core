/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
