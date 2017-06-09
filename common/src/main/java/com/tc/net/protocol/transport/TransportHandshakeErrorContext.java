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
package com.tc.net.protocol.transport;

public class TransportHandshakeErrorContext {
  private String    message;
  private TransportHandshakeError     errorType;
  private Throwable throwable;

  public TransportHandshakeErrorContext(String message) {
    this.message = message;
    this.errorType = TransportHandshakeError.ERROR_GENERIC;
  }

  public TransportHandshakeErrorContext(String message, Throwable throwable) {
    this(message);
    this.errorType = TransportHandshakeError.ERROR_GENERIC;
    this.throwable = throwable;
  }

  public TransportHandshakeErrorContext(String message, TransportHandshakeError errorType) {
    this(message);
    this.errorType = errorType;
  }

  public TransportHandshakeErrorContext(String message, short errorType) {
    this(message);
    this.errorType = TransportHandshakeError.values()[errorType];
  }
  
  public String getMessage() {
    return message;
  }
  
  public TransportHandshakeError getErrorType() {
    return errorType;
  }

  @Override
  public String toString() {
    StringBuffer rv = new StringBuffer(getClass().getName() + ": " + this.message);
    if (this.throwable != null) {
      rv.append(", throwable=" + throwable.getMessage());
    }
    return rv.toString();
  }
}
