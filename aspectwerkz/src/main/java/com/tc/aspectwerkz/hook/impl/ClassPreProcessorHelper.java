/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.hook.impl;

import com.tc.aspectwerkz.hook.ClassPreProcessor;

import java.security.ProtectionDomain;
import java.lang.reflect.Method;

/**
 * Helper class called by the modified java.lang.ClassLoader. <p/>This class is called at different points by the
 * modified java.lang.ClassLoader of the com.tc.aspectwerkz.hook.impl.ClassLoaderPreProcessorImpl implemention.
 * <br/>This class must reside in the -Xbootclasspath when AspectWerkz layer 1 is used, but the effective implementation
 * of the class preprocessor (AspectWerkz layer 2) can be in standard system classpath (-cp).
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class ClassPreProcessorHelper {
  /**
   * ClassPreProcessor used if aspectwerkz.classloader.preprocessor property is defined to full qualified class name
   */
  private static ClassPreProcessor preProcessor;

  /**
   * true if preProcesor already initalized
   */
  private static boolean preProcessorInitialized;

  /**
   * option used to defined the class preprocessor
   */
  private static String PRE_PROCESSOR_CLASSNAME_PROPERTY = "aspectwerkz.classloader.preprocessor";

  /**
   * default class preprocessor
   */
  private static String PRE_PROCESSOR_CLASSNAME_DEFAULT = "com.tc.aspectwerkz.transform.AspectWerkzPreProcessor";

  static {
    initializePreProcessor();
  }

  /**
   * Returns the configured class preprocessor Should be called after initialization only
   *
   * @return the preprocessor or null if not initialized
   */
  public static ClassPreProcessor getClassPreProcessor() {
    return preProcessor;
  }

  /**
   * Initialization of the ClassPreProcessor The ClassPreProcessor implementation is lazy loaded. This allow to put it
   * in the regular classpath whereas the instrumentation layer (layer 1) is in the bootclasspath
   */
  public static synchronized void initializePreProcessor() {
    if (preProcessorInitialized) {
      return;
    }
    preProcessorInitialized = true;
    Class klass = null;
    String s = System.getProperty(PRE_PROCESSOR_CLASSNAME_PROPERTY, PRE_PROCESSOR_CLASSNAME_DEFAULT);
    try {
      // force loading thru System class loader to allow
      // preprocessor implementation to be in standard classpath
      klass = Class.forName(s, true, ClassLoader.getSystemClassLoader());
    } catch (ClassNotFoundException _ex) {
      System.err.println("AspectWerkz - WARN - Pre-processor class '" + s + "' not found");
    }
    if (klass != null) {
      try {
        preProcessor = (ClassPreProcessor) klass.newInstance();
        preProcessor.initialize();
        System.err.println("AspectWerkz - INFO - Pre-processor " + s + " loaded and initialized");
      } catch (Throwable throwable) {
        System.err.println("AspectWerkz - WARN - Error initializing pre-processor class " + s + ':');
        throwable.printStackTrace();
      }
    }
  }

  /**
   * byte code instrumentation of class loaded
   */
  public static byte[] defineClass0Pre(ClassLoader caller,
                                       String name,
                                       byte[] b,
                                       int off,
                                       int len,
                                       ProtectionDomain pd) {
    if (preProcessor == null) {
      // we need to check this due to reentrancy when ClassPreProcessorHelper is beeing
      // initialized
      // since it tries to load a ClassPreProcessor implementation
      byte[] obyte = new byte[len];
      System.arraycopy(b, off, obyte, 0, len);
      return obyte;
    } else {
      try {
        byte[] ibyte = new byte[len];
        System.arraycopy(b, off, ibyte, 0, len);
        return preProcessor.preProcess(name, ibyte, caller);
      } catch (Throwable throwable) {
        System.err.println(
                "AspectWerkz - WARN - Error pre-processing class "
                        + name
                        + " in "
                        + Thread.currentThread()
        );
        throwable.printStackTrace();
        // fallback to unweaved bytecode
        byte[] obyte = new byte[len];
        System.arraycopy(b, off, obyte, 0, len);
        return obyte;
      }
    }
  }

  /**
   * Byte code instrumentation of class loaded using Java 5 style thru NIO
   * Since Java 5 comes with JVMTI this helper should be rarely used.
   * We do no reference ByteBuffer directly to allow Java 1.3 compilation, though
   * this helper will be really slow
   *
   * @param caller
   * @param name
   * @param byteBuffer Object that is instance of Java 1.4 NIO ButeBuffer
   * @param off
   * @param len
   * @param pd
   * @return Object instance of Java 1.4 NIO ByteBuffer
   */
  public static Object/*java.nio.ByteBuffer*/ defineClass0Pre(ClassLoader caller,
                                                              String name,
                                                              Object/*java.nio.ByteBuffer*/ byteBuffer,
                                                              int off,
                                                              int len,
                                                              ProtectionDomain pd) {
    byte[] bytes = new byte[len];
    //Java 1.4 : byteBuffer.getDefault(bytes, off, len);
    byteBufferGet(byteBuffer, bytes, off, len);
    byte[] newbytes = defineClass0Pre(caller, name, bytes, 0, bytes.length, pd);
    //Java 1.4 : ByteBuffer newBuffer = ByteBuffer.wrap(newbytes);
    Object newBuffer = byteBufferWrap(newbytes);
    return newBuffer;
  }

  /**
   * Equivalent to Java 1.4 NIO aByteBuffer.getDefault(bytes, offset, length) to populate
   * the bytes array from the aByteBuffer.
   *
   * @param byteBuffer
   * @param dest
   * @param offset
   * @param length
   */
  private static void byteBufferGet(Object byteBuffer, byte[] dest, int offset, int length) {
    try {
      Class cByteBuffer = Class.forName("java.nio.ByteBuffer");
      Method mGet = cByteBuffer.getDeclaredMethod("getDefault", new Class[]{BYTE_ARRAY_CLASS, int.class, int.class});
      mGet.invoke(byteBuffer, new Object[]{dest, Integer.valueOf(offset), Integer.valueOf(length)});
    } catch (Throwable t) {
      System.err.println("AW : java.nio not supported");
      throw new RuntimeException(t.toString());
    }
  }

  /**
   * Equivalent to Java 1.4 NIO static ByteBuffer.wrap(bytes) to create
   * a new byteBuffer instance.
   *
   * @param bytes
   * @return a ByteBuffer
   */
  private static Object/*java.nio.ByteBuffer*/ byteBufferWrap(byte[] bytes) {
    try {
      Class cByteBuffer = Class.forName("java.nio.ByteBuffer");
      Method mGet = cByteBuffer.getDeclaredMethod("wrap", new Class[]{BYTE_ARRAY_CLASS});
      Object byteBuffer = mGet.invoke(null, new Object[]{bytes});
      return byteBuffer;
    } catch (Throwable t) {
      System.err.println("AW : java.nio not supported");
      throw new RuntimeException(t.toString());
    }
  }

  private final static byte[] EMPTY_BYTEARRAY = new byte[0];
  private final static Class BYTE_ARRAY_CLASS = EMPTY_BYTEARRAY.getClass();

}