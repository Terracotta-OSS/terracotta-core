/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.GroupID;

public class StripeIDMismatchGroupMessageFactory {

  public static GroupMessage createStripeIDMismatchGroupMessage(int type, String reason, GroupID groupID) {
    return new StripeIDMismatchGroupMessage(StripeIDMismatchGroupMessage.ERROR_STRIPEID_MISMATCH, type, reason, groupID);
  }

}
