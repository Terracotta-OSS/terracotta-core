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
