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
import com.tc.util.runtime.Vm;

public class JavaUtilConcurrentHashMapEntryIteratorAdapter extends ClassAdapter implements Opcodes {

  public JavaUtilConcurrentHashMapEntryIteratorAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    String[] interfacesNew = new String[interfaces.length + 1];
    System.arraycopy(interfaces, 0, interfacesNew, 0, interfaces.length);
    interfacesNew[interfacesNew.length - 1] = TCMapEntry.class.getName().replace('.', '/');
    super.visit(version, access, name, signature, superName, interfacesNew);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (Vm.isJDK16Compliant()
        && "next".equals(name)
        && "()Ljava/util/Map$Entry;".equals(desc)) {
      return mv;  
    } else {
      return new JavaUtilConcurrentHashMapLazyValuesMethodAdapter(access, desc, mv, false);
    }  
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
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$EntryIterator", "lastReturned", "Ljava/util/concurrent/ConcurrentHashMap$HashEntry;");
    Label lastReturnedNotNull = new Label();
    mv.visitJumpInsn(IFNONNULL, lastReturnedNotNull);
    mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Entry was removed");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(lastReturnedNotNull);

    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$EntryIterator", "lastReturned", "Ljava/util/concurrent/ConcurrentHashMap$HashEntry;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/bytecode/TCMapEntry", TCMapEntry.TC_RAWSETVALUE_METHOD_NAME, TCMapEntry.TC_RAWSETVALUE_METHOD_DESC);
    mv.visitInsn(RETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();
  }
  
  private void createTCIsFaultedInMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNCHRONIZED, TCMapEntry.TC_ISVALUEFAULTEDIN_METHOD_NAME, TCMapEntry.TC_ISVALUEFAULTEDIN_METHOD_DESC, null, null);
    mv.visitCode();
    
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$EntryIterator", "lastReturned", "Ljava/util/concurrent/ConcurrentHashMap$HashEntry;");
    Label lastReturnedNotNull = new Label();
    mv.visitJumpInsn(IFNONNULL, lastReturnedNotNull);
    mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Entry was removed");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(lastReturnedNotNull);

    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$EntryIterator", "lastReturned", "Ljava/util/concurrent/ConcurrentHashMap$HashEntry;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/bytecode/TCMapEntry", TCMapEntry.TC_ISVALUEFAULTEDIN_METHOD_NAME, TCMapEntry.TC_ISVALUEFAULTEDIN_METHOD_DESC);
    mv.visitInsn(IRETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }
}