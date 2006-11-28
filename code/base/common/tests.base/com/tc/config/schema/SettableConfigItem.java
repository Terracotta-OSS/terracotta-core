/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema;

/**
 * A {@link ConfigItem} that lets you set its value; useful for tests.
 */
public interface SettableConfigItem {

  void setValue(Object newValue);

  void setValue(int newValue);

  void setValue(boolean newValue);

}
