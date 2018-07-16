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
package com.tc.object;

import com.tc.entity.VoltronEntityMessage.Acks;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvokeMonitor;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;



public class InFlightMonitor<R extends EntityResponse> implements Consumer<byte[]>, AckMonitor, AutoCloseable {
  private final InvokeMonitor<R> monitor;
  private final MessageCodec<?, R> codec;
  private final Executor executor;

  public InFlightMonitor(MessageCodec<?, R> codec, InvokeMonitor<R> monitor, Executor executor) {
    this.monitor = monitor == null ? (r)->{} : monitor;
    this.codec = codec;
    this.executor = executor;
  }

  @Override
  public void accept(byte[] t) {
    try {
      deliverMessage(codec.decodeResponse(t));
    } catch (MessageCodecException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  private void deliverMessage(R er) {
    if (executor != null) {
      executor.execute(()->monitor.accept(er));
    } else {
      monitor.accept(er);
    }
  }

  @Override
  public void ackDelivered(Acks ack) {
    if (monitor instanceof AckMonitor) {
      ((AckMonitor)monitor).ackDelivered(ack);
    }
  }

  @Override
  public void close() {
    monitor.close();
  }
}
