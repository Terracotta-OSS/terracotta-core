/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services.events;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceLocator;

import com.terracotta.management.resource.events.TopologyEventEntityV2;
import com.terracotta.management.service.events.TopologyEventServiceV2;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for sending topology events.
 * 
 * @author Ludovic Orban
 */
@Path("/v2/agents/events/topologies")
public class TopologyEventResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(TopologyEventResourceServiceImplV2.class);

  private final TopologyEventServiceV2 topologyEventService;

  public TopologyEventResourceServiceImplV2() {
    this.topologyEventService = ServiceLocator.locate(TopologyEventServiceV2.class);
  }

  @GET
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  public EventOutput getServerSentEvents(@Context UriInfo info) {
    LOG.debug(String.format("Invoking TopologyEventResourceServiceImplV2.getServerSentEvents: %s", info.getRequestUri()));

    final EventOutput eventOutput = new EventOutput();
    topologyEventService.registerTopologyEventListener(new TopologyEventServiceV2.TopologyEventListener() {
      @Override
      public void onEvent(TopologyEventEntityV2 eventEntity) {
        OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
        eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE);
        eventBuilder.name(TopologyEventEntityV2.class.getSimpleName());
        eventBuilder.data(TopologyEventEntityV2.class, eventEntity);
        OutboundEvent event = eventBuilder.build();

        try {
          eventOutput.write(event);
        } catch (IOException e) {
          topologyEventService.unregisterTopologyEventListener(this);
          try {
            eventOutput.close();
          } catch (IOException ioe) {
            LOG.warn("Error when closing the event output.", ioe);
          }
        }
      }
    });

    return eventOutput;
  }

}
