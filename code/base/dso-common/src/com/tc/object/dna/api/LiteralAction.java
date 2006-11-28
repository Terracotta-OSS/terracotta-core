/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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