/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

public class AnonymousTraversedReference implements TraversedReference {

  private final Object value;

  public AnonymousTraversedReference(Object value) {
    this.value = value;
  }
  
  public Object getValue() {
    return value;
  }

  public boolean isAnonymous() {
    return true;
  }

  public String getFullyQualifiedReferenceName() {
    return null;
  }

}
