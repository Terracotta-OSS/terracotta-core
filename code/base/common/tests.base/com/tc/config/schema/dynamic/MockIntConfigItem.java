/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
