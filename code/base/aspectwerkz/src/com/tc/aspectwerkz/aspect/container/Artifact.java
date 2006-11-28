/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.aspectwerkz.aspect.container;

/**
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
class Artifact {
  public Artifact(String className, byte[] bytecode) {
    this.className = className;
    this.bytecode = bytecode;
  }

  public byte[] bytecode;
  public String className;
}

