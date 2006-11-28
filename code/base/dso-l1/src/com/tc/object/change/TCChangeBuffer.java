/*
 * Created on Dec 20, 2003
 */
package com.tc.object.change;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;

import java.util.Collection;

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

  /**
   * Adds a reference to the real object, if the object is new.
   */
  public void addNewObjectTo(Collection newObjects);

  public TCObject getTCObject();

  public int getTotalEventCount();

  public int getType();

  public void accept(TCChangeBufferEventVisitor visitor);

  public DNACursor getDNACursor(OptimisticTransactionManager transactionManager);

}