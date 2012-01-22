/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.api;

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
/**
 * @author steve Used to filter events. Note, these are evaluted in the context of the sender so they should be fast.
 */
public interface AddPredicate {

  /**
   * Take a look at the context in the thread of the sender and see if you want to take it or ignore it or do something
   * else to it.
   * 
   * @param context
   * @return
   */
  public boolean accept(EventContext context);
}
