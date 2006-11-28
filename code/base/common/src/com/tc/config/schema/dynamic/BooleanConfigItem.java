/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema.dynamic;

/**
 * A {@link ConfigItem} that also exposes its value as a <code>boolean</code>.
 */
public interface BooleanConfigItem extends ConfigItem {

  boolean getBoolean();
  
}
