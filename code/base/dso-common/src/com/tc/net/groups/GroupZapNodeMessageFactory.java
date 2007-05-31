/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.groups;

public class GroupZapNodeMessageFactory {

  public static GroupMessage createGroupZapNodeMessage(int type, String reason) {
    return new GroupZapNodeMessage(GroupZapNodeMessage.ZAP_NODE_REQUEST, type, reason);
  }

}
