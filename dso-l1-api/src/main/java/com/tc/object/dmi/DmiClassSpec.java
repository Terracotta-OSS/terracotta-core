/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.util.Assert;

/**
 * Specify class for DMI
 */
public class DmiClassSpec {

  private final String classLoaderDesc;
  private final String className;
  private final String spec;

  /**
   * Construct new spec
   * @param classLoaderDesc Classloader name
   * @param className Class name
   */
  public DmiClassSpec(final String classLoaderDesc, final String className) {
    Assert.pre(classLoaderDesc != null);
    Assert.pre(className != null);
    this.classLoaderDesc = classLoaderDesc;
    this.className = className;
    this.spec = classLoaderDesc + "-" + className;
  }

  /**
   * @return Classloader name
   */
  public String getClassLoaderDesc() {
    return classLoaderDesc;
  }

  /**
   * @return Class name
   */
  public String getClassName() {
    return className;
  }

  public int hashCode() {
    return spec.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof DmiClassSpec)) return false;
    DmiClassSpec that = (DmiClassSpec) obj;
    return this.spec.equals(that.spec);
  }
  
  public String toString() {
    return "DmiClassSpec{loader=" + classLoaderDesc + ", className=" + className + "}";
  }
  
  public static String toString(DmiClassSpec[] specs) {
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    for (int i = 0; i < specs.length; i++) {
      sb.append(specs[i].toString()).append(",");
    }
    sb.append("]");
    return sb.toString();
  }
}
