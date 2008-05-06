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

public class JavaUtilConcurrentHashMapHashEntryAdapter extends ClassAdapter implements Opcodes {
  public JavaUtilConcurrentHashMapHashEntryAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    String[] interfacesNew = new String[interfaces.length + 1];
    System.arraycopy(interfaces, 0, interfacesNew, 0, interfaces.length);
    interfacesNew[interfacesNew.length - 1] = TCMapEntry.class.getName().replace('.', '/');
    super.visit(version, access, name, signature, superName, interfacesNew);
  }
  
  public void visitEnd() {
    createTCRawSetValueMethod();
    createTCIsFaultedInMethod();

    super.visitEnd();
  }
  
  private void createTCRawSetValueMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNCHRONIZED, TCMapEntry.TC_RAWSETVALUE_METHOD_NAME, TCMapEntry.TC_RAWSETVALUE_METHOD_DESC, null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "value", "Ljava/lang/Object;");
    mv.visitInsn(RETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();
  }
  
  private void createTCIsFaultedInMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNCHRONIZED, TCMapEntry.TC_ISVALUEFAULTEDIN_METHOD_NAME, TCMapEntry.TC_ISVALUEFAULTEDIN_METHOD_DESC, null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "value", "Ljava/lang/Object;");
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
