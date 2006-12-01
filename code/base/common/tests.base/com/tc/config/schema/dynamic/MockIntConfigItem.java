/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

/**
 * A mock {@link IntConfigItem}, for use in tests.
 */
public class MockIntConfigItem extends MockConfigItem implements IntConfigItem {

  public MockIntConfigItem(int value) {
    super(new Integer(value));
  }

  public int getInt() {
    return ((Integer) getObject()).intValue();
  }

}
