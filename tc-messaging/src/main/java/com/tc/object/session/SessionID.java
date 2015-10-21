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
package com.tc.object.session;

import com.tc.util.AbstractIdentifier;

/**
 * Session identifier
 */
public class SessionID extends AbstractIdentifier {

  /**
   * Represents no session (id=-1)
   */
  public static final SessionID NULL_ID = new SessionID(-1);
  
  /**
   * Create new session with specified identifier
   * @param id Id value
   */
  public SessionID(long id) {
    super(id);
  }

  @Override
  public String getIdentifierType() {
    return "SessionID";
  }

}
