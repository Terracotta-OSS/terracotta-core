/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.change;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;

/**
 * @author orion
 */
public interface TCChangeBuffer {
  public final static int NONE     = 0;
  public final static int PHYSICAL = 1;
  public final static int LOGICAL  = 3;
  public final static int ARRAY    = 7;

  public void literalValueChanged(Object newValue);

  public void fieldChanged(String classname, String fieldname, Object newValue, int index);

  public void arrayChanged(int startPos, Object array, int length);

  public void logicalInvoke(int method, Object[] parameters);

  public void writeTo(TCByteBufferOutputStream output, ObjectStringSerializer serializer, DNAEncoding encoding);

  public TCObject getTCObject();

  public int getTotalEventCount();

  public int getType();

  public void accept(TCChangeBufferEventVisitor visitor);

  public DNACursor getDNACursor(OptimisticTransactionManager transactionManager);

}