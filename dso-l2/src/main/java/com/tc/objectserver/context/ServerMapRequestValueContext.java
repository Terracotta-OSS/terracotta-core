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
package com.tc.objectserver.context;

import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;

import java.util.Collection;

public class ServerMapRequestValueContext extends ServerMapRequestContext {

  private final Collection<ServerMapGetValueRequest> getValueRequests;

  public ServerMapRequestValueContext(final ClientID clientID, final ObjectID mapID,
                                      final Collection<ServerMapGetValueRequest> getValueRequests,
                                      final Sink destinationSink) {
    super(clientID, mapID, destinationSink);
    this.getValueRequests = getValueRequests;
  }

  public Collection<ServerMapGetValueRequest> getValueRequests() {
    return this.getValueRequests;
  }

  @Override
  public String toString() {
    return super.toString() + " [ value requests : " + this.getValueRequests + "]";
  }

  @Override
  public ServerMapRequestType getRequestType() {
    return ServerMapRequestType.GET_VALUE_FOR_KEY;
  }

  @Override
  public ServerMapRequestID getRequestID() {
    return ServerMapRequestID.NULL_ID;
  }
}
