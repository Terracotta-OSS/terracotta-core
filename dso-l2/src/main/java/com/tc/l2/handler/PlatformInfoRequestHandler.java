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

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.l2.msg.PlatformInfoRequest;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.MessageID;
import com.tc.object.config.schema.L2Config;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.server.TCServerMain;
import com.tc.util.ProductInfo;
import com.tc.util.State;


public class PlatformInfoRequestHandler {
  
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final ITopologyEventCollector remoteEvents;

  public PlatformInfoRequestHandler(GroupManager<AbstractGroupMessage> groupManager, ITopologyEventCollector remoteEvents) {
    this.groupManager = groupManager;
    this.remoteEvents = remoteEvents;
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
            case PlatformInfoRequest.SERVER_INFO:
              handleServerInfo(context);
              break;
            case PlatformInfoRequest.SERVER_STATE:
              handleServerState(context);
              break;
            default:
              break;
          }
        } catch (GroupException g) {
// Ignore
        }
      }
    };
  }
  
  private void handleRequestEvent(PlatformInfoRequest context) throws GroupException {
    switch(context.getRequestType()) {
      case SERVER_INFO:
        collectAndSendServerInfo(context.messageFrom(), context.getMessageID());
        collectAndSendStateInfo(context.messageFrom(), context.getMessageID());
        break;
      default:
    }
  }
  
  private void handleServerInfo(PlatformInfoRequest msg) throws GroupException {
    try {
      L2Config config = TCServerMain.getSetupManager().dsoL2ConfigFor(msg.getName());
      String bindAddress = config.tsaPort().getBind();
      if (bindAddress == null) {
        // workaround for CDV-584
        bindAddress = TCSocketAddress.WILDCARD_IP;
      }
      remoteEvents.serverDidJoinGroup((ServerID)msg.messageFrom(), msg.getName(), config.host(), bindAddress, 
          config.tsaPort().getValue(), config.tsaGroupPort().getValue(), msg.getVersion(), msg.getBuild());
    } catch (ConfigurationSetupException set) {

    }
  }  
  
  private void handleServerState(PlatformInfoRequest msg) throws GroupException {
    remoteEvents.serverDidEnterState((ServerID)msg.messageFrom(), new State(msg.getState()), msg.getActivateTime());
  }  
  
  private void collectAndSendServerInfo(NodeID dest, MessageID msg) throws GroupException {
    String name = TCServerMain.getServer().getL2Identifier();
    String version = ProductInfo.getInstance().version();
    String build = ProductInfo.getInstance().buildID();
    groupManager.sendTo(dest, new PlatformInfoRequest(name, version, build, TCServerMain.getServer().getStartTime(), msg));
  }
  
  
  private void collectAndSendStateInfo(NodeID dest, MessageID msg) throws GroupException {
    groupManager.sendTo(dest, new PlatformInfoRequest(TCServerMain.getServer().getState().getName(), TCServerMain.getServer().getActivateTime(), msg));
  }  
}
