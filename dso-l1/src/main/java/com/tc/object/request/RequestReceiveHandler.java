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
    switch (response.getAckType()) {
      case APPLIED:
        VoltronEntityAppliedResponse appliedResponse = (VoltronEntityAppliedResponse) response;
        EntityException failureException = appliedResponse.getFailureException();
        if (failureException != null) {
          this.handler.failed(transactionID, failureException);
        } else {
          this.handler.complete(transactionID, appliedResponse.getSuccessValue());
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
