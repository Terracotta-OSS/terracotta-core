/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

