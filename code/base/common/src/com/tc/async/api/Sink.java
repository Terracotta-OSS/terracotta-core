/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.async.api;

import com.tc.stats.Monitorable;

import java.util.Collection;
import java.util.List;

/**
 * @author steve
 */
public interface Sink extends Monitorable {
  /**
   * Add context but it may never get executed if the stage decides it is too busy
   * 
   * @param context
   * @return
   */
  public boolean addLossy(EventContext context);

  /**
   * Add More than one context at a time. This is more efficient then adding one at a time
   * 
   * @param contexts
   */
  public void addMany(Collection contexts);

  /**
   * Add a event to the Sink (no, really!)
   * 
   * @param context
   */
  public void add(EventContext context);

  /**
   * The predicate allows the Sink to reject the EventContext rather than handle it
   * 
   * @param predicate
   */
  public void setAddPredicate(AddPredicate predicate);

  /**
   * Get the predicate (I hate the useless javadocs)
   * 
   * @return
   */
  public AddPredicate getPredicate();

  /**
   * returns the current size of the queue
   * 
   * @return
   */
  public int size();
  
  public void clear();

  public void pause(List pauseEvents);
  
  public void unpause();
}