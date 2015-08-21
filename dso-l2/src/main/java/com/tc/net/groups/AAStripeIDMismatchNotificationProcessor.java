/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;

public class AAStripeIDMismatchNotificationProcessor implements StripeIDMismatchNotificationProcessor {
  private static final TCLogger logger         = TCLogging.getLogger(AAStripeIDMismatchNotificationProcessor.class);
  private static final TCLogger CONSOLE_LOGGER = CustomerLogging.getConsoleLogger();

  @Override
  public boolean acceptOutgoingStripeIDMismatchNotification(NodeID fromNodeID, int errorType, String reason) {
    return true;
  }

  @Override
  public void incomingStripeIDMismatchNotification(NodeID fromNodeID, int errorType, String reason, GroupID groupID) {
    String errorMsg = "Received StripeID Mismatch Error from " + fromNodeID + " " + groupID + " type = " + errorType
                      + " reason = " + reason;
    logMessage(errorType, errorMsg);
  }

  private void logMessage(int errorType, String mesg) {
    switch (errorType) {
      case StripeIDMismatchGroupMessage.MISMATCH_TEMPORARY:
      case StripeIDMismatchGroupMessage.MISMATCH_NOT_READY_YET:
        // can be temporarily mismatched, log debug
        logger.debug(mesg);
        break;
      default:
        logger.error(mesg);
        CONSOLE_LOGGER.error(mesg);
        break;
    }
  }

}
