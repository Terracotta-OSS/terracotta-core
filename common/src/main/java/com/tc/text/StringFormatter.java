/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.text;

public class StringFormatter {
  
  private final String nl;
  
  public StringFormatter() {
    nl = System.getProperty("line.separator");
  }
  
  public String newline() {
    return nl;
  }
  
  public String leftPad(int size, Object s) {
    return pad(false, size, s);
  }
  

  public String leftPad(int size, int i) {
    return leftPad(size, "" + i);
  }
  
  public String rightPad(int size, Object s) {
    return pad(true, size, s);
  }

  public String rightPad(int size, int i) {
    return rightPad(size, "" + i);
  }

  private String pad(boolean right, int size, Object s) {
    StringBuffer buf = new StringBuffer();
    buf.append(s);
    while (buf.length() < size) {
      if (right) buf.append(" ");
      else buf.insert(0, " ");
    }
    while (buf.length() > size) {
      buf.deleteCharAt(buf.length() - 1);
      if (buf.length() == size) {
        buf.deleteCharAt(buf.length() - 1).append("~");
      }
    }
    return buf.toString();
  }

}
