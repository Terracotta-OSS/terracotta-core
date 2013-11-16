/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.SearchRequestID;

public interface SearchRequestMessage extends TCMessage {

  /**
   * ClientID
   */
  public ClientID getClientID();

  /**
   * Search Identifier. return SearchRequestID requestID
   */
  public SearchRequestID getRequestID();

  /**
   * Name of cache to query against.
   * 
   * @return String string.
   */
  public String getCacheName();

}
