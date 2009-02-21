/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.LoaderDescription;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ClassInstance implements Serializable {
  private final static Map PRIMITIVE_TYPE_MAP = new HashMap();

  private final UTF8ByteDataHolder name;
  private final UTF8ByteDataHolder loaderDef;
  
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
    return (Class)PRIMITIVE_TYPE_MAP.get(className);
  }

  // Used in tests
  ClassInstance(String className, String loaderDefinition) {
    this(new UTF8ByteDataHolder(className), new UTF8ByteDataHolder(loaderDefinition));
  }

  public ClassInstance(UTF8ByteDataHolder className, UTF8ByteDataHolder loaderDefinition) {
    name = className;
    loaderDef = loaderDefinition;
  }

  public Class asClass(ClassProvider classProvider) throws ClassNotFoundException {
    LoaderDescription classLoaderdef = LoaderDescription.fromString(loaderDef.asString());
    String className = name.asString();
    Class clazz = getPrimitiveClass(className);
    if (clazz != null) { return clazz; }
    return classProvider.getClassFor(className, classLoaderdef);
  }

  public boolean equals(Object obj) {
    if (obj instanceof ClassInstance) {
      ClassInstance other = (ClassInstance) obj;
      return this.name.equals(other.name) && this.loaderDef.equals(other.loaderDef);
    }
    return false;
  }

  public int hashCode() {
    int hash = 17;
    hash = (37 * hash) + name.hashCode();
    hash = (37 * hash) + loaderDef.hashCode();
    return hash;
  }

  public UTF8ByteDataHolder getLoaderDef() {
    return loaderDef;
  }

  public UTF8ByteDataHolder getName() {
    return name;
  }

  public String toString() {
    return "Class(" + name.asString() + "," + loaderDef.asString() + ")";
  }

}
