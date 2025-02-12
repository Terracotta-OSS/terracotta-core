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
package com.tc.net.core.event;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkMessage;

/**
 * A special flavor of TCConnectionEvent indicating an error on a specific connection
 * 
 * @author teck
 */
public class TCConnectionErrorEvent extends TCConnectionEvent {

  private final Exception        exception;
  private final TCNetworkMessage context;

  public TCConnectionErrorEvent(TCConnection connection, Exception exception, TCNetworkMessage context) {
    super(connection);
    this.exception = exception;
    this.context = context;
  }

  /**
   * The exception thrown by an IO operation on this connection
   */
  public Exception getException() {
    return exception;
  }

  /**
   * If relevant, the message instance that was being used for the IO operation. Can be null
   */
  public TCNetworkMessage getMessageContext() {
    return context;
  }

  @Override
  public String toString() {
    return getSource() + ", exception: " + ((exception != null) ? exception.toString() : "[null exception]")
           + ", message context: " + ((context != null) ? context.toString() : "[no message context]");
  }
}
