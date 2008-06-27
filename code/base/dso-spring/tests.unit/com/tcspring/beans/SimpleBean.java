/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcspring.beans;

public class SimpleBean extends SimpleParentBean {

  private int           intField;
  private short         shortField;
  private byte          byteField;
  private char          charField;
  private double        doubleField;
  private float         floatField;

  private int[]         aintField;
  private short[]       ashortField;
  private byte[]        abyteField;
  private char[]        acharField;
  private double[]      adoubleField;
  private float[]       afloatField;

  private Object        ObjectField;
  private Object[]      aObjectField;

  private SimpleBean1   simpleBean;
  private SimpleBean2[] asimpleBean;

  public int getIntField() {
    return intField;
  }

  public short getShortField() {
    return shortField;
  }

  public byte getByteField() {
    return byteField;
  }

  public char getCharField() {
    return charField;
  }

  public double getDoubleField() {
    return doubleField;
  }

  public float getFloatField() {
    return floatField;
  }

  public int[] getAintField() {
    return aintField;
  }

  public short[] getAshortField() {
    return ashortField;
  }

  public byte[] getAbyteField() {
    return abyteField;
  }

  public char[] getAcharField() {
    return acharField;
  }

  public double[] getAdoubleField() {
    return adoubleField;
  }

  public float[] getAfloatField() {
    return afloatField;
  }

  public Object getObjectField() {
    return ObjectField;
  }

  public Object[] getAObjectField() {
    return aObjectField;
  }

  public SimpleBean1 getSimpleBean() {
    return simpleBean;
  }

  public SimpleBean2[] getAsimpleBean() {
    return asimpleBean;
  }

}
