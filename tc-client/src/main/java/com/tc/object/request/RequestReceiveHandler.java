/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.object.request;

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
        case COMPLETED:
          VoltronEntityAppliedResponse appliedResponse = (VoltronEntityAppliedResponse) response;
          Exception failureException = appliedResponse.getFailureException();
          if (failureException != null) {
            this.handler.failed(transactionID, failureException);
          } else {
            this.handler.complete(transactionID, appliedResponse.getSuccessValue());
          }
          // always retire single use messages
          this.handler.retired(transactionID);
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
