/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema.dynamic;

/**
 * A {@link ConfigItem} that also exposes its value as an array of objects.
 */
public interface ObjectArrayConfigItem extends ConfigItem {

  Object[] getObjects();
  
}
