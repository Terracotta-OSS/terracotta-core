/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.terracotta.management.security.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.terracotta.management.security.InvalidRequestTicketException;
import com.terracotta.management.security.RequestTicketMonitor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author brandony
 */
public final class DfltRequestTicketMonitor implements RequestTicketMonitor {
  private static final Logger LOG = LoggerFactory.getLogger(DfltRequestTicketMonitor.class);

  private static final long TICKET_LIFETIME = TimeUnit.MINUTES.toMillis(1);

  private final LinkedList<RequestTicket> issuedTickets = new LinkedList<RequestTicket>();

  @Override
  public synchronized String issueRequestTicket() {
    cleanupOldsters();

    RequestTicket ticket = new RequestTicket();
    boolean added = issuedTickets.add(ticket);

    return added ? ticket.getTicketId().toString() : null;
  }

  @Override
  public synchronized void redeemRequestTicket(String ticket) throws InvalidRequestTicketException {
    RequestTicket found = null;

    for (Iterator<RequestTicket> itr = issuedTickets.iterator(); itr.hasNext(); ) {
      RequestTicket next = itr.next();
      if (ticket.equals(next.getTicketId().toString())) {
        itr.remove();
        found = next;
        break;
      }
    }

    if (found == null) {
      throw new InvalidRequestTicketException(String.format(
          "Unknown ticket cannot be redeemed. Either ticket '%s' has expired prior to this redemption attempt or this is a replay attack!",
          ticket));
    }
  }

  private void cleanupOldsters() {
    boolean removed;
    do {
      RequestTicket oldest = issuedTickets.peek();
      if (oldest != null && oldest.isExpired()) {
        if ((removed = issuedTickets.remove(oldest)) == true) {
          LOG.debug("RequestTicket '{}' has expired and has been removed from the issuedTickets queue.", oldest);
        }
      } else removed = false;
    } while (removed);
  }

  /**
   * @author brandony
   */
  private final class RequestTicket {
    private final UUID ticketId;

    private final long timestamp;

    private RequestTicket() {
      this.ticketId = UUID.randomUUID();
      this.timestamp = System.currentTimeMillis();
    }

    public UUID getTicketId() {
      return ticketId;
    }

    public boolean isExpired() {
      return timestamp + TICKET_LIFETIME < System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      RequestTicket that = (RequestTicket) o;

      if (timestamp != that.timestamp) return false;
      if (ticketId != null ? !ticketId.equals(that.ticketId) : that.ticketId != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = ticketId != null ? ticketId.hashCode() : 0;
      result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
      return result;
    }

    @Override
    public String toString() {
      return "RequestTicketImpl{" +
          "ticketId=" + ticketId +
          ", timestamp=" + timestamp +
          '}';
    }
  }
}
