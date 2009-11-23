/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.GroupID;
import com.tc.net.StripeID;
import com.tc.util.State;

public class StripeIDGroupMessageFactory {

  public static GroupMessage createStripeIDGroupMessage(GroupID groupID, StripeID stripeID, State senderState,
                                                        boolean isRemap) {
    return new StripeIDGroupMessage(StripeIDGroupMessage.STRIPEID_MESSAGE, groupID, stripeID, senderState, isRemap);
  }

}
