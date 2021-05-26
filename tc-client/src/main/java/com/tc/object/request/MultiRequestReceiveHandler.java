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
import com.tc.text.PrettyPrintable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;


public class MultiRequestReceiveHandler extends AbstractEventHandler<VoltronEntityMultiResponse> implements PrettyPrintable {
  private final RequestResponseHandler handler;
  private final LongAdder opCount = new LongAdder();
  private final LongAdder msgCount = new LongAdder();

  public MultiRequestReceiveHandler(RequestResponseHandler handler) {
    this.handler = handler;
  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("opCount", opCount.longValue());
    map.put("msgCount", msgCount.longValue());
    return map;
  }

  @Override
  public void handleEvent(VoltronEntityMultiResponse response) throws EventHandlerException {
    msgCount.increment();
    response.replay(new VoltronEntityMultiResponse.ReplayReceiver() {
      @Override
      public void received(TransactionID tid) {
        opCount.increment();
        handler.received(tid);
      }

      @Override
      public void retired(TransactionID tid) {
        opCount.increment();
        handler.retired(tid);
      }

      @Override
      public void result(TransactionID tid, byte[] result) {
        opCount.increment();
        if (result != null) {
          handler.complete(tid, result);
        } else {
          handler.complete(tid);
        }
      }

      @Override
      public void message(ClientInstanceID cid, byte[] message) {
        opCount.increment();
        handler.handleMessage(cid, message);
      }

      @Override
      public void message(TransactionID tid, byte[] message) {
        opCount.increment();
        handler.handleMessage(tid, message);
      }

      @Override
      public void stats(TransactionID tid, long[] message) {
        opCount.increment();
        handler.handleStatistics(tid, message);
      }
    });
  }
}
