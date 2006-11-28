/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util;

/**
 * @author steve To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code
 *         Generation&gt;Code and Comments
 */
public class Conversions {
  public final static void main(String[] args) throws Exception {
    int x = 36;
    System.out.println((x == (x | (x - 2))) ? x : x - 2);
  }
}