/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
  public String    typeName       = "SomeClassName";
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

  public String getTypeName() {
    return typeName;
  }

  public ObjectID getObjectID() throws DNAException {
    return objectID;
  }

  public DNACursor getCursor() {
    return cursor;
  }

  public boolean hasLength() {
    return false;
  }

  public int getArraySize() {
    return 0;
  }

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

  public long getVersion() {
    return this.version;
  }

  public boolean isDelta() {
    return isDelta;
  }

  @Override
  public String toString() {
    return "TestDNA(" + objectID + ", version = " + version + ")";
  }
}
