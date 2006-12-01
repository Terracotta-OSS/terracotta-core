/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;

public interface AcknowledgeTransactionMessage {

  public void initialize(ChannelID channelID, TransactionID txID);

  public ChannelID getRequesterID();

  public TransactionID getRequestID();

  public void send();

  public ChannelID getChannelID();
  
  public SessionID getLocalSessionID();

}