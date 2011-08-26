/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.hook;

/**
 * Implement to be the java.lang.ClassLoader pre processor. <p/>ProcessStarter calls once the no-arg constructor of the
 * class implementing this interface and specified with the <code>-Daspectwerkz.classloader.clpreprocessor</code>
 * option. It uses com.tc.aspectwerkz.hook.impl.ClassLoaderPreProcessorImpl by default, which is a ASM
 * implementation (since 2004 10 20).
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 * @see com.tc.aspectwerkz.hook.ProcessStarter
 * @see com.tc.aspectwerkz.hook.impl.ClassLoaderPreProcessorImpl
 */
public interface ClassLoaderPreProcessor {
  /**
   * instruments the java.lang.ClassLoader bytecode
   */
  public byte[] preProcess(byte[] b);
}