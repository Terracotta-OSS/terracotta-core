/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.asm.commons.LocalVariablesSorter;

class JavaUtilConcurrentHashMapLazyValuesMethodAdapter extends LocalVariablesSorter implements Opcodes {
  public JavaUtilConcurrentHashMapLazyValuesMethodAdapter(final int access, final String desc, final MethodVisitor mv) {
    super(access, desc, mv);      
  }

  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    // Ensure that the hash entry values are looked up through the manager util
    // when they're object IDs and not actual objects. This is synchronized through
    // lock JVM locks on the hash entry instances to ensure that write access to the
    // hash entry value doesn't happen in the middle, cause new values to be
    // overwritten by old values that still had to be fetched and stored.
    if (GETFIELD == opcode
        && "java/util/concurrent/ConcurrentHashMap$HashEntry".equals(owner)
        && "value".equals(name)
        && "Ljava/lang/Object;".equals(desc)) {
      
      Label labelDone = new Label();

      // store the hash entry in a local variable
      int hashEntryLocal = newLocal(Type.getObjectType("java/util/concurrent/ConcurrentHashMap$HashEntry"));
      mv.visitVarInsn(ASTORE, hashEntryLocal); 

      // setup try catch with finally to release the monitor on the hash entry that will be entered below
      Label labelStartTryCatch = new Label();
      Label labelEndTryCatch = new Label();
      Label labelFinally = new Label();
      mv.visitTryCatchBlock(labelStartTryCatch, labelEndTryCatch, labelFinally, null);
      
      // enter a monitor on the hash entry
      mv.visitVarInsn(ALOAD, hashEntryLocal);
      mv.visitInsn(MONITORENTER);

      mv.visitLabel(labelStartTryCatch);

      // obtain the value of the entry
      mv.visitVarInsn(ALOAD, hashEntryLocal); 
      super.visitFieldInsn(opcode, owner, name, desc);
      int valueLocal = newLocal(Type.getObjectType("java/lang/Object"));
      mv.visitVarInsn(ASTORE, valueLocal); 
      
      // check that the 'this' object is actually managed by DSO
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
      mv.visitJumpInsn(IFEQ, labelDone);

      // check if the entry's value is a logical object ID
      mv.visitVarInsn(ALOAD, valueLocal); 
      mv.visitTypeInsn(INSTANCEOF, "com/tc/object/ObjectID");
      mv.visitJumpInsn(IFEQ, labelDone);

      // ensure that the type is verified by the VM
      mv.visitVarInsn(ALOAD, valueLocal); 
      mv.visitTypeInsn(CHECKCAST, "com/tc/object/ObjectID");

      // lookup the real entry value from the object ID and store it back into the entry for caching
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "lookupObject", "(Lcom/tc/object/ObjectID;)Ljava/lang/Object;");
      mv.visitVarInsn(ASTORE, valueLocal); 
      mv.visitVarInsn(ALOAD, hashEntryLocal); 
      mv.visitVarInsn(ALOAD, valueLocal); 
      mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "value", "Ljava/lang/Object;");
      
      // load the entry's value from the local variable
      mv.visitLabel(labelDone);
      mv.visitVarInsn(ALOAD, valueLocal);
      
      // exit the monitor on the hash entry
      mv.visitVarInsn(ALOAD, hashEntryLocal);
      mv.visitInsn(MONITOREXIT);
      mv.visitLabel(labelEndTryCatch);
      Label labelAfterFinally = new Label();
      mv.visitJumpInsn(GOTO, labelAfterFinally);
      mv.visitLabel(labelFinally);
      mv.visitVarInsn(ALOAD, hashEntryLocal);
      mv.visitInsn(MONITOREXIT);
      // re-throw exception
      mv.visitInsn(ATHROW);
      
      mv.visitLabel(labelAfterFinally);
    }
    // Ensure that a modification to the value field of a hash entry is synchronized.
    // Note that this is local JVM synchronization and that this is sufficient.
    // We only want to ensure that a new value / object ID isn't written during
    // the process of fetching and storing the real hash entry value since this could
    // cause new values to be overwritten by old ones.
    else if (PUTFIELD == opcode
        && "java/util/concurrent/ConcurrentHashMap$HashEntry".equals(owner)
        && "value".equals(name)
        && "Ljava/lang/Object;".equals(desc)) {
      
      // store the value in a local variable
      int valueLocal = newLocal(Type.getObjectType("java/lang/Object"));
      mv.visitVarInsn(ASTORE, valueLocal); 

      // store the hash entry in a local variable
      int hashEntryLocal = newLocal(Type.getObjectType("java/util/concurrent/ConcurrentHashMap$HashEntry"));
      mv.visitVarInsn(ASTORE, hashEntryLocal); 

      // setup try catch with finally to release the monitor on the hash entry that will be entered below
      Label labelStartTryCatch = new Label();
      Label labelEndTryCatch = new Label();
      Label labelFinally = new Label();
      mv.visitTryCatchBlock(labelStartTryCatch, labelEndTryCatch, labelFinally, null);
      
      // enter a monitor on the hash entry
      mv.visitVarInsn(ALOAD, hashEntryLocal);
      mv.visitInsn(MONITORENTER);

      mv.visitLabel(labelStartTryCatch);

      // obtain the value of the entry
      mv.visitVarInsn(ALOAD, hashEntryLocal); 
      mv.visitVarInsn(ALOAD, valueLocal); 
      super.visitFieldInsn(opcode, owner, name, desc);
      
      // exit the monitor on the hash entry
      mv.visitVarInsn(ALOAD, hashEntryLocal);
      mv.visitInsn(MONITOREXIT);
      mv.visitLabel(labelEndTryCatch);
      Label labelAfterFinally = new Label();
      mv.visitJumpInsn(GOTO, labelAfterFinally);
      mv.visitLabel(labelFinally);
      mv.visitVarInsn(ALOAD, hashEntryLocal);
      mv.visitInsn(MONITOREXIT);
      // re-throw exception
      mv.visitInsn(ATHROW);
      
      mv.visitLabel(labelAfterFinally);
    } else {
      super.visitFieldInsn(opcode, owner, name, desc);
    }
  }
}