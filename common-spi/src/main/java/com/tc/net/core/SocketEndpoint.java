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
