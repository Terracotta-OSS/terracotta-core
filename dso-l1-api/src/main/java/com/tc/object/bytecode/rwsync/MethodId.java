/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.object.bytecode.rwsync;

/**
 * Uniquely identify a method by name and signature. Immutable and honors equality contract; can be used as a key.
 */
public final class MethodId {
  private final String name;
  private final String desc;

  /**
   * @param name method name, e.g., "setFoo". Must be non-null.
   * @param desc argument descriptor, e.g., "(Ljava/lang/Object;)V". Must be non-null.
   */
  public MethodId(String name, String desc) {
    if (name == null || desc == null) { throw new IllegalArgumentException("Null argument to MethodId()"); }
    this.name = name;
    this.desc = desc;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + desc.hashCode();
    result = prime * result + name.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof MethodId)) return false;
    MethodId other = (MethodId) obj;
    if (!name.equals(other.name)) return false;
    if (!desc.equals(other.desc)) return false;
    return true;
  }

  /**
   * Return a string like "setFoo(Ljava/lang/Object;)V". Because <code>name</code> cannot contain a '(', and
   * <code>desc</code> always starts with a '(', this string representation is as unique as the MethodId itself is, and
   * can be used as a key in collections that require a String key.
   */
  @Override
  public String toString() {
    return name + desc;
  }
}
