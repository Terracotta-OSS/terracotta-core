/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupZapNodeMessage;

public class GroupZapNodeMessageFactory {

  public static AbstractGroupMessage createGroupZapNodeMessage(int type, String reason, long[] weights) {
    return new GroupZapNodeMessage(GroupZapNodeMessage.ZAP_NODE_REQUEST, type, reason, weights);
  }

}
