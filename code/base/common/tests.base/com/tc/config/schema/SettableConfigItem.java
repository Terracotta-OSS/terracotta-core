/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
