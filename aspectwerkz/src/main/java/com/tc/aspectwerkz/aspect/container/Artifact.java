/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

