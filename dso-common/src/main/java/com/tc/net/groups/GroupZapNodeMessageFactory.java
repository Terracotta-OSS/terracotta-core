/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

public class GroupZapNodeMessageFactory {

  public static GroupMessage createGroupZapNodeMessage(int type, String reason, long[] weights) {
    return new GroupZapNodeMessage(GroupZapNodeMessage.ZAP_NODE_REQUEST, type, reason, weights);
  }

}
