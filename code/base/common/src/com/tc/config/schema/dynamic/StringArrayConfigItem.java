/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

/**
 * A {@link ConfigItem} that also exposes its value as an array of {@link String}s.
 */
public interface StringArrayConfigItem extends ConfigItem {

  String[] getStringArray();
  
}
