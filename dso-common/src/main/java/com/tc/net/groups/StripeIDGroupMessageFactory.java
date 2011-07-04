/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.GroupID;
import com.tc.net.StripeID;

public class StripeIDGroupMessageFactory {

  public static GroupMessage createStripeIDGroupMessage(GroupID groupID, StripeID stripeID, boolean isActive,
                                                        boolean isRemap) {
    return new StripeIDGroupMessage(StripeIDGroupMessage.STRIPEID_MESSAGE, groupID, stripeID, isActive, isRemap);
  }

}
