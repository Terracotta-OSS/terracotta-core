/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.terracotta.management.security;

/**
 * @author brandony
 */
public interface RequestTicketMonitor {
  String issueRequestTicket();

  void redeemRequestTicket(String ticketId) throws InvalidRequestTicketException;
}
