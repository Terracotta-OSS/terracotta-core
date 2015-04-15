/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
