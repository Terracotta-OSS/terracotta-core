/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.master;

import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.terracotta.ipceventbus.event.Event;
import org.terracotta.ipceventbus.event.EventBus;
import org.terracotta.ipceventbus.event.EventListener;
import org.terracotta.testing.common.IPCMessageConstants;
import org.terracotta.testing.common.SimpleEventingStream;


public class ClientEventManager {
  private final SimpleEventingStream eventingStream;
  
  public ClientEventManager(final IMultiProcessControl control, PipedOutputStream inferiorProcessStdin, OutputStream dataSink) {
    final PrintStream processStdin = new PrintStream(inferiorProcessStdin);
    EventBus subBus = new EventBus.Builder().id("sub-bus").build();
    Map<String, String> eventMap = new HashMap<String, String>();
    
    // We might want to register one handler for all events, instead of handling the cases this way.
    String syncEventName = "Client Sync";
    eventMap.put(IPCMessageConstants.SYNC_SYN, syncEventName);
    subBus.on(syncEventName, new EventListener() {
      @Override
      public void onEvent(Event e) throws Throwable {
        control.synchronizeClient();
        // We also want to send the ACK to the client.
        processStdin.println(IPCMessageConstants.SYNC_ACK);
        processStdin.flush();
      }});
    
    String restartActiveEventName = "Restart active";
    eventMap.put(IPCMessageConstants.RESTART_ACTIVE_SYN, restartActiveEventName);
    subBus.on(restartActiveEventName, new EventListener() {
      @Override
      public void onEvent(Event e) throws Throwable {
        control.restartActive();
        // We also want to send the ACK to the client.
        processStdin.println(IPCMessageConstants.RESTART_ACTIVE_ACK);
        processStdin.flush();
      }});

    String terminateActiveEventName = "Terminate active";
    eventMap.put(IPCMessageConstants.TERMINATE_ACTIVE_SYN, terminateActiveEventName);
    subBus.on(terminateActiveEventName, new EventListener() {
      @Override
      public void onEvent(Event e) throws Throwable {
        control.terminateActive();
        // We also want to send the ACK to the client.
        processStdin.println(IPCMessageConstants.TERMINATE_ACTIVE_ACK);
        processStdin.flush();
      }});

    String startLastTerminatedServerEventName = "Start Last Terminated active";
    eventMap.put(IPCMessageConstants.START_LAST_TERMINATED_ACTIVE_SYN, startLastTerminatedServerEventName);
    subBus.on(startLastTerminatedServerEventName, new EventListener() {
      @Override
      public void onEvent(Event e) throws Throwable {
        control.startLastTerminatedServer();
        // We also want to send the ACK to the client.
        processStdin.println(IPCMessageConstants.START_LAST_TERMINATED_ACTIVE_ACK);
        processStdin.flush();
      }});
    
    String shutDownStripeEventName = "Shut down stripe";
    eventMap.put(IPCMessageConstants.SHUT_DOWN_STRIPE_SYN, shutDownStripeEventName);
    subBus.on(shutDownStripeEventName, new EventListener() {
      @Override
      public void onEvent(Event e) throws Throwable {
        control.shutDown();
        // We also want to send the ACK to the client.
        processStdin.println(IPCMessageConstants.SHUT_DOWN_STRIPE_ACK);
        processStdin.flush();
      }});
    
    String waitForActiveEventName = "Wait for active";
    eventMap.put(IPCMessageConstants.WAIT_FOR_ACTIVE_SYN, waitForActiveEventName);
    subBus.on(waitForActiveEventName, new EventListener() {
      @Override
      public void onEvent(Event e) throws Throwable {
        control.waitForActive();
        // We also want to send the ACK to the client.
        processStdin.println(IPCMessageConstants.WAIT_FOR_ACTIVE_ACK);
        processStdin.flush();
      }});
    
    String waitForPassiveEventName = "Wait for passive";
    eventMap.put(IPCMessageConstants.WAIT_FOR_PASSIVE_SYN, waitForPassiveEventName);
    subBus.on(waitForPassiveEventName, new EventListener() {
      @Override
      public void onEvent(Event e) throws Throwable {
        control.waitForPassive();
        // We also want to send the ACK to the client.
        processStdin.println(IPCMessageConstants.WAIT_FOR_PASSIVE_ACK);
        processStdin.flush();
      }});
    
    String shutDownClientEventName = "Client shut down";
    eventMap.put(IPCMessageConstants.CLIENT_SHUT_DOWN_SYN, shutDownClientEventName);
    subBus.on(shutDownClientEventName, new EventListener() {
      @Override
      public void onEvent(Event e) throws Throwable {
        // We want to ACK to the client so they know that they can shut down.
        processStdin.println(IPCMessageConstants.CLIENT_SHUT_DOWN_ACK);
        // Then we need to close the stream in order to avoid broken pipe exceptions.
        processStdin.close();
      }});
    
    this.eventingStream = new SimpleEventingStream(subBus, eventMap, dataSink);
  }
  
  /**
   * This is called to get the stream we want to use for the stdout of the inferior process so that it can hook into the event processing, here.
   */
  public OutputStream getEventingStream() {
    return this.eventingStream;
  }
}
