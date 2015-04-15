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
package com.tc.config.schema;

import com.tc.config.schema.dynamic.ConfigItem;

/**
 * A mock {@link IllegalConfigurationChangeHandler}, for use in tests.
 */
public class MockIllegalConfigurationChangeHandler implements IllegalConfigurationChangeHandler {

  private int        numChangeFaileds;
  private ConfigItem lastItem;
  private Object     lastOldValue;
  private Object     lastNewValue;

  public MockIllegalConfigurationChangeHandler() {
    reset();
  }

  public void reset() {
    this.numChangeFaileds = 0;
    this.lastItem = null;
    this.lastOldValue = null;
    this.lastNewValue = null;
  }

  @Override
  public void changeFailed(ConfigItem item, Object oldValue, Object newValue) {
    ++this.numChangeFaileds;
    this.lastItem = item;
    this.lastOldValue = oldValue;
    this.lastNewValue = newValue;
  }

  public ConfigItem getLastItem() {
    return lastItem;
  }

  public Object getLastNewValue() {
    return lastNewValue;
  }

  public Object getLastOldValue() {
    return lastOldValue;
  }

  public int getNumChangeFaileds() {
    return numChangeFaileds;
  }

}
