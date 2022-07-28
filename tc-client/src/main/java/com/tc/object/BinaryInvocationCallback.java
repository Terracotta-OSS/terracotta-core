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

import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvocationCallback;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

public class BinaryInvocationCallback<R extends EntityResponse> implements InvocationCallback<byte[]> {
  private final MessageCodec<?, R> codec;
  private final InvocationCallback<R> callback;

  public BinaryInvocationCallback(MessageCodec<?, R> codec, InvocationCallback<R> callback) {
    this.codec = codec;
    this.callback = callback;
  }

  @Override
  public void sent() {
    callback.sent();
  }

  @Override
  public void received() {
    callback.received();
  }

  @Override
  public void result(byte[] response) {
    try {
      callback.result(codec.decodeResponse(response));
    } catch (MessageCodecException e) {
      callback.failure(e);
    }
  }

  @Override
  public void failure(Throwable failure) {
    callback.failure(failure);
  }

  @Override
  public void complete() {
    callback.complete();
  }

  @Override
  public void retired() {
    callback.retired();
  }
}
