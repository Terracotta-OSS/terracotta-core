/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store;

import com.tc.util.Assert;
import com.tc.util.concurrent.CopyOnWriteArrayMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class TextualDataFormat {
  private final static Map FORMATS = new CopyOnWriteArrayMap();

  public final static TextualDataFormat CSV = new TextualDataFormat("CSV");
  public final static TextualDataFormat XML = new TextualDataFormat("XML");

  private final String identifier;

  private TextualDataFormat(final String identifier) {
    Assert.assertNotNull("identifier", identifier);
    this.identifier = identifier;
    FORMATS.put(identifier, this);
  }

  public static TextualDataFormat getFormat(final String identifier) {
    if (null == identifier) {
      return null;
    }
    return (TextualDataFormat)FORMATS.get(identifier.toUpperCase());
  }

  public static Collection getAllFormats() {
    return Collections.unmodifiableCollection(FORMATS.values());
  }

  public String toString() {
    return identifier;
  }

  public int hashCode() {
    return identifier.hashCode();
  }

  public boolean equals(final Object object) {
    if (null == object) {
      return false;
    }

    return (object instanceof TextualDataFormat)
           && ((TextualDataFormat)object).identifier.equals(identifier);
  }
}
