/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.loaders.ClassProvider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ClassInstance implements Serializable {
  private final static Map         PRIMITIVE_TYPE_MAP = new HashMap();

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

  private static Class getPrimitiveClass(String className) {
    return (Class) PRIMITIVE_TYPE_MAP.get(className);
  }

  // Used in tests
  ClassInstance(String className) {
    this(new UTF8ByteDataHolder(className));
  }

  public ClassInstance(UTF8ByteDataHolder className) {
    name = className;
  }

  public Class asClass(ClassProvider classProvider) throws ClassNotFoundException {
    String className = name.asString();
    Class clazz = getPrimitiveClass(className);
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
