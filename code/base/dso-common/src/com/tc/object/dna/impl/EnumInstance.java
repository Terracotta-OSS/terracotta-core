/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import java.io.Serializable;

public class EnumInstance implements Serializable {
  private final ClassInstance      classInstance;
  private final UTF8ByteDataHolder enumName;

  public EnumInstance(ClassInstance classInstance, UTF8ByteDataHolder enumName) {
    this.classInstance = classInstance;
    this.enumName = enumName;
  }

  public ClassInstance getClassInstance() {
    return classInstance;
  }

  public Object getEnumName() {
    return enumName;
  }

  public String toString() {
    return enumName.asString();
  }
}
