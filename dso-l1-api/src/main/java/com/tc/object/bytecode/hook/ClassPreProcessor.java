/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.hook;

/**
 * Modify byte[] before being loaded as a class by the classloader
 */
public interface ClassPreProcessor {

  /**
   * 
   * XXX::NOTE:: ClassLoader checks the returned byte array to see if the class is instrumented or not to maintain the
   * offset.
   * @param name The class name
   * @param data Data buffer
   * @param offset Offset into data
   * @param length Length of class data after offset
   * @param caller The classloader loading the class
   * @return new byte array if the class is instrumented and same input byte array if not.
   * @see ClassLoaderPreProcessorImpl
   */
  public byte[] preProcess(String name, byte[] data, int offset, int length, ClassLoader caller);

}
