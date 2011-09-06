/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

public class GroupZapNodeMessageFactory {

  public static GroupMessage createGroupZapNodeMessage(int type, String reason, long[] weights) {
    return new GroupZapNodeMessage(GroupZapNodeMessage.ZAP_NODE_REQUEST, type, reason, weights);
  }

}
