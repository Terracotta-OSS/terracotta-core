/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store;

import com.tc.util.Assert;

import java.util.Arrays;
import java.util.Collection;

public enum TextualDataFormat {

  CSV("CSV"), XML("XML");

  private final String identifier;

  private TextualDataFormat(final String identifier) {
    Assert.assertNotNull("identifier", identifier);
    this.identifier = identifier;
  }

  public static TextualDataFormat getFormat(final String identifier) {
    return identifier == null ? null : valueOf(identifier.toUpperCase());
  }

  public static Collection<TextualDataFormat> getAllFormats() {
    return Arrays.asList(values());
  }

  @Override
  public String toString() {
    return identifier;
  }

}
