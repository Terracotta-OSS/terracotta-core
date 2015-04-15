/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.groups;

import com.tc.logging.TCLogger;
import com.tc.net.NodeID;

public class DefaultZapNodeRequestProcessor implements ZapNodeRequestProcessor {

  private final TCLogger logger;

  public DefaultZapNodeRequestProcessor(final TCLogger logger) {
    this.logger = logger;
  }

  @Override
  public boolean acceptOutgoingZapNodeRequest(NodeID nodeID, int type, String reason) {
    return true;
  }

  @Override
  public void incomingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason, long[] weights) {
    this.logger.fatal("DefaultZapNodeRequestProcessor : Received Zap Node request from " + nodeID + " type = "
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
