package com.terracotta.management.resource.services;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

@Ignore
public class AllEventsServiceTest {

  @Test
  public void allEventsTest() {
    Client client = ClientBuilder.newBuilder().register(SseFeature.class).build();
    WebTarget target = client.target("http://localhost:9540/tc-management-api/v2/agents/events/topologies");

    EventInput eventInput = target.request().get(EventInput.class);
    while (!eventInput.isClosed()) {
      final InboundEvent inboundEvent = eventInput.read();
      if (inboundEvent == null) {
        // connection has been closed
        break;
      }
      System.out.println(inboundEvent.getName() + "; " + inboundEvent.readData(String.class));
    }

  }

}
