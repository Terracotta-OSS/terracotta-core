/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import java.nio.ByteBuffer;
import java.security.ProtectionDomain;

/**
 * The only purpose of this class is to localize the references to the java.nio.ByteBuffer type so that we don't have to
 * use reflection
 */
public class ClassProcessorHelperJDK15 {

  public static ByteBuffer defineClass0Pre(ClassLoader caller, String name, ByteBuffer buffer, int off, int len,
                                           ProtectionDomain pd) {

    final byte[] origBytes;
    final int offset;

    if (buffer.hasArray()) {
      origBytes = buffer.array();
      offset = buffer.arrayOffset() + buffer.position();
    } else {
      origBytes = new byte[len];
      offset = 0;
      int origPos = buffer.position();
      try {
        buffer.get(origBytes);
      } finally {
        buffer.position(origPos);
      }
    }

    byte[] possiblyTransformed = ClassProcessorHelper.defineClass0Pre(caller, name, origBytes, offset, len, pd);

    if (possiblyTransformed == origBytes) { return buffer; }

    ByteBuffer returnValue;
    if (buffer.isDirect()) {
      returnValue = ByteBuffer.allocateDirect(possiblyTransformed.length);
    } else {
      returnValue = ByteBuffer.allocate(possiblyTransformed.length);
    }

    returnValue.put(possiblyTransformed);
    returnValue.position(0);
    return returnValue;
  }

}
