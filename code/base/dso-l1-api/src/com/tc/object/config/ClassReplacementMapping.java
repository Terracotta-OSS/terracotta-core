/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import java.net.URL;

/**
 * Manages a set of classes that are being swapped.
 */
public interface ClassReplacementMapping {
  
  /**
   * Add a new mapping
   * @param originalClassName Original class
   * @param replacementClassName Replacement class
   * @param replacementResource Class bits to use for replacement
   * @return Previous mapping, if any
   */
  public String addMapping(final String originalClassName, final String replacementClassName, final URL replacementResource);
  
  /**
   * Get class bytes URL
   * @param replacementClassName Replacement class name
   * @return Location of bytes
   */
  public URL getReplacementResource(final String replacementClassName);
  
  /**
   * Get class bytes URL
   * @param replacementClassName Replacement class name
   * @param defaultClassLoader Classloader to use when loading URL resource 
   * @return Location of bytes
   */
  public URL getReplacementResource(final String replacementClassName, final ClassLoader defaultClassLoader);
  
  /**
   * Check if has replacement
   * @param originalClassName Class 
   * @return True if has replacement in mapping
   */
  public boolean hasReplacement(String originalClassName);

  /**
   * Get replacement mapping
   * @param original Original class name
   * @return Replacement class name
   */
  public String getReplacementClassName(String original);
  
  /**
   * Get original class name with slashes instead of dots.
   * @param replacement Replacement class name with slashes
   * @return Original class name with slashes instead of dots.
   */
  public String getOriginalClassNameSlashes(String replacement);
  
  /**
   * Get original bytecode class name (slashes, start with L, etc).
   * @param replacement Replacement class name in bytecode form
   * @return Original class name in bytecode form
   */
  public String getOriginalAsmType(String replacement);

  /**
   * In string s, convert all replacement ASM class names from the 
   * replacement names back to the original.
   * @param s The string
   * @return s, with replacement names converted to original 
   */
  public String ensureOriginalAsmTypes(String s);
}
