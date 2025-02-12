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
package com.tc.l2.state;

import java.util.Collection;


public interface ConsistencyMBean {

  String CONSISTENCY_BEAN_NAME = "ConsistencyManager";

  /**
   * a server is stuck when a restricted action is requested and there are not
   * enough registered voters to grant access.  The only way to release such a
   * server is to give an override vote or connect all members of the stripe
   * @return true if the server is in the above state;
   */
  boolean isStuck();
  /**
   * a server is requesting a restricted action and is currently blocked from doing so while 
   * conducting a vote tally.
   * 
   * @return true if the server is blocked
   */
  boolean isBlocked();
  /**
   * the list of restricted actions that are currently restricted but blocked. 
   * 
   * @return the list of transitions requested.
   */
  Collection<String> requestedActions();

  /**
   * Forcibly allow a requested transition action
   */
  void allowRequestedTransition();
}
