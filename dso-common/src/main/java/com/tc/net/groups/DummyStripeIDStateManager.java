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

import com.tc.net.GroupID;
import com.tc.net.StripeID;

import java.util.HashMap;
import java.util.Map;

public class DummyStripeIDStateManager implements StripeIDStateManager {

  @Override
  public StripeID getStripeID(GroupID gid) {
    return StripeID.NULL_ID;
  }

  @Override
  public Map<GroupID, StripeID> getStripeIDMap(boolean fromAACoordinator) {
    return new HashMap();
  }

  @Override
  public boolean isStripeIDMatched(GroupID gid, StripeID stripeID) {
    return true;
  }

  @Override
  public void registerForStripeIDEvents(StripeIDEventListener listener) {
    // NOP
  }

  @Override
  public boolean verifyOrSaveStripeID(GroupID gid, StripeID stripeID, boolean overwrite) {
    return true;
  }

}
