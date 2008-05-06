/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutput;
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

  public void addPhysicalAction(String fieldName, Object value, boolean canBeReference) {
    //
  }

  public void addClassLoaderAction(String classLoaderFieldName, ClassLoader value) {
    //

  }

  public void addSubArrayAction(int start, Object array, int length) {
    //
  }

  public int getActionCount() {
    throw new ImplementMe();
  }

  public void copyTo(TCByteBufferOutput dest) {
    throw new ImplementMe();
  }

  public DNAWriter createAppender() {
    throw new ImplementMe();
  }

  public void finalizeHeader() {
    throw new ImplementMe();
  }

  public boolean isContiguous() {
    throw new ImplementMe();
  }

  public void markSectionEnd() {
    throw new ImplementMe();
  }

}
