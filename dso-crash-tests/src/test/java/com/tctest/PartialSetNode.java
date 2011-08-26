/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;


public class PartialSetNode implements Comparable{
  private int i;

  public PartialSetNode(int i) {
    this.i = i;
  }

  public int getNumber() {
    return i;
  }

  @Override
  public boolean equals(Object obj) {
    PartialSetNode number = (PartialSetNode) obj;
    return number.i == this.i;
  }

  @Override
  public int hashCode() {
    return i;
  }

  public int compareTo(Object o) {
    PartialSetNode other = (PartialSetNode)o;
    return this.i - other.i;
  }
}
