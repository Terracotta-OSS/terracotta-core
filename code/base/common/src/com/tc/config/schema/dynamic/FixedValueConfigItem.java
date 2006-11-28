/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema.dynamic;

/**
 * A {@link ConfigItem} that always returns the same thing. Useful for tests.
 */
public class FixedValueConfigItem implements ConfigItem {

  private final Object value;

  public FixedValueConfigItem(Object value) {
    this.value = value;
  }

  public Object getObject() {
    return this.value;
  }

  public void addListener(ConfigItemListener changeListener) {
    // nothing here; this object never changes
  }

  public void removeListener(ConfigItemListener changeListener) {
    // nothing here; this object never changes
  }

}
