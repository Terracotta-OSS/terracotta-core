/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.test.activepassive.ActivePassiveCrashMode;
import com.tc.test.activepassive.ActivePassivePersistenceMode;
import com.tc.test.activepassive.ActivePassiveSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tctest.runner.TransparentAppConfig;

import java.util.HashMap;
import java.util.Map;

public class ArrayMutateValidateTest extends TransparentTestBase {

  public static final int      MUTATOR_NODE_COUNT      = 2;
  public static final int      ADAPTED_MUTATOR_COUNT   = 1;
  public static final int      VALIDATOR_NODE_COUNT    = 1;
  public static final int      APP_INSTANCE_PER_NODE   = 2;
  private static final boolean IS_MUTATE_VALIDATE_TEST = true;

  public void doSetUp(TransparentTestIface t) throws Exception {
    TransparentAppConfig tac = t.getTransparentAppConfig();
    tac.setClientCount(MUTATOR_NODE_COUNT).setIntensity(1).setValidatorCount(VALIDATOR_NODE_COUNT)
        .setApplicationInstancePerClientCount(APP_INSTANCE_PER_NODE).setAdaptedMutatorCount(ADAPTED_MUTATOR_COUNT);

    Map adapterMap = new HashMap();
    adapterMap.put(getApplicationClass().getName(), ArrayMutateValidateTestAppAdapter.class);
    tac.setAttribute(TransparentAppConfig.adapterMapKey, adapterMap);

    t.initializeTestRunner(IS_MUTATE_VALIDATE_TEST);
  }

  protected Class getApplicationClass() {
    return ArrayMutateValidateTestApp.class;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveCrashMode.CRASH_AFTER_MUTATE);
    setupManager.setServerShareDataMode(ActivePassiveSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(ActivePassivePersistenceMode.TEMPORARY_SWAP_ONLY);
  }

  protected boolean canRunActivePassive() {
    return true;
  }

  public static class ArrayMutateValidateTestAppAdapter extends ClassAdapter {

    private String owner;

    public ArrayMutateValidateTestAppAdapter(ClassVisitor cv) {
      super(cv);
    }

    public void visitEnd() {
      super.visitField(Opcodes.ACC_PUBLIC, "foo", "Ljava/lang/String;", null, null);
      super.visitEnd();
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      super.visit(version, access, name, signature, superName, interfaces);
      owner = name;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);
      if (name.equals("accessFoo")) {
        visitor.visitCode();
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, owner, "foo", "Ljava/lang/String;");
        visitor.visitInsn(Opcodes.ARETURN);
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
        return null;
      }
      return visitor;
    }
  }
}
