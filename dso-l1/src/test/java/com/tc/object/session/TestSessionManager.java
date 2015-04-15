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

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;


public class TestSessionManager implements SessionManager, SessionProvider {

  public boolean isCurrentSession = true;
  public SessionID sessionID = SessionID.NULL_ID;
  
  @Override
  public SessionID getSessionID(NodeID nid) {
    return sessionID;
  }
  
  @Override
  public SessionID nextSessionID(NodeID nid) {
    throw new ImplementMe();
  }

  @Override
  public void newSession(NodeID nid) {
    return;
  }

  @Override
  public boolean isCurrentSession(NodeID nid, SessionID theSessionID) {
    return isCurrentSession;
  }

  @Override
  public void initProvider(NodeID nid) {
    return;
  }

  @Override
  public void resetSessionProvider() {
    throw new ImplementMe();
  }

}
