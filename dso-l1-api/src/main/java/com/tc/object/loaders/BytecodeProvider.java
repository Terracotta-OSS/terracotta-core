/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;

/**
 * To be implemented by all class loaders that can load a class that is only represented by a byte array
 * in memory and can not be found using regular class loading. For example classes that have been generated   
 * on-the-fly.
 */ 
public interface BytecodeProvider {

  /**
   * Returns the bytecode for a class with the name specified. 
   * 
   * @param className the name of the class who's bytecode is missing
   * @return the bytecode for the class or NULL if bytecode is not in the repository
   */
  public byte[] __tc_getBytecodeForClass(String className);
}
