/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.GroupID;
import com.tc.net.StripeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.StripeIDGroupMessage;

public class StripeIDGroupMessageFactory {

  public static AbstractGroupMessage createStripeIDGroupMessage(GroupID groupID, StripeID stripeID, boolean isActive,
                                                        boolean isRemap) {
    return new StripeIDGroupMessage(StripeIDGroupMessage.STRIPEID_MESSAGE, groupID, stripeID, isActive, isRemap);
  }

}
