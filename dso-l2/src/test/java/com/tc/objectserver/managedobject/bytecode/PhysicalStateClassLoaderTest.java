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
    ArrayList<FieldType> field = new ArrayList<FieldType>();
    field.add(FieldType.create("int_field", 12, false, 1));
    field.add(FieldType.create("float_field", 12.0f, false, 1));
    field.add(FieldType.create("long_field", 12L, false, 1));
    field.add(FieldType.create("double_field", 12.0d, false, 1));
    field.add(FieldType.create("char_field", 'c', false, 1));
    field.add(FieldType.create("boolean_field", true, false, 1));
    field.add(FieldType.create("byte_field", (byte)2, false, 1));
    field.add(FieldType.create("short_field", (short)2, false, 1));
    
    
    byte[] clazzBytes = loader.createClassBytes(cs, field);

    try {
      Class clazz = loader.defineClassFromBytes(cs.getGeneratedClassName(), 0, clazzBytes, 0, clazzBytes.length);
      if(clazz == null) {
        fail("could not load class");
      }
      else{
        Class[]  noParam = new Class[]{};
        Assert.assertEquals(int.class, clazz.getDeclaredField("int_field_1").getType());
        Assert.assertEquals(float.class, clazz.getDeclaredField("float_field_1").getType());
        Assert.assertEquals(long.class, clazz.getDeclaredField("long_field_1").getType());
        Assert.assertEquals(double.class, clazz.getDeclaredField("double_field_1").getType());
        Assert.assertEquals(char.class, clazz.getDeclaredField("char_field_1").getType());
        Assert.assertEquals(boolean.class, clazz.getDeclaredField("boolean_field_1").getType());
        Assert.assertEquals(byte.class, clazz.getDeclaredField("byte_field_1").getType());
        Assert.assertEquals(short.class, clazz.getDeclaredField("short_field_1").getType());
        
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
