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
package com.tc.objectserver.core.api;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;

/**
 * @author steve
 */
public class TestDNA implements DNA {
  public DNACursor cursor;
  public ObjectID  objectID;
  public long      version;
  public String    typeName       = "com.terracotta.toolkit.roots.impl.ToolkitTypeRootImpl";
  public ObjectID  parentObjectID = ObjectID.NULL_ID;
  public boolean   isDelta;

  public TestDNA(DNACursor cursor) {
    this.cursor = cursor;
  }

  public TestDNA(DNACursor cursor, String className) {
    this.cursor = cursor;
    this.typeName = className;
  }

  public TestDNA(ObjectID oid) {
    this.objectID = oid;

  }

  public TestDNA(ObjectID id, boolean isDelta) {
    this.objectID = id;
    this.isDelta = isDelta;
  }

  /*
   * public void setObject(TCObject object) throws DNAException { return; }
   */

  @Override
  public String getTypeName() {
    return typeName;
  }

  @Override
  public ObjectID getObjectID() throws DNAException {
    return objectID;
  }

  @Override
  public DNACursor getCursor() {
    return cursor;
  }

  @Override
  public boolean hasLength() {
    return false;
  }

  @Override
  public int getArraySize() {
    return 0;
  }

  @Override
  public ObjectID getParentObjectID() throws DNAException {
    return parentObjectID;
  }

  public void setHeaderInformation(ObjectID id, ObjectID parentID, String type, int length, long version)
      throws DNAException {
    return;
  }

  public void addPhysicalAction(String field, Object value) throws DNAException {
    return;
  }

  public void addLogicalAction(int method, Object[] parameters) {
    return;
  }

  @Override
  public long getVersion() {
    return this.version;
  }

  @Override
  public boolean isDelta() {
    return isDelta;
  }

  @Override
  public String toString() {
    return "TestDNA(" + objectID + ", version = " + version + ")";
  }
}
