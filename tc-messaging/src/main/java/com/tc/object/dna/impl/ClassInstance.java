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

import com.tc.object.loaders.ClassProvider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ClassInstance implements Serializable {
  private final static Map<String, Class<?>>         PRIMITIVE_TYPE_MAP = new HashMap<String, Class<?>>();

  private final UTF8ByteDataHolder name;

  static {
    PRIMITIVE_TYPE_MAP.put(Integer.TYPE.getName(), Integer.TYPE);
    PRIMITIVE_TYPE_MAP.put(Short.TYPE.getName(), Short.TYPE);
    PRIMITIVE_TYPE_MAP.put(Long.TYPE.getName(), Long.TYPE);
    PRIMITIVE_TYPE_MAP.put(Byte.TYPE.getName(), Byte.TYPE);
    PRIMITIVE_TYPE_MAP.put(Double.TYPE.getName(), Double.TYPE);
    PRIMITIVE_TYPE_MAP.put(Float.TYPE.getName(), Float.TYPE);
    PRIMITIVE_TYPE_MAP.put(Double.TYPE.getName(), Double.TYPE);
    PRIMITIVE_TYPE_MAP.put(Boolean.TYPE.getName(), Boolean.TYPE);
    PRIMITIVE_TYPE_MAP.put(Void.TYPE.getName(), Void.TYPE);
  }

  private static Class<?> getPrimitiveClass(String className) {
    return PRIMITIVE_TYPE_MAP.get(className);
  }

  // Used in tests
  ClassInstance(String className) {
    this(new UTF8ByteDataHolder(className));
  }

  public ClassInstance(UTF8ByteDataHolder className) {
    name = className;
  }

  public Class<?> asClass(ClassProvider classProvider) throws ClassNotFoundException {
    String className = name.asString();
    Class<?> clazz = getPrimitiveClass(className);
    if (clazz != null) { return clazz; }
    return classProvider.getClassFor(className);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClassInstance) {
      ClassInstance other = (ClassInstance) obj;
      return this.name.equals(other.name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash = (37 * hash) + name.hashCode();
    return hash;
  }

  public UTF8ByteDataHolder getName() {
    return name;
  }

  @Override
  public String toString() {
    return "Class(" + name.asString() + ")";
  }

}
