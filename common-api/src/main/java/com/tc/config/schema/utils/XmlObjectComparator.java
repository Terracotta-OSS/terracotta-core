/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.utils;

import org.apache.xmlbeans.XmlObject;

/**
 * Allows you to compare several {@link XmlObject}s.
 */
public interface XmlObjectComparator {

  boolean equals(XmlObject one, XmlObject two);

  /**
   * This compares two {@link XmlObject} implementations to see if they are semantically equal; it also descends to
   * child objects. It throws an exception instead of returning a value so that you can find out <em>why</em> the two
   * objects aren't equal, since this is a deep compare.
   */
  void checkEquals(XmlObject one, XmlObject two) throws NotEqualException;

}
