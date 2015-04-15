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
package com.tc.objectserver.l1.api;

import com.tc.net.NodeID;
import com.tc.object.ObjectID;

import java.util.Set;

public interface ClientState {

  public boolean addReference(ObjectID id);

  public boolean containsReference(ObjectID id);

  public void removeReferences(Set<ObjectID> references);

  public void addReferencedIdsTo(Set<ObjectID> ids);

  public NodeID getNodeID();

  public Set<ObjectID> getReferences();

}