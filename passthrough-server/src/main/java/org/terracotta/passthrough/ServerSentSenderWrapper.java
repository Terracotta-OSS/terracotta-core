/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import java.util.function.Consumer;

/**
 *
 */
public class ServerSentSenderWrapper implements IMessageSenderWrapper {
  private final Consumer<PassthroughMessage> result;
  private final Consumer<PassthroughMessage> retire;
  private final PassthroughClientDescriptor sender;

  public ServerSentSenderWrapper(Consumer<PassthroughMessage> result, Consumer<PassthroughMessage> retire, PassthroughClientDescriptor sender) {
    this.result = result;
    this.retire = retire;
    this.sender = sender;
  }

  @Override
  public void sendAck(PassthroughMessage ack) {
    // Do nothing on ack.
  }
  @Override
  public void sendComplete(PassthroughMessage complete, boolean last) {
    if (result != null) {
      result.accept(complete);
    }
  }
  @Override
  public void sendRetire(PassthroughMessage retired) {
    retire.accept(retired);
  }
  @Override
  public PassthroughClientDescriptor clientDescriptorForID(long clientInstanceID) {
    return new PassthroughClientDescriptor(sender == null ? null : sender.server, null, clientInstanceID);
  }
  @Override
  public long getClientOriginID() {
    return -1L;
  }
}
