/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class JavaUtilConcurrentHashMapWriteThroughEntryAdapter extends ClassAdapter implements Opcodes {
  public JavaUtilConcurrentHashMapWriteThroughEntryAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    String[] interfacesNew = new String[interfaces.length + 1];
    System.arraycopy(interfaces, 0, interfacesNew, 0, interfaces.length);
    interfacesNew[interfacesNew.length - 1] = TCMapEntry.class.getName().replace('.', '/');
    super.visit(version, access, name, signature, superName, interfacesNew);
  }
  
  public void visitEnd() {
    createGetValueMethod();
    createTCRawSetValueMethod();
    createTCIsFaultedInMethod();

    super.visitEnd();
  }
  
  private void createGetValueMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "getValue", "()Ljava/lang/Object;", null, null);
    mv.visitCode();

    // get the value form the outer ConcurrentHashMap instance (uses correct locking) - this causes the fault in of the latest value
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$WriteThroughEntry", "this$0", "Ljava/util/concurrent/ConcurrentHashMap;");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$WriteThroughEntry", "getKey", "()Ljava/lang/Object;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
    mv.visitInsn(POP);

    // but we can't trust the get return since it might not be valid for our uses (SUN BUG: 6312056)
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/AbstractMap$SimpleEntry", "getValue", "()Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 1);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitTypeInsn(INSTANCEOF, "com/tc/object/ObjectID");
    Label labelNotObjectID = new Label();
    mv.visitJumpInsn(IFEQ, labelNotObjectID);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitTypeInsn(CHECKCAST, "com/tc/object/ObjectID");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "lookupObject", "(Lcom/tc/object/ObjectID;)Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/AbstractMap$SimpleEntry", "setValue", "(Ljava/lang/Object;)Ljava/lang/Object;");
    mv.visitInsn(POP);
    mv.visitLabel(labelNotObjectID);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(1, 2);
    mv.visitEnd();
  }

  private void createTCRawSetValueMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNCHRONIZED, TCMapEntry.TC_RAWSETVALUE_METHOD_NAME, TCMapEntry.TC_RAWSETVALUE_METHOD_DESC, null, null);
    mv.visitCode();
    
    // set the value in the parent SimpleEntry
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/AbstractMap$SimpleEntry", "setValue", "(Ljava/lang/Object;)Ljava/lang/Object;");
    mv.visitInsn(POP);

    // push the new value into the outer ConcurrentHashMap instance
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$WriteThroughEntry", "this$0", "Ljava/util/concurrent/ConcurrentHashMap;");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$WriteThroughEntry", "getKey", "()Ljava/lang/Object;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap", "__tc_applicator_put", "(Ljava/lang/Object;Ljava/lang/Object;)V");
    
    mv.visitInsn(RETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();
  }
  
  private void createTCIsFaultedInMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNCHRONIZED, TCMapEntry.TC_ISVALUEFAULTEDIN_METHOD_NAME, TCMapEntry.TC_ISVALUEFAULTEDIN_METHOD_DESC, null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/AbstractMap$SimpleEntry", "getValue", "()Ljava/lang/Object;");
    mv.visitTypeInsn(INSTANCEOF, "com/tc/object/ObjectID");
    
    Label labelTrue = new Label();
    mv.visitJumpInsn(IFEQ, labelTrue);
    mv.visitInsn(ICONST_0);
    Label labelFalse = new Label();
    mv.visitJumpInsn(GOTO, labelFalse);
    mv.visitLabel(labelTrue);
    mv.visitInsn(ICONST_1);
    mv.visitLabel(labelFalse);
    
    mv.visitInsn(IRETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }
}
