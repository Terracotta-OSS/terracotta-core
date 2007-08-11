/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject.bytecode;

import com.tc.asm.ClassWriter;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.exception.TCRuntimeException;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.util.AdaptedClassDumper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PhysicalStateClassLoader extends ClassLoader implements Opcodes {

  public static class MethodDetail {

    private final String methodName;
    private final String methodDesc;

    public MethodDetail(String methodName, String methodDesc) {
      this.methodName = methodName;
      this.methodDesc = methodDesc;
    }

    public String getMethodDescriptor() {
      return methodDesc;
    }

    public String getMethodName() {
      return methodName;
    }

  }

  private static final String PARENT_ID_FIELD       = "parentId";

  private static final Map    OBJECT_OUTPUT_METHODS = Collections.synchronizedMap(new HashMap());
  private static final Map    OBJECT_INPUT_METHODS  = Collections.synchronizedMap(new HashMap());

  static {
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.INTEGER, "writeInt", "(I)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.LONG, "writeLong", "(J)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.CHARACTER, "writeChar", "(I)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.BYTE, "writeByte", "(I)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.SHORT, "writeShort", "(I)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.FLOAT, "writeFloat", "(F)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.DOUBLE, "writeDouble", "(D)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.BOOLEAN, "writeBoolean", "(Z)V");
    // rest are written as Objects - Since we use TCObjectOutputStream, we optimize it there.
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.OBJECT, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.OBJECT_ID, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.ARRAY, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.JAVA_LANG_CLASS, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.JAVA_LANG_CLASS_HOLDER, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.STACK_TRACE_ELEMENT, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.STRING, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.STRING_BYTES, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.BIG_INTEGER, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.BIG_DECIMAL, "writeObject", "(Ljava/lang/Object;)V");
    
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.JAVA_LANG_CLASSLOADER, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.JAVA_LANG_CLASSLOADER_HOLDER, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.ENUM, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.ENUM_HOLDER, "writeObject", "(Ljava/lang/Object;)V");
    addMapping(OBJECT_OUTPUT_METHODS, LiteralValues.CURRENCY, "writeObject", "(Ljava/lang/Object;)V");


    addMapping(OBJECT_INPUT_METHODS, LiteralValues.INTEGER, "readInt", "()I");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.LONG, "readLong", "()J");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.CHARACTER, "readChar", "()C");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.BYTE, "readByte", "()B");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.SHORT, "readShort", "()S");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.FLOAT, "readFloat", "()F");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.DOUBLE, "readDouble", "()D");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.BOOLEAN, "readBoolean", "()Z");
    // rest are read as Objects - Since we use TCObjectInputStream, we optimize it there.
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.OBJECT, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.OBJECT_ID, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.ARRAY, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.JAVA_LANG_CLASS, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.JAVA_LANG_CLASS_HOLDER, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.STACK_TRACE_ELEMENT, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.STRING, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.STRING_BYTES, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.BIG_INTEGER, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.BIG_DECIMAL, "readObject", "()Ljava/lang/Object;");
    
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.JAVA_LANG_CLASSLOADER, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.JAVA_LANG_CLASSLOADER_HOLDER, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.ENUM, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.ENUM_HOLDER, "readObject", "()Ljava/lang/Object;");
    addMapping(OBJECT_INPUT_METHODS, LiteralValues.CURRENCY, "readObject", "()Ljava/lang/Object;");

    
  }

  public PhysicalStateClassLoader(ClassLoader parent) {
    super(parent);
  }

  private static void addMapping(Map map, int type, String methodName, String methodDesc) {
    map.put(new Integer(type), new MethodDetail(methodName, methodDesc));
  }

  private static MethodDetail get(Map map, int type) {
    MethodDetail md = (MethodDetail) map.get(new Integer(type));
    if (md == null) { throw new TCRuntimeException("Unknown Type : " + type + " Map = " + map); }
    return md;
  }

  public PhysicalStateClassLoader() {
    super();
  }

  public byte[] createClassBytes(ClassSpec cs, ObjectID parentID, List fields) {
    byte data[] = basicCreateClassBytes(cs, parentID, fields);
    AdaptedClassDumper.INSTANCE.write(cs.getGeneratedClassName(), data);
    return data;
  }

  public Class defineClassFromBytes(String className, int classId, byte[] clazzBytes, int offset, int length) {
    Class clazz = defineClass(className, clazzBytes, offset, length);
    return clazz;
  }

  private byte[] basicCreateClassBytes(ClassSpec cs, ObjectID parentID, List fields) {
    String classNameSlash = cs.getGeneratedClassName().replace('.', '/');
    String superClassNameSlash = cs.getSuperClassName().replace('.', '/');
    ClassWriter cw = new ClassWriter(0);  // don't compute maxs

    cw.visit(V1_2, ACC_PUBLIC | ACC_SUPER, classNameSlash, null, superClassNameSlash, null);

    createConstructor(cw, superClassNameSlash);
    if (!parentID.isNull()) {
      createParentIDField(cw);
      createGetParentIDMethod(cw, classNameSlash);
      createSetParentIDMethod(cw, classNameSlash);
    }

    createFields(cw, fields);
    createGetClassNameMethod(cw, classNameSlash, cs);
    createGetLoaderDescriptionMethod(cw, classNameSlash, cs);
    createGetObjectReferencesMethod(cw, classNameSlash, parentID, cs, superClassNameSlash, fields);
    createBasicSetMethod(cw, classNameSlash, cs, superClassNameSlash, fields);
    createBasicDehydrateMethod(cw, classNameSlash, cs, superClassNameSlash, fields);
    createAddValuesMethod(cw, classNameSlash, cs, superClassNameSlash, fields);
    createWriteObjectMethod(cw, classNameSlash, cs, superClassNameSlash, fields);
    createReadObjectMethod(cw, classNameSlash, cs, superClassNameSlash, fields);

    createGetClassIdMethod(cw, classNameSlash, cs);

    cw.visitEnd();
    return cw.toByteArray();
  }

  // *************************************************************************************
  // The Code generated by this method looks (kind of) this.
  //
  // public String getLoaderDescription() {
  // return "System.ext";
  // }
  // *************************************************************************************
  private void createGetLoaderDescriptionMethod(ClassWriter cw, String classNameSlash, ClassSpec cs) {
    if (!cs.isDirectSubClassOfPhysicalMOState()) {
      // We dont have to regenerate this method as the super class would have it.
      return;
    }
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getLoaderDescription", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitLdcInsn(cs.getLoaderDesc());
    mv.visitInsn(ARETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // *************************************************************************************
  // The Code generated by this method looks (kind of) this.
  //
  // public String getClassName() {
  // return "com.tc.className";
  // }
  // *************************************************************************************
  private void createGetClassNameMethod(ClassWriter cw, String classNameSlash, ClassSpec cs) {
    if (!cs.isDirectSubClassOfPhysicalMOState()) {
      // We dont have to regenerate this method as the super class would have it.
      return;
    }
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getClassName", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitLdcInsn(cs.getClassName());
    mv.visitInsn(ARETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // *************************************************************************************
  // The Code generated by this method looks (kind of) this.
  //
  // protected int getClassId() {
  // return 8;
  // }
  // *************************************************************************************
  private void createGetClassIdMethod(ClassWriter cw, String classNameSlash, ClassSpec cs) {
    MethodVisitor mv = cw.visitMethod(ACC_PROTECTED, "getClassId", "()I", null, null);
    mv.visitLdcInsn(new Integer(cs.getClassID()));
    mv.visitInsn(IRETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  // *************************************************************************************
  // The Code generated by this method looks (kind of) this.
  //
  // long x;
  // Object o;
  // char c;
  // public void writeObject(ObjectOutput out) throws IOException {
  // out.writeLong(x);
  // out.writeObject(o);
  // out.writeChar(c);
  // }
  // *************************************************************************************
  private void createWriteObjectMethod(ClassWriter cw, String classNameSlash, ClassSpec cs, String superClassNameSlash, List fields) {
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "writeObject", "(Ljava/io/ObjectOutput;)V", null,
                                      new String[] { "java/io/IOException" });
    mv.visitCode();
    
    if (!cs.isDirectSubClassOfPhysicalMOState()) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, superClassNameSlash, "writeObject", "(Ljava/io/ObjectOutput;)V");
    }

    for (Iterator i = fields.iterator(); i.hasNext();) {
      FieldType f = (FieldType) i.next();
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, classNameSlash, f.getLocalFieldName(), getFieldTypeDesc(f.getType()));
      MethodDetail md = get(OBJECT_OUTPUT_METHODS, f.getType());
      mv.visitMethodInsn(INVOKEINTERFACE, "java/io/ObjectOutput", md.getMethodName(), md.getMethodDescriptor());
    }
    mv.visitInsn(RETURN);
    mv.visitMaxs(3, 2);
    mv.visitEnd();
  }

  // *************************************************************************************
  // The Code generated by this method looks (kind of) this.
  //
  // long x;
  // Object o;
  // char c;
  //
  // public void readObject(ObjectInput in) throws IOException, ClassNotFoundException {
  // x = in.readLong();
  // o = in.readObject();
  // c = in.readChar();
  // }
  // *************************************************************************************
  private void createReadObjectMethod(ClassWriter cw, String classNameSlash, ClassSpec cs, String superClassNameSlash, List fields) {
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "readObject", "(Ljava/io/ObjectInput;)V", null, new String[] {
        "java/io/IOException", "java/lang/ClassNotFoundException" });
    mv.visitCode();

    if (!cs.isDirectSubClassOfPhysicalMOState()) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, superClassNameSlash, "readObject", "(Ljava/io/ObjectInput;)V");
    }
    
    for (Iterator i = fields.iterator(); i.hasNext();) {
      FieldType f = (FieldType) i.next();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      MethodDetail md = get(OBJECT_INPUT_METHODS, f.getType());
      mv.visitMethodInsn(INVOKEINTERFACE, "java/io/ObjectInput", md.getMethodName(), md.getMethodDescriptor());
      mv.visitFieldInsn(PUTFIELD, classNameSlash, f.getLocalFieldName(), getFieldTypeDesc(f.getType()));
    }
    mv.visitInsn(RETURN);
    mv.visitMaxs(3, 2);
    mv.visitEnd();
  }

  // *************************************************************************************
  // The Code generated by this method looks (kind of) this.
  //
  // long x;
  // Object o;
  // public Map addValues(Map map) {
  // map.put("x", new Long(x));
  // map.put("o", o);
  // return map;
  // }
  // *************************************************************************************
  private void createAddValuesMethod(ClassWriter cw, String classNameSlash, ClassSpec cs, String superClassNameSlash,
                                     List fields) {
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "addValues", "(Ljava/util/Map;)Ljava/util/Map;", null, null);
    mv.visitCode();

    if (!cs.isDirectSubClassOfPhysicalMOState()) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, superClassNameSlash, "addValues", "(Ljava/util/Map;)Ljava/util/Map;");
      mv.visitInsn(POP);
    }

    for (Iterator i = fields.iterator(); i.hasNext();) {
      FieldType f = (FieldType) i.next();
      mv.visitVarInsn(ALOAD, 1);
      mv.visitLdcInsn(f.getQualifiedName());
      getObjectFor(mv, classNameSlash, f);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                         "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
      mv.visitInsn(POP);
    }
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARETURN);

    mv.visitMaxs(6, 2);
    mv.visitEnd();

  }

  // *************************************************************************************
  // The Code generated by this method looks (kind of) this.
  //
  // long x;
  // Object o;
  // protected void basicDehydrate(DNAWriter writer) {
  // writer.addPhysicalAction("x", new Long(x));
  // writer.addPhysicalAction("o", o);
  // }
  // *************************************************************************************
  private void createBasicDehydrateMethod(ClassWriter cw, String classNameSlash, ClassSpec cs,
                                          String superClassNameSlash, List fields) {
    MethodVisitor mv = cw.visitMethod(ACC_PROTECTED, "basicDehydrate", "(Lcom/tc/object/dna/api/DNAWriter;)V", null,
                                      null);
    mv.visitCode();

    if (!cs.isDirectSubClassOfPhysicalMOState()) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, superClassNameSlash, "basicDehydrate", "(Lcom/tc/object/dna/api/DNAWriter;)V");
    }

    for (Iterator i = fields.iterator(); i.hasNext();) {
      FieldType f = (FieldType) i.next();
      mv.visitVarInsn(ALOAD, 1);
      mv.visitLdcInsn(f.getQualifiedName());
      getObjectFor(mv, classNameSlash, f);
      // TODO:: May be we shouldnt call DNAWriter methods from instrumented code !
      mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/dna/api/DNAWriter", "addPhysicalAction",
                         "(Ljava/lang/String;Ljava/lang/Object;)V");
    }
    mv.visitInsn(RETURN);

    mv.visitMaxs(6, 2);
    mv.visitEnd();
  }

  // *************************************************************************************
  // The Code generated by this method looks (kind of) this.
  //
  // long x;
  // Object o;
  // protected Object basicSet(String f, Object value) {
  // if ("x".equals(f)) {
  // Object old = new Long(x);
  // x = ((Long) value).longValue();
  // return old;
  // }
  // if("o".equals(f)) {
  // Object old = o;
  // o = value;
  // return old;
  // }
  // throw new ClassNotCompatableException("Not found ! field = " + f + " value = " + value);
  // }
  // *************************************************************************************
  private void createBasicSetMethod(ClassWriter cw, String classNameSlash, ClassSpec cs, String superClassNameSlash,
                                    List fields) {
    MethodVisitor mv = cw.visitMethod(ACC_PROTECTED, "basicSet",
                                      "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", null, null);
    mv.visitCode();

    for (Iterator i = fields.iterator(); i.hasNext();) {
      FieldType f = (FieldType) i.next();
      mv.visitLdcInsn(f.getQualifiedName());
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
      Label l1 = new Label();
      mv.visitJumpInsn(IFEQ, l1);
      getObjectFor(mv, classNameSlash, f);
      mv.visitVarInsn(ASTORE, 3);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 2);
      getValueFrom(mv, classNameSlash, f);
      mv.visitFieldInsn(PUTFIELD, classNameSlash, f.getLocalFieldName(), getFieldTypeDesc(f.getType()));
      mv.visitVarInsn(ALOAD, 3);
      mv.visitInsn(ARETURN);
      mv.visitLabel(l1);
    }

    if (cs.isDirectSubClassOfPhysicalMOState()) {
      // throw Assertion Error
      mv.visitTypeInsn(NEW, "com/tc/objectserver/managedobject/bytecode/ClassNotCompatableException");
      mv.visitInsn(DUP);
      mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
      mv.visitInsn(DUP);
      mv.visitLdcInsn("Not found ! field = ");
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                         "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
      mv.visitLdcInsn(" value = ");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                         "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                         "(Ljava/lang/Object;)Ljava/lang/StringBuffer;");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
      mv.visitMethodInsn(INVOKESPECIAL, "com/tc/objectserver/managedobject/bytecode/ClassNotCompatableException",
                         "<init>", "(Ljava/lang/String;)V");
      mv.visitInsn(ATHROW);
    } else {
      // Call super class's implementation
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKESPECIAL, superClassNameSlash, "basicSet",
                         "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
      mv.visitInsn(ARETURN);
    }

    mv.visitMaxs(5, 4);
    mv.visitEnd();
  }

  /**
   * This method generates code to the object reference from the top of the stack and convert it into a primitive type
   * (if needed) and leave that value in the top of the stack.
   */
  private void getValueFrom(MethodVisitor mv, String classNameSlash, FieldType f) {
    String classOnStack = getClassNameFor(f.getType());
    if ("java/lang/Object".equals(classOnStack)) { return; }
    mv.visitTypeInsn(CHECKCAST, classOnStack);
    mv.visitMethodInsn(INVOKEVIRTUAL, classOnStack, getMethodNameForPrimitives(f.getType()), "()"
                                                                                             + getFieldTypeDesc((f
                                                                                                 .getType())));

  }

  /**
   * This method generates code so that the object equivalent of the field is left on the stack.
   */
  private void getObjectFor(MethodVisitor mv, String classNameSlash, FieldType f) {
    String classToReturn = getClassNameFor(f.getType());
    if ("java/lang/Object".equals(classToReturn)) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, classNameSlash, f.getLocalFieldName(), getFieldTypeDesc(f.getType()));
      return;
    }
    String fieldTypeDesc = getFieldTypeDesc(f.getType());
    String constructorDesc = "(" + fieldTypeDesc + ")V";
    mv.visitTypeInsn(NEW, classToReturn);
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, classNameSlash, f.getLocalFieldName(), fieldTypeDesc);
    mv.visitMethodInsn(INVOKESPECIAL, classToReturn, "<init>", constructorDesc);

  }

  // *************************************************************************************
  // The Code generated by this method looks (kind of) this.
  //
  // private Object oid1;
  // public Set getObjectReferences() {
  // Set result = new THashSet(25);
  // if (oid1 instanceof ObjectID && !((ObjectID)oid1).isNull() ) {
  // result.add(oid1);
  // }
  // return result;
  // }
  // *************************************************************************************
  private void createGetObjectReferencesMethod(ClassWriter cw, String classNameSlash, ObjectID parentID, ClassSpec cs,
                                               String superClassNameSlash, List fields) {
    List referenceFields = new ArrayList(fields.size());
    for (Iterator i = fields.iterator(); i.hasNext();) {
      FieldType f = (FieldType) i.next();
      if (f.getType() == LiteralValues.OBJECT_ID) {
        referenceFields.add(f);
      }
    }

    // There is no references in this object and it is not a direct subclass of Physical Managed Object State
    if (referenceFields.size() == 0 && parentID.isNull() && !cs.isDirectSubClassOfPhysicalMOState()) {
      // The parent object has the necessary implementations
      return;
    }

    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getObjectReferences", "()Ljava/util/Set;", null, null);
    mv.visitCode();

    // There is no references in this object
    if (referenceFields.size() == 0 && parentID.isNull()) {
      mv.visitFieldInsn(GETSTATIC, "java/util/Collections", "EMPTY_SET", "Ljava/util/Set;");
      mv.visitInsn(ARETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
      return;
    }

    int size = referenceFields.size();
    if (!parentID.isNull() && cs.isDirectSubClassOfPhysicalMOState()) {
      size++;
    }

    mv.visitTypeInsn(NEW, "gnu/trove/THashSet");
    mv.visitInsn(DUP);
    mv.visitIntInsn(BIPUSH, size);
    mv.visitMethodInsn(INVOKESPECIAL, "gnu/trove/THashSet", "<init>", "(I)V");
    mv.visitVarInsn(ASTORE, 1);

    if (!cs.isDirectSubClassOfPhysicalMOState()) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, superClassNameSlash, "getObjectReferences", "()Ljava/util/Set;");
      mv.visitVarInsn(ASTORE, 2);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "addAll", "(Ljava/util/Collection;)Z");
      mv.visitInsn(POP);
    }

    for (Iterator i = referenceFields.iterator(); i.hasNext();) {
      FieldType f = (FieldType) i.next();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, classNameSlash, f.getLocalFieldName(), getFieldTypeDesc(f.getType()));
      mv.visitTypeInsn(INSTANCEOF, "com/tc/object/ObjectID");
      Label l2 = new Label();
      mv.visitJumpInsn(IFEQ, l2);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, classNameSlash, f.getLocalFieldName(), getFieldTypeDesc(f.getType()));
      mv.visitTypeInsn(CHECKCAST, "com/tc/object/ObjectID");
      mv.visitMethodInsn(INVOKEVIRTUAL, "com/tc/object/ObjectID", "isNull", "()Z");
      mv.visitJumpInsn(IFNE, l2);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, classNameSlash, f.getLocalFieldName(), getFieldTypeDesc(f.getType()));
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "add", "(Ljava/lang/Object;)Z");
      mv.visitInsn(POP);
      mv.visitLabel(l2);
    }
    if (!parentID.isNull() && cs.isDirectSubClassOfPhysicalMOState()) {
      // add parentID too
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, classNameSlash, PARENT_ID_FIELD, "Lcom/tc/object/ObjectID;");
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "add", "(Ljava/lang/Object;)Z");
      mv.visitInsn(POP);
    }
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(3, 3);
    mv.visitEnd();
  }

  private void createFields(ClassWriter cw, List fields) {
    for (Iterator i = fields.iterator(); i.hasNext();) {
      FieldType f = (FieldType) i.next();
      FieldVisitor fv = cw.visitField(ACC_PRIVATE, f.getLocalFieldName(), getFieldTypeDesc(f.getType()), null, null);
      fv.visitEnd();
    }
  }

  private String getFieldTypeDesc(int type) {
    switch (type) {
      case LiteralValues.INTEGER:
        return "I";
      case LiteralValues.LONG:
        return "J";
      case LiteralValues.CHARACTER:
        return "C";
      case LiteralValues.FLOAT:
        return "F";
      case LiteralValues.DOUBLE:
        return "D";
      case LiteralValues.BYTE:
        return "B";
      case LiteralValues.SHORT:
        return "S";
      case LiteralValues.BOOLEAN:
        return "Z";
      case LiteralValues.OBJECT_ID:
        return "Ljava/lang/Object;"; // ObjectIDs are NOT stored as longs anymore :(
      default:
        return "Ljava/lang/Object;"; // Everything else is stored as Object reference
    }
  }

  // TODO:: Clean up to use MethodDetail class
  private String getMethodNameForPrimitives(int type) {
    switch (type) {
      case LiteralValues.INTEGER:
        return "intValue";
      case LiteralValues.LONG:
        return "longValue";
      case LiteralValues.CHARACTER:
        return "charValue";
      case LiteralValues.FLOAT:
        return "floatValue";
      case LiteralValues.DOUBLE:
        return "doubleValue";
      case LiteralValues.BYTE:
        return "byteValue";
      case LiteralValues.SHORT:
        return "shortValue";
      case LiteralValues.BOOLEAN:
        return "booleanValue";
      // ObjectIDs are NOT stored as longs anymore :(
      // case LiteralValues.OBJECT_ID:
      // return "toLong";
      default:
        throw new AssertionError("This type is invalid : " + type);
    }
  }

  private String getClassNameFor(int type) {
    switch (type) {
      case LiteralValues.INTEGER:
        return "java/lang/Integer";
      case LiteralValues.LONG:
        return "java/lang/Long";
      case LiteralValues.CHARACTER:
        return "java/lang/Character";
      case LiteralValues.FLOAT:
        return "java/lang/Float";
      case LiteralValues.DOUBLE:
        return "java/lang/Double";
      case LiteralValues.BYTE:
        return "java/lang/Byte";
      case LiteralValues.SHORT:
        return "java/lang/Short";
      case LiteralValues.BOOLEAN:
        return "java/lang/Boolean";
      case LiteralValues.OBJECT_ID:
        return "java/lang/Object"; // ObjectIDs are NOT stored as longs anymore :(
      default:
        return "java/lang/Object"; // Everything else is stored as Object reference
    }
  }

  private void createParentIDField(ClassWriter cw) {
    FieldVisitor fv = cw.visitField(ACC_PRIVATE, PARENT_ID_FIELD, "Lcom/tc/object/ObjectID;", null, null);
    fv.visitEnd();
  }

  // *************************************************************************************
  // The Code generated by this method looks (kind of) this.
  //
  // ObjectID parentId;
  // public ObjectID getParentID() {
  // return parentId;
  // }
  // *************************************************************************************
  private void createGetParentIDMethod(ClassWriter cw, String classNameSlash) {
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getParentID", "()Lcom/tc/object/ObjectID;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, classNameSlash, PARENT_ID_FIELD, "Lcom/tc/object/ObjectID;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(2, 1);
    mv.visitEnd();
  }

  // *************************************************************************************
  // The Code generated by this method looks (kind of) this.
  //
  // ObjectID parentId;
  // public void setParentID(ObjectID id) {
  // parentId = id;
  // }
  // *************************************************************************************
  private void createSetParentIDMethod(ClassWriter cw, String classNameSlash) {
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "setParentID", "(Lcom/tc/object/ObjectID;)V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, classNameSlash, PARENT_ID_FIELD, "Lcom/tc/object/ObjectID;");
    mv.visitInsn(RETURN);
    mv.visitMaxs(3, 2);
    mv.visitEnd();
  }

  private void createConstructor(ClassWriter cw, String superClassNameSlash) {
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, superClassNameSlash, "<init>", "()V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();
  }

}
