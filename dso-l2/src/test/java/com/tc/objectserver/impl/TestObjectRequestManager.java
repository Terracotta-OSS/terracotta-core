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
package com.tc.objectserver.impl;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.object.ObjectRequestServerContext.LOOKUP_STATE;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

public class TestObjectRequestManager implements ObjectRequestManager {

  public LinkedBlockingQueue<ObjectRequestServerContext> requestedObjects = new LinkedBlockingQueue<ObjectRequestServerContext>();

  @Override
  public void requestObjects(ObjectRequestServerContext requestContext) {
    try {
      this.requestedObjects.put(requestContext);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

  }

  @Override
  public void sendObjects(ClientID requestedNodeID, Collection objs, ObjectIDSet requestedObjectIDs,
                          ObjectIDSet missingObjectIDs, LOOKUP_STATE lookupState, int maxRequestDepth) {
    // not implemented
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out;
  }

  public int getCachedObjectCount() {
    return 0;
  }

  @Override
  public int getLiveObjectCount() {
    return 0;
  }

  @Override
  public Iterator getRootNames() {
    return null;
  }

  @Override
  public Iterator getRoots() {
    return null;
  }

  @Override
  public ObjectID lookupRootID(String name) {
    return null;
  }
}
