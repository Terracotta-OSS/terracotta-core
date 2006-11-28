package com.tc.async.api;

/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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