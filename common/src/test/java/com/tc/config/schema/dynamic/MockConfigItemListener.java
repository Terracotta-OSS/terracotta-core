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
package com.tc.config.schema.dynamic;

/**
 * Unit test for {@link ConfigItemListener}.
 */
public class MockConfigItemListener implements ConfigItemListener {

  private int    numValueChangeds;
  private Object lastOldValue;
  private Object lastNewValue;

  public MockConfigItemListener() {
    reset();
  }

  public void reset() {
    this.numValueChangeds = 0;
    this.lastOldValue = null;
    this.lastNewValue = null;
  }

  @Override
  public void valueChanged(Object oldValue, Object newValue) {
    ++this.numValueChangeds;
    this.lastOldValue = oldValue;
    this.lastNewValue = newValue;
  }

  public Object getLastNewValue() {
    return lastNewValue;
  }

  public Object getLastOldValue() {
    return lastOldValue;
  }

  public int getNumValueChangeds() {
    return numValueChangeds;
  }

}
