/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.loaders.ClassProvider;

import java.io.Serializable;

public class ClassInstance implements Serializable {

  private final UTF8ByteDataHolder name;
  private final UTF8ByteDataHolder loaderDef;

  // Used in tests
  ClassInstance(String className, String loaderDefinition) {
    this(new UTF8ByteDataHolder(className), new UTF8ByteDataHolder(loaderDefinition));
  }
  
  public ClassInstance(UTF8ByteDataHolder className, UTF8ByteDataHolder loaderDefinition) {
    name = className;
    loaderDef = loaderDefinition;
  }

  public Class asClass(ClassProvider classProvider) throws ClassNotFoundException {
    String classLoaderdef = loaderDef.asString();
    String className = name.asString();
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

}
