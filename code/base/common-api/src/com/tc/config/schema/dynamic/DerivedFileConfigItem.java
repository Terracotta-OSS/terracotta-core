/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.dynamic;

public abstract class DerivedFileConfigItem extends DerivedConfigItem implements FileConfigItem {

  public DerivedFileConfigItem(ConfigItem[] derivedFrom) {
    super(derivedFrom);
  }

}
