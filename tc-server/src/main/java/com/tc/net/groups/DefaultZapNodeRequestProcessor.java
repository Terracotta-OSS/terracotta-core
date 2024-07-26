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
package com.tc.net.groups;

import org.slf4j.Logger;

import com.tc.net.NodeID;

public class DefaultZapNodeRequestProcessor implements ZapNodeRequestProcessor {

  private final Logger logger;

  public DefaultZapNodeRequestProcessor(Logger logger) {
    this.logger = logger;
  }

  @Override
  public boolean acceptOutgoingZapNodeRequest(NodeID nodeID, int type, String reason) {
    return true;
  }

  @Override
  public void incomingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason, long[] weights) {
    this.logger.error("DefaultZapNodeRequestProcessor : Received Zap Node request from " + nodeID + " type = "
                      + zapNodeType + " reason = " + reason);

    System.exit(zapNodeType);
  }

  @Override
  public long[] getCurrentNodeWeights() {
    return new long[0];
  }

  @Override
  public void addZapEventListener(ZapEventListener listener) {
    //
  }

}
