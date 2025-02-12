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
package com.tc.net.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.bytes.TCReference;
import com.tc.net.core.TCConnection;

public class NullProtocolAdaptor implements TCProtocolAdaptor {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public NullProtocolAdaptor() {
    super();
  }

  @Override
  public int getExpectedBytes() {
    return 32;
  }

  @Override
  public void addReadData(TCConnection source, TCReference data) {
    logger.warn("Null Protocol Adaptor isn't supposed to receive any data from the network.");
  }
}
