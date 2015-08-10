package com.tc.entity;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;


/**
 * VoltronEntityMessage is primarily used over the network but it also has server-internal "loopback" messages so this
 * interface specifically describes how the network variant would work.
 */
public interface NetworkVoltronEntityMessage extends VoltronEntityMessage, TCMessage {
  /**
   * Initializes the contents of the message.
   */
  public void setContents(ClientID clientID, TransactionID transactionID, EntityDescriptor entityDescriptor, Type type, boolean requiresReplication, byte[] extendedData);
}
