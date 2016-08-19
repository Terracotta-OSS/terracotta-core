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
    installEventHandler(subBus, eventMap, control, IPCMessageConstants.SYNC, new ControlCaller() {
      @Override
      public void runWithControl(IMultiProcessControl control) throws Throwable {
        control.synchronizeClient();
      }}, processStdin);
    
    installEventHandler(subBus, eventMap, control, IPCMessageConstants.TERMINATE_ACTIVE, new ControlCaller() {
      @Override
      public void runWithControl(IMultiProcessControl control) throws Throwable {
        control.terminateActive();
      }}, processStdin);
    
    installEventHandler(subBus, eventMap, control, IPCMessageConstants.TERMINATE_ONE_PASSIVE, new ControlCaller() {
      @Override
      public void runWithControl(IMultiProcessControl control) throws Throwable {
        control.terminateOnePassive();
      }}, processStdin);
    
    installEventHandler(subBus, eventMap, control, IPCMessageConstants.START_ONE_SERVER, new ControlCaller() {
      @Override
      public void runWithControl(IMultiProcessControl control) throws Throwable {
        control.startOneServer();
      }}, processStdin);
    
    installEventHandler(subBus, eventMap, control, IPCMessageConstants.START_ALL_SERVERS, new ControlCaller() {
      @Override
      public void runWithControl(IMultiProcessControl control) throws Throwable {
        control.startAllServers();
      }}, processStdin);
    
    installEventHandler(subBus, eventMap, control, IPCMessageConstants.SHUT_DOWN_STRIPE, new ControlCaller() {
      @Override
      public void runWithControl(IMultiProcessControl control) throws Throwable {
        control.terminateAllServers();
      }}, processStdin);
    
    installEventHandler(subBus, eventMap, control, IPCMessageConstants.WAIT_FOR_ACTIVE, new ControlCaller() {
      @Override
      public void runWithControl(IMultiProcessControl control) throws Throwable {
        control.waitForActive();
      }}, processStdin);
    
    installEventHandler(subBus, eventMap, control, IPCMessageConstants.WAIT_FOR_PASSIVE, new ControlCaller() {
      @Override
      public void runWithControl(IMultiProcessControl control) throws Throwable {
        control.waitForRunningPassivesInStandby();
      }}, processStdin);
    
    installEventHandler(subBus, eventMap, control, IPCMessageConstants.CLIENT_SHUT_DOWN, new ControlCaller() {
      @Override
      public void runWithControl(IMultiProcessControl control) throws Throwable {
        // We do nothing - just acknowledge this.
      }}, processStdin);
    
    this.eventingStream = new SimpleEventingStream(subBus, eventMap, dataSink);
  }
  
  /**
   * This is called to get the stream we want to use for the stdout of the inferior process so that it can hook into the event processing, here.
   */
  public OutputStream getEventingStream() {
    return this.eventingStream;
  }


  private void installEventHandler(EventBus subBus, Map<String, String> eventMap, final IMultiProcessControl control, String eventNameBase, ControlCaller call, final PrintStream processStdin) {
    eventMap.put(IPCMessageConstants.synFrom(eventNameBase), eventNameBase);
    subBus.on(eventNameBase, new EventListener() {
      @Override
      public void onEvent(Event e) throws Throwable {
        String responseToSend = IPCMessageConstants.ackFrom(eventNameBase);
        call.runWithControl(control);
        
        // We also want to send the ACK to the client.
        processStdin.println(responseToSend);
        processStdin.flush();
      }});
  }

  private static interface ControlCaller {
    void runWithControl(IMultiProcessControl control) throws Throwable;
  }
}
