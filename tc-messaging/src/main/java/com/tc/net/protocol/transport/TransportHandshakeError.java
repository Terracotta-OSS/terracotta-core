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
package com.tc.net.protocol.transport;

public enum TransportHandshakeError {
  /**
   * These are the types of error that may happen at the time of handshake
   */
  ERROR_NONE, ERROR_HANDSHAKE, ERROR_INVALID_CONNECTION_ID, ERROR_STACK_MISMATCH,ERROR_GENERIC,
  ERROR_MAX_CONNECTION_EXCEED, ERROR_RECONNECTION_REJECTED, ERROR_REDIRECT_CONNECTION, ERROR_NO_ACTIVE,
  ERROR_PRODUCT_NOT_SUPPORTED;

  public String getMessage() {
    return name();
  }

  public short getErrorType() {
    return (short)ordinal();
  }
}
