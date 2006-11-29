/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util;

/**
 * @author steve
 */
public class Conversions {
  public final static void main(String[] args) throws Exception {
    int x = 36;
    System.out.println((x == (x | (x - 2))) ? x : x - 2);
  }
}