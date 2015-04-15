/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
