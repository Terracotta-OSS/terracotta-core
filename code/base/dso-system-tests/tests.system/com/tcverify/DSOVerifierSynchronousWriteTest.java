/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcverify;


/**
 * A quick/shallow test that uses the DSOVerifier client program
 */
public class DSOVerifierSynchronousWriteTest extends DSOVerifierTest {
  
  protected boolean isSynchronousWrite() {
    return true;
  }

  protected String getMainClass() {
    return DSOVerifier.class.getName();
  }
}
