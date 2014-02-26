/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.search;

import com.tc.search.SearchRequestID;

import java.util.concurrent.atomic.AtomicLong;

public class SearchRequestIDGenerator {
  
  private AtomicLong requestID = new AtomicLong(0);
  
  
  public SearchRequestID getNextRequestID() {
    return new SearchRequestID(requestID.incrementAndGet());
  }

}
