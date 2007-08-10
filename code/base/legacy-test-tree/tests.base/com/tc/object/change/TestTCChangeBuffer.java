/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.change;

import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;

import gnu.trove.TIntObjectHashMap;

import java.util.List;
import java.util.Map;

public class TestTCChangeBuffer implements TCChangeBuffer {

  public void fieldChanged(String classname, String fieldname, Object newValue, int index) {
    throw new ImplementMe();

  }

  public void logicalInvoke(int method, Object[] parameters) {
    throw new ImplementMe();

  }

  public void writeTo(TCByteBufferOutputStream output, ObjectStringSerializer serializer, DNAEncoding encoding) {
    return;
  }

  public Map getPhysicalEvents() {
    throw new ImplementMe();

  }

  public List getLogicalEvents() {
    throw new ImplementMe();
  }

  public TIntObjectHashMap getArrayEvents() {
    throw new ImplementMe();
  }

  public TCObject getTCObject() {
    throw new ImplementMe();
  }

  public void arrayChanged(int startPos, Object array, int length) {
    throw new ImplementMe();

  }

  public int getTotalEventCount() {
    throw new ImplementMe();
  }

  public int getType() {
    throw new ImplementMe();
  }

  public DNACursor getDNACursor(OptimisticTransactionManager transactionManager) {
    throw new ImplementMe();
  }

  public void accept(TCChangeBufferEventVisitor visitor) {
    throw new ImplementMe();
  }

  public void literalValueChanged(Object newValue) {
    throw new ImplementMe();

  }
}
