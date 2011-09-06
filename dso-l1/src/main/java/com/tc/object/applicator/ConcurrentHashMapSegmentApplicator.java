/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.exception.TCRuntimeException;
import com.tc.object.ClientObjectManager;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.bytecode.JavaUtilConcurrentHashMapSegmentAdapter;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.util.Assert;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ConcurrentHashMapSegmentApplicator extends PhysicalApplicator {
  private static final String TABLE_LENGTH_FIELD_NAME = "java.util.concurrent.ConcurrentHashMap$Segment.capacity";
  private static final String TABLE_FIELD_NAME = "table";
  
  private final TCClass clazz;

  public ConcurrentHashMapSegmentApplicator(TCClass clazz, DNAEncoding encoding) {
    super(clazz, encoding);
    this.clazz = clazz;
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    String fieldName;
    Object fieldValue;
    
    Integer capacity = null;

    while (cursor.next(encoding)) {
      PhysicalAction a = cursor.getPhysicalAction();
      Assert.eval(a.isTruePhysical());
      fieldName = a.getFieldName();
      fieldValue = a.getObject();
      if (TABLE_LENGTH_FIELD_NAME.equals(fieldName)) {
        capacity = (Integer)fieldValue;
      } else {
        tcObject.setValue(fieldName, fieldValue);
      }
    }
    initializeTable(capacity, po);
  }
  
  private void initializeTable(Integer capacity, Object pojo) {
    Assert.assertNotNull(capacity);
    Class peerClass = clazz.getPeerClass();
    try {
      Method method = peerClass.getDeclaredMethod(JavaUtilConcurrentHashMapSegmentAdapter.INITIAL_TABLE_METHOD_NAME, new Class[]{Integer.TYPE});
      method.setAccessible(true);
      method.invoke(pojo, new Object[]{capacity});
    } catch (SecurityException e) {
      throw new TCRuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new TCRuntimeException(e);
    }
  }
  
  private Field getTableField() {
    Class peerClass = clazz.getPeerClass();
    try {
      Field field = peerClass.getDeclaredField(TABLE_FIELD_NAME);
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException e) {
      throw new TCRuntimeException(e);
    }
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    super.dehydrate(objectManager, tcObject, writer, pojo);
    
    try {
      Field field = getTableField();
      Object[] tableArray = (Object[])field.get(pojo);
      int tableLength = tableArray.length;
      writer.addPhysicalAction(TABLE_LENGTH_FIELD_NAME, Integer.valueOf(tableLength));
    } catch (SecurityException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }
    
  }

}
