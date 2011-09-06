/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.stringbuffer;


public final class StringBufferBuddy implements StringBuddy {

  private final StringBuffer buffer;

  public StringBufferBuddy(StringBuffer buffer) {
    this.buffer = buffer;
  }

  public Object append(boolean b) {
    return buffer.append(b);
  }

  public Object append(char c) {
    return buffer.append(c);
  }

  public Object append(char[] str, int offset, int len) {
    return buffer.append(str, offset, len);
  }

  public Object append(char[] str) {
    return buffer.append(str);
  }

  public Object append(CharSequence s, int start, int end) {
    return buffer.append(s, start, end);
  }

  public Object append(CharSequence s) {
    return buffer.append(s);
  }

  public Object append(double d) {
    return buffer.append(d);
  }

  public Object append(float f) {
    return buffer.append(f);
  }

  public Object append(int i) {
    return buffer.append(i);
  }

  public Object append(long lng) {
    return buffer.append(lng);
  }

  public Object append(Object obj) {
    return buffer.append(obj);
  }

  public Object append(String str) {
    return buffer.append(str);
  }

  public Object appendCodePoint(int codePoint) {
    return buffer.appendCodePoint(codePoint);
  }

  public int capacity() {
    return buffer.capacity();
  }

  public char charAt(int index) {
    return buffer.charAt(index);
  }

  public int codePointAt(int index) {
    return buffer.codePointAt(index);
  }

  public int codePointBefore(int index) {
    return buffer.codePointBefore(index);
  }

  public int codePointCount(int beginIndex, int endIndex) {
    return buffer.codePointCount(beginIndex, endIndex);
  }

  public Object delete(int start, int end) {
    return buffer.delete(start, end);
  }

  public Object deleteCharAt(int index) {
    return buffer.deleteCharAt(index);
  }

  public void ensureCapacity(int minimumCapacity) {
    buffer.ensureCapacity(minimumCapacity);
  }

  public boolean equals(Object obj) {
    return buffer.equals(obj);
  }

  public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
    buffer.getChars(srcBegin, srcEnd, dst, dstBegin);
  }

  public int hashCode() {
    return buffer.hashCode();
  }

  public int indexOf(String str, int fromIndex) {
    return buffer.indexOf(str, fromIndex);
  }

  public int indexOf(String str) {
    return buffer.indexOf(str);
  }

  public Object insert(int offset, boolean b) {
    return buffer.insert(offset, b);
  }

  public Object insert(int offset, char c) {
    return buffer.insert(offset, c);
  }

  public Object insert(int index, char[] str, int offset, int len) {
    return buffer.insert(index, str, offset, len);
  }

  public Object insert(int offset, char[] str) {
    return buffer.insert(offset, str);
  }

  public Object insert(int dstOffset, CharSequence s, int start, int end) {
    return buffer.insert(dstOffset, s, start, end);
  }

  public Object insert(int dstOffset, CharSequence s) {
    return buffer.insert(dstOffset, s);
  }

  public Object insert(int offset, double d) {
    return buffer.insert(offset, d);
  }

  public Object insert(int offset, float f) {
    return buffer.insert(offset, f);
  }

  public Object insert(int offset, int i) {
    return buffer.insert(offset, i);
  }

  public Object insert(int offset, long l) {
    return buffer.insert(offset, l);
  }

  public Object insert(int offset, Object obj) {
    return buffer.insert(offset, obj);
  }

  public Object insert(int offset, String str) {
    return buffer.insert(offset, str);
  }

  public int lastIndexOf(String str, int fromIndex) {
    return buffer.lastIndexOf(str, fromIndex);
  }

  public int lastIndexOf(String str) {
    return buffer.lastIndexOf(str);
  }

  public int length() {
    return buffer.length();
  }

  public int offsetByCodePoints(int index, int codePointOffset) {
    return buffer.offsetByCodePoints(index, codePointOffset);
  }

  public Object replace(int start, int end, String str) {
    return buffer.replace(start, end, str);
  }

  public Object reverse() {
    return buffer.reverse();
  }

  public void setCharAt(int index, char ch) {
    buffer.setCharAt(index, ch);
  }

  public void setLength(int newLength) {
    buffer.setLength(newLength);
  }

  public CharSequence subSequence(int start, int end) {
    return buffer.subSequence(start, end);
  }

  public String substring(int start, int end) {
    return buffer.substring(start, end);
  }

  public String substring(int start) {
    return buffer.substring(start);
  }

  public String toString() {
    return buffer.toString();
  }

  public void trimToSize() {
    buffer.trimToSize();
  }
}