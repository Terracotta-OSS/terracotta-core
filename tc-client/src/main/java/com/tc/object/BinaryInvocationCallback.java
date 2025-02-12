/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.object;

import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

public class BinaryInvocationCallback<R extends EntityResponse> implements SafeInvocationCallback<byte[]> {
  private final MessageCodec<?, R> codec;
  private final SafeInvocationCallback<R> callback;

  public BinaryInvocationCallback(MessageCodec<?, R> codec, SafeInvocationCallback<R> callback) {
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
