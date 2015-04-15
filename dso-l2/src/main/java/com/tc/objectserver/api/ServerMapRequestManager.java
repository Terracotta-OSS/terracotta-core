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
package com.tc.objectserver.api;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapRequestID;
import com.tc.objectserver.context.ServerMapGetAllSizeHelper;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.text.PrettyPrintable;

import java.util.Collection;

public interface ServerMapRequestManager extends PrettyPrintable {

  public void requestSize(ServerMapRequestID requestID, ClientID clientID, ObjectID mapID,
                          ServerMapGetAllSizeHelper helper);

  public void requestAllKeys(ServerMapRequestID requestID, ClientID clientID, ObjectID mapID);

  public void sendResponseFor(ObjectID mapID, ManagedObject managedObject);

  public void sendMissingObjectResponseFor(ObjectID mapID);

  public void requestValues(ClientID clientID, ObjectID mapID, Collection<ServerMapGetValueRequest> requests);

}
