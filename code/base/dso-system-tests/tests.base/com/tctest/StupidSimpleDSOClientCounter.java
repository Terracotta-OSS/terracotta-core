/*
 * Created on Jun 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.tctest;

public class StupidSimpleDSOClientCounter {
  
  private int count;
  
  public int getCount() {
    return this.count;
  }
  
  public void incrementCount() {
    this.count++;
  }
  
  public String toString() {
    return getClass().getName() + ".count= " + this.count;
  }
}
