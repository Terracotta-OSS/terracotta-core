/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;

/**
 * @author steve Using the NullObject pattern to put a predicate in when no predicate exists
 */
public class DefaultAddPredicate implements AddPredicate {

  private final static AddPredicate instance = new DefaultAddPredicate();

  public static AddPredicate getInstance() {
    return instance;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.async.api.AddPredicate#accept(com.tc.async.api.EventContext)
   */
  public boolean accept(EventContext context) {
    return true;
  }

}
