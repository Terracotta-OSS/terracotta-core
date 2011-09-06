/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.TCObject;

import java.io.Serializable;

/**
 * @author steve
 */
public class FooObject implements Serializable {
  private String      one;
  private long        two;
  private FooObject   three;
  public FooObject    four;
  private FooObject[] five;
  public TCObject     __tcObjecta;

  public void test() {
    synchronized (this) {
      throw new Error();
    }
  }

  public FooObject(Object t) {
    super();
  }

  public FooObject() {
    this(new Object());
  }

  public static class InnerClassTest {
    public InnerClassTest(Object t) {
      super();
    }
  }

  public FooObject(String one, long two, FooObject three, FooObject four, FooObject[] five) {

    this.one = one;
    this.two = two;
    this.three = three;
    this.four = four;
    this.five = five;
  }

  /**
   * @return
   */
  public FooObject getFour() {
    return four;
  }

  /**
   * @return
   */
  public String getOne() {
    return one;
  }

  /**
   * @return
   */
  public FooObject getThree() {
    return three;
  }

  /**
   * @return
   */
  public long getTwo() {
    return two;
  }

  /**
   * @param object
   */
  public void setFour(FooObject object) {
    four = object;
  }

  /**
   * @param string
   */
  public void setOne(String string) {
    one = string;
  }

  /**
   * @param object
   */
  public void setThree(FooObject object) {
    three = object;
  }

  /**
   * @param l
   */
  public void setTwo(long l) {
    two = l;
  }

  /**
   * @return
   */
  public FooObject[] getFive() {
    return five;
  }

  /**
   * @param objects
   */
  public void setFive(FooObject[] objects) {
    five = objects;
  }

}