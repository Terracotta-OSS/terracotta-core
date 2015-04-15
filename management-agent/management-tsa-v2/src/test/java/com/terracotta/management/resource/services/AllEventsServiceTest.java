/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
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
public class AllEventsServiceTest {

  @Test
  @Ignore
  public void allEventsTest() {
    Client client = ClientBuilder.newBuilder().register(SseFeature.class).build();
    WebTarget target = client.target("http://localhost:9888/tc-management-api/v2/events");

    EventInput eventInput = target.request().get(EventInput.class);
    while (!eventInput.isClosed()) {
      final InboundEvent inboundEvent = eventInput.read();
      if (inboundEvent == null) {
        // connection has been closed
        break;
      }
      // CacheManagerEntityEventV2 cacheManagerEntityEventV2 = inboundEvent.readData(CacheManagerEntityEventV2.class);
      //
      //
      // System.out.println(inboundEvent.getName() + " message was received from the L2 SSE Resource Service");
      // System.out.println("Here are the details :");
      // System.out.println("Type of the event : " + cacheManagerEntityEventV2.getType());
      //
      // System.out.println("CacheManager name : " + cacheManagerEntityEventV2.getCacheManagerName());
      // if(cacheManagerEntityEventV2.getCacheEntities() != null) {
      // System.out.println("Cache Attributes :");
      // for (Map<String, Object> cacheEntities : cacheManagerEntityEventV2.getCacheEntities()) {
      // for (Entry<String, Object> iterable_element : cacheEntities.entrySet()) {
      // System.out.println(iterable_element.getKey() + "  " + iterable_element.getValue());
      // }
      //
      // }
      // }
      // System.out.println("end of the event");


      String s = inboundEvent.readData();
      System.out.println("EVENT: " + s);


    }

  }

}
