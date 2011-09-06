/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.stringbuffer;


public final class StringBuilderBuddy implements StringBuddy {
  private final StringBuilder builder;

  public StringBuilderBuddy(StringBuilder builder) {
    this.builder = builder;
  }

  public Object append(boolean b) {
    return builder.append(b);
  }

  public Object append(char c) {
    return builder.append(c);
  }

  public Object append(char[] str, int offset, int len) {
    return builder.append(str, offset, len);
  }

  public Object append(char[] str) {
    return builder.append(str);
  }

  public Object append(CharSequence s, int start, int end) {
    return builder.append(s, start, end);
  }

  public Object append(CharSequence s) {
    return builder.append(s);
  }

  public Object append(double d) {
    return builder.append(d);
  }

  public Object append(float f) {
    return builder.append(f);
  }

  public Object append(int i) {
    return builder.append(i);
  }

  public Object append(long lng) {
    return builder.append(lng);
  }

  public Object append(Object obj) {
    return builder.append(obj);
  }

  public Object append(String str) {
    return builder.append(str);
  }

  public Object append(StringBuffer sb) {
    return builder.append(sb);
  }

  public Object appendCodePoint(int codePoint) {
    return builder.appendCodePoint(codePoint);
  }

  public int capacity() {
    return builder.capacity();
  }

  public char charAt(int index) {
    return builder.charAt(index);
  }

  public int codePointAt(int index) {
    return builder.codePointAt(index);
  }

  public int codePointBefore(int index) {
    return builder.codePointBefore(index);
  }

  public int codePointCount(int beginIndex, int endIndex) {
    return builder.codePointCount(beginIndex, endIndex);
  }

  public Object delete(int start, int end) {
    return builder.delete(start, end);
  }

  public Object deleteCharAt(int index) {
    return builder.deleteCharAt(index);
  }

  public void ensureCapacity(int minimumCapacity) {
    builder.ensureCapacity(minimumCapacity);
  }

  public boolean equals(Object obj) {
    return builder.equals(obj);
  }

  public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
    builder.getChars(srcBegin, srcEnd, dst, dstBegin);
  }

  public int hashCode() {
    return builder.hashCode();
  }

  public int indexOf(String str, int fromIndex) {
    return builder.indexOf(str, fromIndex);
  }

  public int indexOf(String str) {
    return builder.indexOf(str);
  }

  public Object insert(int offset, boolean b) {
    return builder.insert(offset, b);
  }

  public Object insert(int offset, char c) {
    return builder.insert(offset, c);
  }

  public Object insert(int index, char[] str, int offset, int len) {
    return builder.insert(index, str, offset, len);
  }

  public Object insert(int offset, char[] str) {
    return builder.insert(offset, str);
  }

  public Object insert(int dstOffset, CharSequence s, int start, int end) {
    return builder.insert(dstOffset, s, start, end);
  }

  public Object insert(int dstOffset, CharSequence s) {
    return builder.insert(dstOffset, s);
  }

  public Object insert(int offset, double d) {
    return builder.insert(offset, d);
  }

  public Object insert(int offset, float f) {
    return builder.insert(offset, f);
  }

  public Object insert(int offset, int i) {
    return builder.insert(offset, i);
  }

  public Object insert(int offset, long l) {
    return builder.insert(offset, l);
  }

  public Object insert(int offset, Object obj) {
    return builder.insert(offset, obj);
  }

  public Object insert(int offset, String str) {
    return builder.insert(offset, str);
  }

  public int lastIndexOf(String str, int fromIndex) {
    return builder.lastIndexOf(str, fromIndex);
  }

  public int lastIndexOf(String str) {
    return builder.lastIndexOf(str);
  }

  public int length() {
    return builder.length();
  }

  public int offsetByCodePoints(int index, int codePointOffset) {
    return builder.offsetByCodePoints(index, codePointOffset);
  }

  public Object replace(int start, int end, String str) {
    return builder.replace(start, end, str);
  }

  public Object reverse() {
    return builder.reverse();
  }

  public void setCharAt(int index, char ch) {
    builder.setCharAt(index, ch);
  }

  public void setLength(int newLength) {
    builder.setLength(newLength);
  }

  public CharSequence subSequence(int start, int end) {
    return builder.subSequence(start, end);
  }

  public String substring(int start, int end) {
    return builder.substring(start, end);
  }

  public String substring(int start) {
    return builder.substring(start);
  }

  public String toString() {
    return builder.toString();
  }

  public void trimToSize() {
    builder.trimToSize();
  }

}