/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.utils;

import org.apache.xmlbeans.XmlObject;

/**
 * An {@link XmlObjectComparator} that simply calls {@link Object#equals(Object)} to compare its arguments. Note that
 * this should <strong>NEVER</strong> be used on real {@link XmlObject}s, as they don't implement
 * {@link Object#equals(Object)} correctly. Rather, this is used only for tests.
 */
public class ObjectEqualsXmlObjectComparator implements XmlObjectComparator {

  public boolean equals(XmlObject one, XmlObject two) {
    try {
      checkEquals(one, two);
      return true;
    } catch (NotEqualException nee) {
      return false;
    }
  }

  public void checkEquals(XmlObject one, XmlObject two) throws NotEqualException {
    if ((one == null) != (two == null)) throw new NotEqualException("nullness not the same");
    if (one == null) return;
    if (!one.equals(two)) throw new NotEqualException("not equal");
  }

}
