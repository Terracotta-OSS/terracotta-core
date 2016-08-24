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
import com.tc.object.tx.TransactionID;
import java.util.Map;


public class MultiRequestReceiveHandler extends AbstractEventHandler<VoltronEntityMultiResponse> {
  private final RequestResponseHandler handler;

  public MultiRequestReceiveHandler(RequestResponseHandler handler) {
    this.handler = handler;
  }

  @Override
  public void handleEvent(VoltronEntityMultiResponse response) throws EventHandlerException {
    for (TransactionID received : response.getReceivedTransactions()) {
      handler.received(received);
    }
    Map<TransactionID, byte[]> results = response.getResults();
    for (Map.Entry<TransactionID, byte[]> entry : results.entrySet()) {
      byte[] result = entry.getValue();
      if (result == null) {
        handler.complete(entry.getKey());
      } else {
        handler.complete(entry.getKey(), result);
      }
    }
    for (TransactionID retires : response.getRetiredTransactions()) {
      handler.retired(retires);
    }
  }
}
