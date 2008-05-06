/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

/**
 * @author steve To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code
 *         Generation&gt;Code and Comments
 */
public class InnerClassTestObject {
  private InnerClass myInner;

  /**
   * 
   */
  public InnerClassTestObject() {
    super();
    myInner = new InnerClass();
  }

  protected InnerClass getMyInner() {
    return myInner;
  }

  private String getHelloWorld() {
    return "Hello World";
  }

  public class InnerClass {
    String myString;

    public void test() {
      this.myString = getHelloWorld() + System.currentTimeMillis();
    }

  }

}