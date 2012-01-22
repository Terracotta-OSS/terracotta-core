/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.LoaderDescription;

import java.io.Serializable;

public class ClassLoaderInstance implements Serializable {

  private final UTF8ByteDataHolder loaderDef;

  public ClassLoaderInstance(UTF8ByteDataHolder loaderDefinition) {
    loaderDef = loaderDefinition;
  }

  public boolean equals(Object obj) {
    if (obj instanceof ClassLoaderInstance) {
      ClassLoaderInstance other = (ClassLoaderInstance) obj;
      return this.loaderDef.equals(other.loaderDef);
    }
    return false;
  }

  public ClassLoader asClassLoader(ClassProvider classProvider) {
    String classLoaderdef = loaderDef.asString();
    return classProvider.getClassLoader(LoaderDescription.fromString(classLoaderdef));
  }

  public int hashCode() {
    int hash = 17;
    hash = (37 * hash) + loaderDef.hashCode();
    return hash;
  }

  public UTF8ByteDataHolder getLoaderDef() {
    return loaderDef;
  }
}
