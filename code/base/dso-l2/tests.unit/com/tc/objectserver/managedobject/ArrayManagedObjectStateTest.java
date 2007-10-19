/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ManagedObjectStateSerializationTest.TestDNACursor;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ArrayManagedObjectStateTest extends AbstractTestManagedObjectState {

  public void basicTestUnit(String className, final byte type, TestDNACursor cursor, int objCount) throws Exception {
    ManagedObjectState state = createManagedObjectState(className);
    state.apply(objectID, cursor, new BackReferences());

    // API verification
    Assert.assertTrue("BackReferences size="+state.getObjectReferences().size(), 
                      state.getObjectReferences().size() == objCount);
    Assert.assertTrue(state.getType() == type);
    Assert.assertTrue("ClassName:"+state.getClassName(), state.getClassName().equals(className));
    
    // dehydrate
    TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(objectID, dnaWriter);
    cursor.reset();
    cursor.next();
    while(cursor.next()) {
      Object action = cursor.getAction();
      Assert.assertTrue(dnaWriter.containsAction(action));
    }
    
    // writeTo, readFrom and equal
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    TCObjectOutputStream out = new TCObjectOutputStream(bout);
    state.writeTo(out);
    ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
    TCObjectInputStream in = new TCObjectInputStream(bin);
    ManagedObjectState state2 = ManagedObjectStateFactory.getInstance().readManagedObjectStateFrom(in, type);
    Assert.assertTrue(state.equals(state2));
  }
/*
  public void testIntegerArray() throws Exception {
    String className = "[java.lang.Integer";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addArrayAction(new Integer[] { new Integer(2002), new Integer(2003) });

    basicTestUnit(className, ManagedObjectState.ARRAY_TYPE, cursor, 0);
    // failed when readFrom back, the literalType changed from 0 (Integer) to 10 (Object)
  }
  */
  
  public void testObjectArray() throws Exception {
    String className = "[java.lang.Object";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addArrayAction(new Object[] { new ObjectID(2002), new ObjectID(2003) });

    basicTestUnit(className, ManagedObjectState.ARRAY_TYPE, cursor, 2);
  }

}
