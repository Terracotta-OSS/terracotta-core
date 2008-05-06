/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.util.Assert;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class LiteralTypesManagedObjectStateTest extends AbstractTestManagedObjectState {

  public void basicLiteral(String className, Object obj) throws Exception {
    TestDNACursor cursor = new TestDNACursor();
    ManagedObjectState state = createManagedObjectState(className, cursor);
    cursor.addLiteralAction(obj);
    state.apply(objectID, cursor, new BackReferences());

    // API verification
    Assert.assertTrue(state.getObjectReferences().size() == 0);
    Assert.assertTrue(state.getType() == ManagedObjectState.LITERAL_TYPE);
    Assert.assertTrue(state.getClassName() == className);
    
    // dehydrate
    cursor.reset();
    cursor.next();
    Object action = cursor.getAction();
    TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(objectID, dnaWriter);
    Assert.assertTrue(dnaWriter.containsAction(action));
    
    // writeTo, readFrom and equal
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    TCObjectOutputStream out = new TCObjectOutputStream(bout);
    state.writeTo(out);
    ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
    TCObjectInputStream in = new TCObjectInputStream(bin);
    ManagedObjectState state2 = ManagedObjectStateFactory.getInstance().readManagedObjectStateFrom(in, ManagedObjectState.LITERAL_TYPE);
    Assert.assertTrue(state.equals(state2));
  }

  public void testLiteralInteger() throws Exception {
    basicLiteral("java.lang.Integer", new Integer(34));
  }
  
  public void testLiteralString() throws Exception {
    basicLiteral("java.lang.String", new String("Hello"));
  }
  
  public void testLiteralByte() throws Exception {
    Byte data = new Byte((byte)0xf0);
    basicLiteral(data.getClass().getName(), data);
  }

}
