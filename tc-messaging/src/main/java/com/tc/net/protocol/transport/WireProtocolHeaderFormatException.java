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
package com.tc.net.protocol.transport;

import com.tc.net.protocol.TCProtocolException;

/**
 * Thrown to indicate a byte format/validation error with TC Wire protocol header blocks
 * 
 * @author teck
 */
public class WireProtocolHeaderFormatException extends TCProtocolException {

  public WireProtocolHeaderFormatException() {
    super();
  }

  public WireProtocolHeaderFormatException(Throwable cause) {
    super(cause);
  }

  public WireProtocolHeaderFormatException(String message, Throwable cause) {
    super(message, cause);
  }

  WireProtocolHeaderFormatException(String message) {
    super(message);
  }

}
