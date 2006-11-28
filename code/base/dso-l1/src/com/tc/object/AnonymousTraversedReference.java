/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
