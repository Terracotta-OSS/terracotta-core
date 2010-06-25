/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.dynamic;


public interface BindPortConfigItem extends ConfigItem {
  int getBindPort();

  String getBindAddress();
}
