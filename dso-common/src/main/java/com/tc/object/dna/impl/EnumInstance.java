/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((classInstance == null) ? 0 : classInstance.hashCode());
    result = prime * result + ((enumName == null) ? 0 : enumName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof EnumInstance)) return false;
    EnumInstance other = (EnumInstance) obj;
    if (classInstance == null) {
      if (other.classInstance != null) return false;
    } else if (!classInstance.equals(other.classInstance)) return false;
    if (enumName == null) {
      if (other.enumName != null) return false;
    } else if (!enumName.equals(other.enumName)) return false;
    return true;
  }
}
