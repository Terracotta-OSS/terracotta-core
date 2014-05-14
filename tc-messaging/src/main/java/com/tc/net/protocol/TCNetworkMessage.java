/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.lang.Recyclable;

/**
 * @author teck
 */
public interface TCNetworkMessage extends EventContext, Recyclable {

  public TCNetworkHeader getHeader();

  public TCNetworkMessage getMessagePayload();

  public TCByteBuffer[] getPayload();

  public TCByteBuffer[] getEntireMessageData();

  public boolean isSealed();

  public void seal();

  public int getDataLength();

  public int getHeaderLength();

  public int getTotalLength();

  public void wasSent();

  public void setSentCallback(Runnable callback);
  
  public Runnable getSentCallback();
}
