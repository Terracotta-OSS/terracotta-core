/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

/**
 * Interface for java.lang.String to manage the interned and compressed string objects
 */
public interface JavaLangStringTC {

  /**
   * Check whether the String is interned
   * 
   * @return true if it is interned string
   */
  public boolean __tc_isInterned();

  /**
   * Call intern and mark the String instance as interned.
   * 
   * @return Interned string
   */
  public String __tc_intern();

  /**
   * Indicates whether TC-instrumented String is internally compressed or not
   * 
   * @return whether String is compressed or not
   */
  public boolean __tc_isCompressed();

  /**
   * Force String to decompress if it was compressed
   * 
   * @return true if String was compressed
   */
  public void __tc_decompress();
}
