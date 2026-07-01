/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.entity;

import com.tc.async.api.DirectExecutionMode;
import com.tc.async.api.Stage;
import com.tc.async.impl.MonitoringEventCreator;
import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityMultiResponse;
import com.tc.exception.ServerException;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.TCAction;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.StatType;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ResultCapture;
import com.tc.objectserver.api.StatisticsCapture;
import com.tc.objectserver.handler.ResponseMessage;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author
 */
  public class NetworkInvokeResponse implements ResultCapture, StatisticsCapture {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkInvokeResponse.class);

    private final Function<TCMessageType, TCAction> messageCreate;
    private final Stage<ResponseMessage> multiSend;
    private final ClientID clientID;
    private final ClientInstanceID instance;
    private final TransactionID  transaction;
    private Supplier<ActivePassiveAckWaiter> waiter;
    private final SetOnceFlag lastSent = new SetOnceFlag();
    private final boolean sendReceived;
    private final boolean sendStats;
    private final boolean holdResultForRetired;

    private byte[] heldResult;
    private final long[] stats = new long[StatType.SERVER_RETIRED.serverSpot() + 1];
    private final ConcurrentHashMap<ClientID, VoltronEntityMultiResponse> invokeReturn;

    private SetOnceFlag isRetired = new SetOnceFlag();

    public NetworkInvokeResponse(
            ClientID node,
            ClientInstanceID instance,
            TransactionID transaction,
            Function<TCMessageType, TCAction> mCreate,
            ConcurrentHashMap<ClientID, VoltronEntityMultiResponse> invokeReturn,
            Stage<ResponseMessage> messageSender,
            boolean sendReceived,
            boolean sendStats,
            boolean holdRetired) {
      this.clientID = node;
      this.instance = instance;
      this.transaction = transaction;
      this.messageCreate = mCreate;
      this.invokeReturn = invokeReturn;
      this.multiSend = messageSender;
      this.sendReceived = sendReceived;
      this.sendStats = sendStats;
      this.holdResultForRetired = holdRetired;
    }

    @Override
    public void received() {
      stats[StatType.SERVER_RECEIVED.serverSpot()] = System.nanoTime();
      if (sendReceived) {
        addSequentially(adder->adder.addReceived(transaction));
      }
    }

    @Override
    public void failure(ServerException cause) {
      stats[StatType.SERVER_COMPLETE.serverSpot()] = System.nanoTime();
      sendFailure(cause);
    }

    @Override
    public void complete(byte[] result) {
      stats[StatType.SERVER_COMPLETE.serverSpot()] = System.nanoTime();
      sendResponse(result);
    }

    @Override
    public void complete() {
      complete(new byte[0]);
    }

    @Override
    public void message(byte[] msg) {
      if (transaction.isNull()) {
        addSequentially(addTo->addTo.addServerMessage(instance, msg));
      } else {
        addSequentially(addTo->addTo.addServerMessage(transaction, msg));
      }
    }

    @Override
    public void setWaitFor(Supplier<ActivePassiveAckWaiter> waiter) {
      this.waiter = waiter;
    }

    private void sendResponse(byte[] result) {
      if (lastSent.attemptSet()) {
        if (!holdResultForRetired) {
          addSequentially(addTo->addTo.addResult(transaction, result));
        } else {
          heldResult = result;
        }
      }
    }

    private void sendFailure(ServerException e) {
      if (!lastSent.attemptSet()) {
        if (heldResult == null) {
          // no held result.  failure already sent
          return;
        } else {
          // clear held result
          heldResult = null;
        }
      }

      VoltronEntityAppliedResponse message = (VoltronEntityAppliedResponse)messageCreate.apply(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE);
      if (message != null) {
        message.setFailure(transaction, e);
        invokeReturn.compute(clientID, (c,v)-> {
          v.stopAdding();
          multiSend.getSink().addToSink(new ResponseMessage(message));
          return null;
        });
      }
      MonitoringEventCreator.finish();
    }

    @Override
    public CompletionStage<Void> retired() {
      if (!isRetired.attemptSet()) {
        throw new IllegalStateException("double retire");
      }
      CompletableFuture<Void> complete = new CompletableFuture<>();
      this.waiter.get().runWhenCompleted(()->{
        stats[StatType.SERVER_RETIRED.serverSpot()] = System.nanoTime();
        Assert.assertTrue(lastSent.isSet());
        addSequentially(addTo -> {
          if (heldResult != null) {
            return addTo.addResultAndRetire(transaction, heldResult);
          } else {
            return addTo.addRetired(transaction);
          }
        });
        MonitoringEventCreator.finish();
        complete.complete(null);
      });
      return complete;
    }

    @Override
    public void addMessage() {
      stats[StatType.SERVER_ADD.serverSpot()] = System.nanoTime();
    }

    @Override
    public void schedule() {
      stats[StatType.SERVER_SCHEDULE.serverSpot()] = System.nanoTime();
    }

    @Override
    public void beginInvoke() {
      stats[StatType.SERVER_BEGININVOKE.serverSpot()] = System.nanoTime();
    }

    @Override
    public void endInvoke() {
      stats[StatType.SERVER_ENDINVOKE.serverSpot()] = System.nanoTime();
    }

  private void addSequentially(Predicate<VoltronEntityMultiResponse> adder) {
    // don't bother if the client isNull, no where to send the message
    // if not, compute the result and schedule send if neccessary
    while (!clientID.isNull()) {
      // get the vmr.  most cases, will be present but if not create one
      VoltronEntityMultiResponse vmr = invokeReturn.computeIfAbsent(clientID, (client)-> {
        VoltronEntityMultiResponse msg = (VoltronEntityMultiResponse)messageCreate.apply(TCMessageType.VOLTRON_ENTITY_MULTI_RESPONSE);
        if (msg == null) {
          return null;
        }
        if (sendStats) {
          msg.addStats(transaction, stats);
        }
 //  use direct execution under map lock.  this makes sure there
 //  is only one for this client
        if (DirectExecutionMode.isActivated() && msg.shouldSend() && multiSend.isEmpty()) {
          msg.startAdding();
          Assert.assertTrue(adder.test(msg));
          msg.stopAdding();
          msg.send();
          return null;
        } else {
 // no direct execution, return the msg
          return msg;
        }
      });
      // enqueue if start adding returns true;  this means first to add
      boolean enqueue = vmr.startAdding();
      try {
        if (adder.test(vmr)) {
          // added the message, exit the loop
          break;
        }
      } finally {
        if (enqueue) {
          multiSend.getSink().addToSink(new ResponseMessage(vmr));
        }
      }
    }
  }
}