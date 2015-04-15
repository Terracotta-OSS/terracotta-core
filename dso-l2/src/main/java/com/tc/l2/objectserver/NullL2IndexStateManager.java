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
package com.tc.l2.objectserver;

import com.tc.net.NodeID;
import com.tc.util.State;

public class NullL2IndexStateManager implements L2IndexStateManager {

  @Override
  public boolean addL2(NodeID nodeID, State currentState) {
    return true;
  }

  @Override
  public void registerForL2IndexStateChangeEvents(L2IndexStateListener listener) {
    //
  }

  @Override
  public void removeL2(NodeID nodeID) {
    //
  }

  @Override
  public void initiateIndexSync(NodeID nodeID) {
    //
  }

  @Override
  public void receivedAck(NodeID nodeID, int amount) {
    //
  }

}
