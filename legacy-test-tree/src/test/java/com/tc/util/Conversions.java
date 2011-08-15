/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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