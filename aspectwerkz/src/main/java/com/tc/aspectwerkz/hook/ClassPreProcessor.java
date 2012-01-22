/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.hook;

/**
 * Implement to be a class PreProcessor in the AspectWerkz univeral loading architecture. <p/>A single instance of the
 * class implementing this interface is build during the java.lang.ClassLoader initialization or just before the first
 * class loads, bootclasspath excepted. Thus there is a single instance the of ClassPreProcessor per JVM. <br/>Use the
 * <code>-Daspectwerkz.classloader.preprocessor</code> option to specify which class preprocessor to use.
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 * @see com.tc.aspectwerkz.hook.ProcessStarter
 */
public interface ClassPreProcessor {

  public abstract void initialize();

  public abstract byte[] preProcess(String klass, byte[] abyte, ClassLoader caller);
}
