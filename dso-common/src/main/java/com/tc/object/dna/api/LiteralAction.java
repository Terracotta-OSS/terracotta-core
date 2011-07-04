/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.api;


/**
 * A Literal Value Action.
 */
public class LiteralAction {
  private final Object     value;

  public LiteralAction(Object value) {
    this.value = value;
  }
  
  public Object getObject() {
    return this.value;
  }
}