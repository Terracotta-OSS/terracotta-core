/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm.msgs;

import com.tc.net.protocol.tcm.CommunicationsManager;

import java.text.MessageFormat;

public class CommsMessageFactory {
  public static String createReconnectRejectMessage(String commsMgrName, Object[] arguments){
    if (commsMgrName.equals(CommunicationsManager.COMMSMGR_GROUPS)){
      return MessageFormat.format(CommsMessagesResource.getL2L2RejectionMessage(), arguments);
    }else {
      return MessageFormat.format(CommsMessagesResource.getL2L1RejectionMessage(), arguments);
    }
  }
}
