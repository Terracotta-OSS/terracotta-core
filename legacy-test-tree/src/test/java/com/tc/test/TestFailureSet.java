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
package com.tc.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestFailureSet {
  private final List list = new ArrayList();

  public void put(TestFailure f) {
    synchronized (list) {
      list.add(f);
    }
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer("Test failures...\n");
    synchronized (list) {
      for (Iterator i = list.iterator(); i.hasNext();) {
        TestFailure f = (TestFailure) i.next();
        buf.append(f + "\n");
      }
    }
    return buf.toString();
  }

  public int size() {
    synchronized (list) {
      return list.size();
    }
  }
}