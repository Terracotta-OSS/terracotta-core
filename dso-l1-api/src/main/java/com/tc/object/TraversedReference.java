/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;


public interface TraversedReference {

  /**
   * The value of this reference
   */
  public Object getValue();

  /**
   * true if the reference is not a field reference (e.g., an internal reference of a logically managed class)
   */
  public boolean isAnonymous();

  /**
   * The name of the field if it's a field reference or null if it's an anonymous reference.
   */
  public String getFullyQualifiedReferenceName();
}
