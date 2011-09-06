/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.stringbuffer;

// An interface to allow StringBufferTest to interact with both java.lang.StringBuilder
// and java.lang.StringBuffer as equals
public interface StringBuddy {
  Object append(boolean b);

  Object append(char c);

  Object append(char[] str, int offset, int len);

  Object append(char[] str);

  Object append(CharSequence s, int start, int end);

  Object append(CharSequence s);

  Object append(double d);

  Object append(float f);

  Object append(int i);

  Object append(long lng);

  Object append(String str);

  Object append(Object sb);

  Object appendCodePoint(int codePoint);

  public int capacity();

  public char charAt(int index);

  public int codePointAt(int index);

  public int codePointBefore(int index);

  public int codePointCount(int beginIndex, int endIndex);

  Object delete(int start, int end);

  Object deleteCharAt(int index);

  public void ensureCapacity(int minimumCapacity);

  public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin);

  public int indexOf(String str, int fromIndex);

  public int indexOf(String str);

  Object insert(int offset, boolean b);

  Object insert(int offset, char c);

  Object insert(int index, char[] str, int offset, int len);

  Object insert(int offset, char[] str);

  Object insert(int dstOffset, CharSequence s, int start, int end);

  Object insert(int dstOffset, CharSequence s);

  Object insert(int offset, double d);

  Object insert(int offset, float f);

  Object insert(int offset, int i);

  Object insert(int offset, long l);

  Object insert(int offset, Object obj);

  Object insert(int offset, String str);

  public int lastIndexOf(String str, int fromIndex);

  public int lastIndexOf(String str);

  public int length();

  public int offsetByCodePoints(int index, int codePointOffset);

  Object replace(int start, int end, String str);

  Object reverse();

  public void setCharAt(int index, char ch);

  public void setLength(int newLength);

  public CharSequence subSequence(int start, int end);

  public String substring(int start, int end);

  public String substring(int start);

  public void trimToSize();
}