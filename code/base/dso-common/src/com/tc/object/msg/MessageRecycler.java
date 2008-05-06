/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.lang.Recyclable;

import java.util.Set;

public interface MessageRecycler {
  
  /*
   * adds a DSOMessage that needs to be recycled at a latter point in time, along with
   * the set of keys that needs to be processed before the message can be recycled. These
   * keys should be unique across the calls.
   */
  public void addMessage(Recyclable message, Set keys);
  
  /*
   * Indicates that the key is processed. The message associated with the key will be recycled
   * iff there are no more keys associated with it. 
   * 
   * @returns true if the Message associated with this key was recycled.
   */
  public boolean recycle(Object key);

}
