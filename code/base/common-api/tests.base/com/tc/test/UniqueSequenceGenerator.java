/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

/**
 * generates a sequence of unique ints for all kinds of test purposes.
 * NOTE: this sequence starts from 0 and will be reset every time JVM is restarted.
 */
public class UniqueSequenceGenerator {

  public static UniqueSequenceGenerator getInstance() {
    return theInstance;
  }
  
  public synchronized int getNextInt() {
    return this.currentValue++;
  }
  
  private UniqueSequenceGenerator() {
    super();
  }

  private static UniqueSequenceGenerator theInstance = new UniqueSequenceGenerator();
  
  private int currentValue = 84925;
}
