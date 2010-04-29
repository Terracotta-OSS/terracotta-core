package com.tc.objectserver.managedobject.bytecode;

import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNAWriter;
import com.tc.test.TCTestCase;

import java.util.ArrayList;
import java.util.Map;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import junit.framework.Assert;

public class PhysicalStateClassLoaderTest extends TCTestCase {

  PhysicalStateClassLoader loader = new PhysicalStateClassLoader();
  ClassSpec cs;
  
  public void setUp(){
    String className = "com.xxx.SomeClassName";
    String loaderDesc = "System.loader";
    cs = new ClassSpec(className, loaderDesc, 1000);
    cs.setGenerateParentIdStorage(true);
    cs.setGeneratedClassID(2000);
  }
  
  public void testIfMappingForAllLiteralValuesExists() throws Exception {
    for (LiteralValues type : LiteralValues.values()) {

      Assert.assertNotNull(type.getInputMethodName());
      Assert.assertNotNull(type.getInputMethodDescriptor());

      Assert.assertNotNull(type.getOutputMethodName());
      Assert.assertNotNull(type.getOutputMethodDescriptor());
    }
  }
  
  public void testClassBytesNotZero(){
    byte[] clazzBytes = loader.createClassBytes(cs, new ArrayList<FieldType>());
    Assert.assertTrue(clazzBytes.length > 0);
  }
  
  public void testClassGeneratedMethods() throws Exception {
    byte[] clazzBytes = loader.createClassBytes(cs, new ArrayList<FieldType>());

    try {
      Class clazz = loader.defineClassFromBytes(cs.getGeneratedClassName(), 0, clazzBytes, 0, clazzBytes.length);
      if(clazz == null) {
        fail("could not load class");
      }
      else{
        Class[]  noParam = new Class[]{};
        Assert.assertNotNull(clazz.getDeclaredMethod("getLoaderDescription" , noParam));
        Assert.assertNotNull(clazz.getDeclaredMethod("getClassName" , noParam));
        Assert.assertNotNull(clazz.getDeclaredMethod("getClassId" , noParam));
        Assert.assertNotNull(clazz.getDeclaredMethod("writeObject" , new Class[]{ObjectOutput.class}));
        Assert.assertNotNull(clazz.getDeclaredMethod("readObject" , new Class[]{ObjectInput.class}));
        Assert.assertNotNull(clazz.getDeclaredMethod("hasNoReferences" , noParam));
        Assert.assertNotNull(clazz.getDeclaredMethod("addValues" , new Class[]{Map.class}));
        Assert.assertNotNull(clazz.getDeclaredMethod("basicDehydrate" , new Class[]{DNAWriter.class}));
        Assert.assertNotNull(clazz.getDeclaredMethod("basicSet" , new Class[]{String.class, Object.class}));
        Assert.assertNotNull(clazz.getDeclaredMethod("getObjectReferences" , noParam));
        Assert.assertNotNull(clazz.getDeclaredMethod("getParentID" , noParam));
        Assert.assertNotNull(clazz.getDeclaredMethod("setParentID" , new Class[]{ObjectID.class}));
        Assert.assertNotNull(clazz.getDeclaredConstructor(noParam));
      }
     } catch (Exception e) {
      e.printStackTrace();
    }

  }
  
}
