/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import java.io.Serializable;

/**
 * @author steve To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code
 *         Generation&gt;Code and Comments
 */
public class FooObject1 implements Serializable {
  private long       value = 0;
  private String     name  = "Default name";
  private FooObject1 foo;

  public FooObject1 getFooObject1() {
    return foo;
  }

  public void setFooObject1(FooObject1 foo) {
    this.foo = foo;
  }

  /**
   * @return
   */
  public String getName() {
    return name;
  }

  /**
   * @return
   */
  public long getValue() {
    return value;
  }

  /**
   * @param string
   */
  public void setName(String string) {
    name = string;
  }

  /**
   * @param l
   */
  public void setValue(long l) {
    value = l;
  }

  public String toString() {
    return "FooObject name:" + name + " value:" + value;
  }
}

