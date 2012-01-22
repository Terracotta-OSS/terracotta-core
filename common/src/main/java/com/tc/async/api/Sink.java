/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.api;

import com.tc.stats.Monitorable;

import java.util.Collection;

/**
 * Represents the sink in the SEDA system
 */
public interface Sink extends Monitorable {
  /**
   * The context may or may not be added to the sink depending on the state of the sink. The implementation can make the
   * decision based on various factors.
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
   * Get the predicate 
   * 
   */
  public AddPredicate getPredicate();

  /**
   * returns the current size of the queue
   * 
   * @return
   */
  public int size();

  public void clear();

}
