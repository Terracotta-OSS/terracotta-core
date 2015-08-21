/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
