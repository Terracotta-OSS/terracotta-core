/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.net.groups.GroupMessage;

public class ObjectSyncResetMessageFactory {

  public static GroupMessage createObjectSyncResetRequestMessage() {
    return new ObjectSyncResetMessage(ObjectSyncResetMessage.REQUEST_RESET);
  }

  public static GroupMessage createOKResponse(ObjectSyncResetMessage msg) {
    return new ObjectSyncResetMessage(ObjectSyncResetMessage.OPERATION_SUCCESS, msg.getMessageID());
  }

}
