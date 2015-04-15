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
package com.tc.object.session;

import com.tc.net.NodeID;

public class NullSessionManager implements SessionManager, SessionProvider {

  @Override
  public SessionID getSessionID(NodeID nid) {
    return SessionID.NULL_ID;
  }

  @Override
  public SessionID nextSessionID(NodeID nid) {
    return SessionID.NULL_ID;
  }

  @Override
  public void newSession(NodeID nid) {
    return;
  }

  @Override
  public boolean isCurrentSession(NodeID nid, SessionID sessionID) {
    return true;
  }

  @Override
  public void initProvider(NodeID nid) {
    return;
  }

  @Override
  public void resetSessionProvider() {
    return;
  }

}
