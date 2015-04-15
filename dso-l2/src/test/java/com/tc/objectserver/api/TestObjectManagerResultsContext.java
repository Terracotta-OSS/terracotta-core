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

import com.tc.object.ObjectID;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.util.Map;

public class TestObjectManagerResultsContext implements ObjectManagerResultsContext {

  private final Map<ObjectID, ManagedObject> results;
  private final ObjectIDSet objectIDs;

  public TestObjectManagerResultsContext(Map<ObjectID, ManagedObject> results, ObjectIDSet objectIDs) {
    this.results = results;
    this.objectIDs = objectIDs;
  }

  public Map getResults() {
    return results;
  }

  @Override
  public void setResults(ObjectManagerLookupResults results) {
    this.results.putAll(results.getObjects());
    if (!results.getMissingObjectIDs().isEmpty()) { throw new AssertionError("Missing Objects : "
                                                                             + results.getMissingObjectIDs()); }
  }

  @Override
  public ObjectIDSet getLookupIDs() {
    return objectIDs;
  }

  @Override
  public ObjectIDSet getNewObjectIDs() {
    return new BitSetObjectIDSet();
  }

  public boolean updateStats() {
    return true;
  }

}