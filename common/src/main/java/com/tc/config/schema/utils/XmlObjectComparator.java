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
