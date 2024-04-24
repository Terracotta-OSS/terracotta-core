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
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Stage;
import com.tc.exception.ServerException;
import com.tc.exception.TCServerRestartException;
import com.tc.l2.dup.RelayMessage;
import com.tc.objectserver.entity.MessagePayload;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.util.Assert;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DuplicationTransactionHandler {
  private static final Logger PLOGGER = LoggerFactory.getLogger(MessagePayload.class);
  private static final Logger LOGGER = LoggerFactory.getLogger(DuplicationTransactionHandler.class);
  

  private final GroupManager<AbstractGroupMessage> groupManager;
  private final GroupEventsListener listener;
  private volatile long currentSequence = 0L;
  
  public DuplicationTransactionHandler(StateManager stateMgr, Predicate<NodeID> duplicator, GroupManager<AbstractGroupMessage> groupManager) {
    this.groupManager = groupManager;
    this.listener = new GroupEventsListener() {
      @Override
      public void nodeJoined(NodeID nodeID) {
        try {
          if (duplicator.test(nodeID)) {
            switch (stateMgr.getCurrentMode()) {
              case RELAY:
                TCLogging.getConsoleLogger().info("requesting duplication sync");
                stateMgr.moveToPassiveSyncing(nodeID);
                groupManager.sendTo(nodeID, RelayMessage.createStartSync());
                break;
              case PASSIVE:
                groupManager.sendTo(nodeID, RelayMessage.createResumeMessage(currentSequence));
                break;
              default:
                throw new TCServerRestartException("invalid state for duplication");
            }
          } else {
            throw new TCServerRestartException("resyncing duplicate");
          }
        } catch (GroupException ge) {

        }
      }

      @Override
      public void nodeLeft(NodeID nodeID) {

      }
    };
  }

  private final EventHandler<RelayMessage> eventHandler = new AbstractEventHandler<RelayMessage>() {
   private Stage<ReplicationMessage> sendToNext;
   
   @Override
    public void handleEvent(RelayMessage message) throws EventHandlerException {
     switch (message.getType()) {
       case RelayMessage.RELAY_INVALID:
         throw new TCServerRestartException("duplicate server is no longer in sync with relay");
       case RelayMessage.RELAY_SUCCESS:
         TCLogging.getConsoleLogger().info("relay resume is successful");
         break;
       default:
         try {
           processMessage(sendToNext, message);
         } catch (Throwable t) {
           // We don't expect to see an exception executing a replicated message.
           // TODO:  Find a better way to handle this error.
           throw Assert.failure("Unexpected exception executing replicated message", t);
         }break;
     }
    }

    @Override
    protected void initialize(ConfigurationContext context) {
      super.initialize(context);
      sendToNext = context.getStage(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE, ReplicationMessage.class);
      groupManager.registerForGroupEvents(listener);
    }

    @Override
    public void destroy() {
      super.destroy();
      groupManager.unregisterForGroupEvents(listener);
    }
    
    
  };

  public EventHandler<RelayMessage> getEventHandler() {
    return eventHandler;
  }
  
  private void processMessage(Stage<ReplicationMessage> sendToNext, RelayMessage rep) throws ServerException {
    if (rep.getType() == RelayMessage.RELAY_BATCH) {
      long last = rep.unwindBatch(m->sendToNext.getSink().addToSink(m));
      currentSequence = Long.max(currentSequence, last);
    } else {
      throw new UnsupportedOperationException("relay message:" + rep);
    }
  }
}
