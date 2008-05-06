/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.net.groups.AbstractGroupMessage;

import java.util.Set;

public class ServerTxnAckMessageFactory {

  public static ServerTxnAckMessage createServerTxnAckMessage(AbstractGroupMessage request, Set serverTxnIDs) {
    ServerTxnAckMessage msg = new ServerTxnAckMessage(request.messageFrom(), request.getMessageID(), serverTxnIDs);
    return msg;
  }

}
