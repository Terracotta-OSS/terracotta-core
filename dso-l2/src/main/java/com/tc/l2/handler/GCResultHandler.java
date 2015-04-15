/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.msg.GCResultMessage;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;

public class GCResultHandler extends AbstractEventHandler {

  private ReplicatedObjectManager  rObjectManager;
  private ServerTransactionManager transactionManager;
  private Sink                     gcResultSink;

  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof GCResultMessage) {
      GCResultMessage msg = (GCResultMessage) context;
      transactionManager.callBackOnTxnsInSystemCompletion(new GCResultCallback(msg, gcResultSink));
    } else {
      GCResultCallback callback = (GCResultCallback) context;
      rObjectManager.handleGCResult(callback.getGCResult());
    }
  }

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.transactionManager = oscc.getTransactionManager();
    this.rObjectManager = oscc.getL2Coordinator().getReplicatedObjectManager();
    this.gcResultSink = oscc.getStage(ServerConfigurationContext.GC_RESULT_PROCESSING_STAGE).getSink();
  }

  private static final class GCResultCallback implements TxnsInSystemCompletionListener, EventContext {

    private final Sink            sink;
    private final GCResultMessage msg;

    public GCResultCallback(GCResultMessage msg, Sink sink) {
      this.msg = msg;
      this.sink = sink;
    }

    public GCResultMessage getGCResult() {
      return msg;
    }

    @Override
    public void onCompletion() {
      sink.add(this);
    }

    @Override
    public String toString() {
      return "GCResultCallback{" +
             "msg=" + msg +
             '}';
    }
  }
}
