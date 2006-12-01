/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;

public class TestDNAWriter implements DNAWriter {

  public DNA dna;

  public TestDNAWriter() {
    //
  }

  public void addLogicalAction(int method, Object[] parameters) {
    //
  }

  public void addPhysicalAction(String field, Object value) {
    //
  }

  public void finalizeDNA() {
    //
  }

  public void addArrayElementAction(int index, Object value) {
    //
  }

  public void addEntireArray(Object value) {
    //
  }
  
  public void addLiteralValue(Object value) {
    //
  }

  public void setParentObjectID(ObjectID id) {
    //
  }

  public void setArrayLength(int length) {
    //
  }

  public void addPhysicalAction(String fieldName, Object value, boolean canBeReferenced) {
    //
  }

  public void addClassLoaderAction(String classLoaderFieldName, Object value) {
    //
    
  }

  public void addSubArrayAction(int start, Object array) {
    //
  }

}
