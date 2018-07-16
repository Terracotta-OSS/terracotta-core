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
package com.tc.l2.state;

import com.tc.l2.state.ConsistencyManager.Transition;
import java.util.Collection;


public interface ConsistencyMBean {
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
  Collection<Transition> requestedActions();
}
