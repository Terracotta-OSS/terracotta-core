/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

  public UTF8ByteDataHolder getEnumName() {
    return enumName;
  }

  @Override
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
