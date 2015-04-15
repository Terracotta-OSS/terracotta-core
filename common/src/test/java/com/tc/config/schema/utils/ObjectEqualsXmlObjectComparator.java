/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.config.schema.utils;

import org.apache.xmlbeans.XmlObject;

/**
 * An {@link XmlObjectComparator} that simply calls {@link Object#equals(Object)} to compare its arguments. Note that
 * this should <strong>NEVER</strong> be used on real {@link XmlObject}s, as they don't implement
 * {@link Object#equals(Object)} correctly. Rather, this is used only for tests.
 */
public class ObjectEqualsXmlObjectComparator implements XmlObjectComparator {

  @Override
  public boolean equals(XmlObject one, XmlObject two) {
    try {
      checkEquals(one, two);
      return true;
    } catch (NotEqualException nee) {
      return false;
    }
  }

  @Override
  public void checkEquals(XmlObject one, XmlObject two) throws NotEqualException {
    if ((one == null) != (two == null)) throw new NotEqualException("nullness not the same");
    if (one == null) return;
    if (!one.equals(two)) throw new NotEqualException("not equal");
  }

}
