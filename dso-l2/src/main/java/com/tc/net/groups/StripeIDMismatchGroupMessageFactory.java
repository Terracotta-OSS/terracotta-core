/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.GroupID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.StripeIDMismatchGroupMessage;

public class StripeIDMismatchGroupMessageFactory {

  public static AbstractGroupMessage createStripeIDMismatchGroupMessage(int type, String reason, GroupID groupID) {
    return new StripeIDMismatchGroupMessage(StripeIDMismatchGroupMessage.ERROR_STRIPEID_MISMATCH, type, reason, groupID);
  }

}
