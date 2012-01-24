/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.util.Assert;

/**
 * Specify class for DMI
 */
public class DmiClassSpec {

  private final String className;

  /**
   * Construct new spec
   * 
   * @param className Class name
   */
  public DmiClassSpec(final String className) {
    Assert.pre(className != null);
    this.className = className;
  }

  /**
   * @return Class name
   */
  public String getClassName() {
    return className;
  }

  @Override
  public int hashCode() {
    return className.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DmiClassSpec)) return false;
    DmiClassSpec that = (DmiClassSpec) obj;
    return this.className.equals(that.className);
  }

  @Override
  public String toString() {
    return "DmiClassSpec{className=" + className + "}";
  }

  public static String toString(DmiClassSpec[] specs) {
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    for (DmiClassSpec spec2 : specs) {
      sb.append(spec2.toString()).append(",");
    }
    sb.append("]");
    return sb.toString();
  }
}
