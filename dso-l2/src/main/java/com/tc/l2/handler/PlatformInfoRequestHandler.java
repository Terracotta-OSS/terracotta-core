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
package com.tc.l2.handler;

import java.io.Serializable;

import org.terracotta.monitoring.PlatformServer;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.l2.msg.PlatformInfoRequest;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.server.TCServerMain;
import com.tc.services.LocalMonitoringProducer;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;


public class PlatformInfoRequestHandler {
  private static final TCLogger LOGGER = TCLogging.getLogger(PlatformInfoRequestHandler.class);

  private final GroupManager<AbstractGroupMessage> groupManager;
  private final LocalMonitoringProducer monitoringSupport;

  public PlatformInfoRequestHandler(GroupManager<AbstractGroupMessage> groupManager, LocalMonitoringProducer monitoringSupport) {
    Assert.assertNotNull(groupManager);
    Assert.assertNotNull(monitoringSupport);
    this.groupManager = groupManager;
    this.monitoringSupport = monitoringSupport;
  }

  public EventHandler<PlatformInfoRequest> getEventHandler() {
    return new AbstractEventHandler<PlatformInfoRequest>() {
      @Override
      public void handleEvent(PlatformInfoRequest context) throws EventHandlerException {
        try {
          switch (context.getType()) {
            case PlatformInfoRequest.REQUEST:
              handleRequestEvent(context);
              break;
            case PlatformInfoRequest.RESPONSE_INFO:
              handleServerInfo(context);
              break;
            case PlatformInfoRequest.RESPONSE_ADD:
              PlatformInfoRequestHandler.this.monitoringSupport.handleRemoteAdd((ServerID)context.messageFrom(), context.getConsumerID(), context.getParents(), context.getNodeName(), context.getNodeValue());
              break;
            case PlatformInfoRequest.RESPONSE_REMOVE:
              PlatformInfoRequestHandler.this.monitoringSupport.handleRemoteRemove((ServerID)context.messageFrom(), context.getConsumerID(), context.getParents(), context.getNodeName());
              break;
            default:
              break;
          }
        } catch (GroupException g) {
          // If there is something wrong in sending the monitoring data, this isn't critical so just log the error.
          LOGGER.error(g.getLocalizedMessage());
        }
      }
    };
  }


  private void handleRequestEvent(PlatformInfoRequest context) throws GroupException {
    NodeID requester = context.messageFrom();
    
    // First, we want to send the server info so that the other side knows who we are.
    PlatformInfoRequest serverInfo = PlatformInfoRequest.createServerInfoMessage(this.monitoringSupport.getLocalServerInfo());
    this.groupManager.sendTo(requester, serverInfo);
    
    // Now, send all the data in the cache.
    LocalMonitoringProducer.ActivePipeWrapper pipeWrapper = new LocalMonitoringProducer.ActivePipeWrapper() {
      @Override
      public void addNode(long consumerID, String[] parents, String name, Serializable value) {
        PlatformInfoRequest message = PlatformInfoRequest.createAddNode(consumerID, parents, name, value);
        try {
          groupManager.sendTo(requester, message);
        } catch (GroupException e) {
          // If there is something wrong in sending the monitoring data, this isn't critical so just log the error.
          LOGGER.error(e.getLocalizedMessage());
        }
      }
      @Override
      public void removeNode(long consumerID, String[] parents, String name) {
        PlatformInfoRequest message = PlatformInfoRequest.createRemoveNode(consumerID, parents, name);
        try {
          groupManager.sendTo(requester, message);
        } catch (GroupException e) {
          // If there is something wrong in sending the monitoring data, this isn't critical so just log the error.
          LOGGER.error(e.getLocalizedMessage());
        }
      }
    };
    this.monitoringSupport.sendToNewActive(pipeWrapper);
  }

  private void handleServerInfo(PlatformInfoRequest msg) throws GroupException {
    ServerID sender = (ServerID)msg.messageFrom();
    PlatformServer platformServer = msg.getServerInfo();
    this.monitoringSupport.serverDidJoinStripe(sender, platformServer);
  }
}
