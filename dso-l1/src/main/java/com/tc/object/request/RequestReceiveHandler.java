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

import org.terracotta.exception.EntityException;
import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityResponse;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;


public class RequestReceiveHandler extends AbstractEventHandler<VoltronEntityResponse> {
  private final RequestResponseHandler handler;

  public RequestReceiveHandler(RequestResponseHandler handler) {
    this.handler = handler;
  }

  @Override
  public void handleEvent(VoltronEntityResponse response) throws EventHandlerException {
    TransactionID transactionID = response.getTransactionID();
    if (!transactionID.isNull()) {
      switch (response.getAckType()) {
        case RETIRED:
          this.handler.retired(transactionID);
          break;
        case APPLIED:
          VoltronEntityAppliedResponse appliedResponse = (VoltronEntityAppliedResponse) response;
          EntityException failureException = appliedResponse.getFailureException();
          if (failureException != null) {
            this.handler.failed(transactionID, failureException);
          } else {
            this.handler.complete(transactionID, appliedResponse.getSuccessValue());
          }
          if (appliedResponse.alsoRetire()) {
            this.handler.retired(transactionID);
          }
          break;
        case RECEIVED:
          this.handler.received(transactionID);
          break;
        default:
          Assert.fail();
      }
    }
  }
}
