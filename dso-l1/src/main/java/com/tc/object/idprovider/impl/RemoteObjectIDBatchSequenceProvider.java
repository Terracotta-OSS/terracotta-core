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
package com.tc.object.idprovider.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.msg.ObjectIDBatchRequestMessage;
import com.tc.object.msg.ObjectIDBatchRequestMessageFactory;
import com.tc.object.msg.ObjectIDBatchRequestResponseMessage;
import com.tc.util.Assert;
import com.tc.util.sequence.BatchSequenceProvider;
import com.tc.util.sequence.BatchSequenceReceiver;

/**
 * Manages object id requests to servers
 */
public class RemoteObjectIDBatchSequenceProvider extends AbstractEventHandler implements BatchSequenceProvider {
  private final ObjectIDBatchRequestMessageFactory mf;
  private volatile BatchSequenceReceiver           receiver;

  public RemoteObjectIDBatchSequenceProvider(ObjectIDBatchRequestMessageFactory mf) {
    this.mf = mf;
  }

  public void setBatchSequenceReceiver(BatchSequenceReceiver receiver) {
    this.receiver = receiver;
  }

  @Override
  public void requestBatch(BatchSequenceReceiver r, int size) {
    Assert.assertTrue(receiver == r);
    ObjectIDBatchRequestMessage m = mf.newObjectIDBatchRequestMessage();
    m.initialize(size);
    m.send();
  }

  @Override
  public void handleEvent(EventContext context) {
    ObjectIDBatchRequestResponseMessage m = (ObjectIDBatchRequestResponseMessage) context;
    receiver.setNextBatch(m.getBatchStart(), m.getBatchEnd());
  }

}