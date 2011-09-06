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
import com.tc.asm.Type;
import com.tc.object.TCObjectExternal;

import java.lang.reflect.AccessibleObject;

public class AccessibleObjectAdapter extends ClassAdapter implements Opcodes {

  public AccessibleObjectAdapter(ClassVisitor cv) {
    super(cv);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("setAccessible0".equals(name) && "(Ljava/lang/reflect/AccessibleObject;Z)V".equals(desc)) { return new AccessibleSetAccessibleMethodVisitor(
                                                                                                                                                    mv); }

    return mv;
  }

  private static class AccessibleSetAccessibleMethodVisitor extends MaxLocalVarStoreDetectingMethodAdapter {

    private AccessibleSetAccessibleMethodVisitor(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (PUTFIELD == opcode && owner.equals(Type.getInternalName(AccessibleObject.class)) && name.equals("override")
          && desc.equals("Z")) {

        int boolean_var_store = getMaxLocalVarStore() + 1;
        int tcobject_var_store = boolean_var_store + 1;

        // make a copy of the boolean that's currently on the stack,
        // ready to be assigned to the 'override' field
        mv.visitInsn(DUP);

        // store the boolean value in the appropriate local variable slot
        mv.visitVarInsn(ISTORE, boolean_var_store);

        // load the reference to the currently executing object instance
        mv.visitVarInsn(ALOAD, 0);

        // look up the TCObject from the TC manager that corresponds
        // to the current object instance
        mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ManagerUtil.class), "lookupExistingOrNull",
                           "(Ljava/lang/Object;)Lcom/tc/object/TCObjectExternal;");

        // store the TCObject in the appropriate local variable slot
        mv.visitVarInsn(ASTORE, tcobject_var_store);

        // check if the TCObject instance is null, and jump over the state
        // modification code that follows
        mv.visitVarInsn(ALOAD, tcobject_var_store);
        Label label_tcobject_null = new Label();
        mv.visitJumpInsn(IFNULL, label_tcobject_null);

        // if the TCObject instance is not null, obtain it again so
        // that it can be used to signal the new value for the local 'override"
        // variable
        mv.visitVarInsn(ALOAD, tcobject_var_store);
        mv.visitLdcInsn(AccessibleObject.class.getName());
        mv.visitLdcInsn(AccessibleObject.class.getName() + ".override");
        mv.visitVarInsn(ILOAD, boolean_var_store);
        mv.visitInsn(ICONST_M1);
        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(TCObjectExternal.class), "booleanFieldChanged",
                           "(Ljava/lang/String;Ljava/lang/String;ZI)V");

        // label that is jumped to in case the TCObject instance is null
        mv.visitLabel(label_tcobject_null);
      }
      super.visitFieldInsn(opcode, owner, name, desc);
    }
  }
}