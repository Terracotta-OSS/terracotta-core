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

package com.tc.object.request;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.entity.VoltronEntityMultiResponse;
import com.tc.object.ClientInstanceID;
import com.tc.object.tx.TransactionID;


public class MultiRequestReceiveHandler extends AbstractEventHandler<VoltronEntityMultiResponse> {
  private final RequestResponseHandler handler;

  public MultiRequestReceiveHandler(RequestResponseHandler handler) {
    this.handler = handler;
  }

  @Override
  public void handleEvent(VoltronEntityMultiResponse response) throws EventHandlerException {
    response.replay(new VoltronEntityMultiResponse.ReplayReceiver() {
      @Override
      public void received(TransactionID tid) {
        handler.received(tid);
      }

      @Override
      public void retired(TransactionID tid) {
        handler.retired(tid);
      }

      @Override
      public void result(TransactionID tid, byte[] result) {
        if (result != null) {
          handler.complete(tid, result);
        } else {
          handler.complete(tid);
        }
      }

      @Override
      public void message(ClientInstanceID cid, byte[] message) {
        handler.handleMessage(cid, message);
      }

      @Override
      public void message(TransactionID tid, byte[] message) {
        handler.handleMessage(tid, message);
      }
    });
  }
}
