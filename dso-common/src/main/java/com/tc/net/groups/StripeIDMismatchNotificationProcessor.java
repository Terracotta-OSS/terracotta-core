/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.GroupID;
import com.tc.net.NodeID;

public interface StripeIDMismatchNotificationProcessor {

  public boolean acceptOutgoingStripeIDMismatchNotification(NodeID fromNodeID, int errorType, String reason);

  public void incomingStripeIDMismatchNotification(NodeID fromNodeID, int errorType, String reason, GroupID groupID);

}
