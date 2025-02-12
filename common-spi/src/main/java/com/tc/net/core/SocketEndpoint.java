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
package com.tc.net.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 */
public interface SocketEndpoint extends Closeable {
    
  ResultType writeFrom(ByteBuffer[] ref) throws IOException;
  
  ResultType readTo(ByteBuffer[] ref) throws IOException;

  enum ResultType {
    EOF,  // end of file
    ZERO, // zero bytes produced or consumed
    SUCCESS, // some bytes produced or consumed
    UNDERFLOW, // may occur on writeFrom, provide more data to be consumed
    OVERFLOW,  // may occur on readTo, provide more capacity to read into
  }
}
