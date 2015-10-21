/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
