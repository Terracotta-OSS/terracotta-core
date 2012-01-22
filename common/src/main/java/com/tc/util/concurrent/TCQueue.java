/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

public interface TCQueue {
  /**
   * Adds the object in the queue
   * @throws InterruptedException 
   */
  public void put(Object obj) throws InterruptedException;
  
  /**
   * Place item in channel only if it can be accepted within msecs milliseconds
   */
  public boolean offer(Object obj, long timeout) throws InterruptedException;
  
  /**
   * Return and remove an item from channel, possibly waiting indefinitely until such an item exists
   * @throws InterruptedException 
   */
  public Object take() throws InterruptedException;
  
  /**
   * Return and remove an item from channel only if one is available within msecs milliseconds
   * @throws InterruptedException 
   */
  public Object poll(long timeout) throws InterruptedException;
  
  /**
   * Return, but do not remove object at head of Channel, or null if it is empty
   */
  public Object peek();

  /**
   * Returns the size of the queue
   */
  public int size();
  
  /**
   * Tells whether queue is empty or not
   */
  public boolean isEmpty();
}
