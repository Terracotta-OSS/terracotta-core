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

import java.util.function.Consumer;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvokeMonitor;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;



public class InFlightMonitor<R extends EntityResponse> implements Consumer<byte[]>, AutoCloseable {
  private final InvokeMonitor<R> monitor;
  private final MessageCodec<?, R> codec;

  public InFlightMonitor(MessageCodec<?, R> codec, InvokeMonitor<R> monitor) {
    this.monitor = monitor;
    this.codec = codec;
  }

  @Override
  public void accept(byte[] t) {
    try {
      monitor.accept(codec.decodeResponse(t));
    } catch (MessageCodecException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void close() {
    monitor.close();
  }
}
