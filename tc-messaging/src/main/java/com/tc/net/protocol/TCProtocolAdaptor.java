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
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.core.TCConnection;
import java.util.Queue;

/**
 * Message adaptor/parser for incoming data from TCConnection
 * 
 * @author teck
 */
public interface TCProtocolAdaptor {
  default void addReadData(TCConnection source, TCByteBuffer data[], int length) throws TCProtocolException {
    addReadData(source, data, length, null);
  }

  public void addReadData(TCConnection source, TCByteBuffer data[], int length, Queue<TCByteBuffer> recycle) throws TCProtocolException;

  public TCByteBuffer[] getReadBuffers();
}

