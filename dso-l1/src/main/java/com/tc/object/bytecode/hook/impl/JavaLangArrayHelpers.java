/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.object.TCObjectExternal;
import com.tc.object.bytecode.ManagerUtil;

public class JavaLangArrayHelpers {
  public static final String CLASS = "com/tc/object/bytecode/hook/impl/JavaLangArrayHelpers";

  /**
   * Optimization used by String.getChars(int, int, char[], int)
   */
  public static void charArrayCopy(char[] src, int srcPos, char[] dest, int destPos, int length) {
    TCObjectExternal tco = ManagerUtil.getObject(dest);
    if (tco != null) {
      ManagerUtil.charArrayCopy(src, srcPos, dest, destPos, length, tco);
    } else {
      System.arraycopy(src, srcPos, dest, destPos, length);
    }
  }

  public static void javaLangStringGetBytes(int srcBegin, int srcEnd, byte dst[], int dstBegin, char[] value) {
    javaLangStringGetBytes(srcBegin, srcEnd, dst, dstBegin, value.length, 0, value);
  }

  /**
   * The entire implementation of String.getBytes() is replaced by a call to this method
   */
  public static void javaLangStringGetBytes(int srcBegin, int srcEnd, byte dst[], int dstBegin, int count, int offset,
                                            char[] value) {
    if (srcBegin < 0) { throw new StringIndexOutOfBoundsException(srcBegin); }
    if (srcEnd > count) { throw new StringIndexOutOfBoundsException(srcEnd); }
    if (srcBegin > srcEnd) { throw new StringIndexOutOfBoundsException(srcEnd - srcBegin); }
    int j = dstBegin;
    int n = offset + srcEnd;
    int i = offset + srcBegin;
    char[] val = value;

    TCObjectExternal tco = ManagerUtil.getObject(dst);
    if (tco != null) {
      while (i < n) {
        dst[j] = (byte) val[i++];
        tco.byteFieldChanged(null, null, dst[j], j++);
      }
    } else {
      while (i < n) {
        dst[j++] = (byte) val[i++];
      }
    }
  }

  /**
   * Called by the 1.5 implementation of java/lang/AbstractStringBuilder.append(int|long)
   */
  public static void javaLangAbstractStringBuilderAppend(char[] value, int appendLength, int bufferLength) {
    TCObjectExternal tco = ManagerUtil.getObject(value);

    if (tco != null) {
      int index = bufferLength - appendLength;

      while (index < bufferLength) {
        tco.charFieldChanged(null, null, value[index], index++);
      }
    }
  }

}
