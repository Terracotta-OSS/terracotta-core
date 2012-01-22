/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.util.Assert;
import com.tc.util.stringification.OurStringBuilder;

/**
 * Represents a root.
 */
public class Root {

  private final String rootName;
  private final String fieldName;

  public Root(String rootName, String fieldName) {
    Assert.assertNotBlank(fieldName);

    this.rootName = rootName;
    this.fieldName = fieldName;
  }

  public String rootName() {
    return this.rootName;
  }

  public String fieldName() {
    return this.fieldName;
  }

  public String toString() {
    return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).append("name", this.rootName)
        .append("field", this.fieldName).toString();
  }
}
