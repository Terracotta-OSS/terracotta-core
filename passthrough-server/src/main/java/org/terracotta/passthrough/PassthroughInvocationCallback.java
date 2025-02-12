/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import org.terracotta.entity.InvocationCallback;

public class PassthroughInvocationCallback implements InvocationCallback<byte[]> {

  private final byte[] message;
  private final InvocationCallback<byte[]> callback;
  
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value="EI_EXPOSE_REP2")
  public PassthroughInvocationCallback(byte[] message, InvocationCallback<byte[]> callback) {
    this.message = message;
    this.callback = callback;
  }

  public void forceDisconnect() {

  }
  
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value="EI_EXPOSE_REP")
  public byte[] getMessage() {
    return message;
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
    callback.result(response);
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
