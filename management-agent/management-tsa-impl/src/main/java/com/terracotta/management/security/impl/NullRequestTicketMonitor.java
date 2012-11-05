/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.terracotta.management.security.impl;

import com.terracotta.management.security.RequestTicketMonitor;

/**
 * @author Ludovic Orban
 */
public final class NullRequestTicketMonitor implements RequestTicketMonitor {
  @Override
  public String issueRequestTicket() {
    return null;
  }

  @Override
  public void redeemRequestTicket(String ticket) {
    //
  }
}
