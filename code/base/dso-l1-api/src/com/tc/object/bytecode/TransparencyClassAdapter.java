/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;
import com.tc.aspectwerkz.exception.DefinitionException;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.Portability;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.locks.LockLevel;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.Banner;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * @author steve
 */
public class TransparencyClassAdapter extends ClassAdapterBase implements TransparencyClassAdapterHack {
  private static final TCLogger            logger             = TCLogging.getLogger(TransparencyClassAdapter.class);
  private static final boolean             useFastFinalFields = TCPropertiesImpl
                                                                  .getProperties()
                                                                  .getBoolean(TCPropertiesConsts.INSTRUMENTATION_FINAL_FIELD_FAST_READ);

  private final Set                        doNotInstrument    = new HashSet();
  private final PhysicalClassAdapterLogger physicalClassLogger;
  private final InstrumentationLogger      instrumentationLogger;

  private boolean                          supportMethodsCreated;

  public TransparencyClassAdapter(final ClassInfo classInfo, final TransparencyClassSpec spec, final ClassVisitor cv,
                                  final InstrumentationLogger instrumentationLogger, final ClassLoader caller,
                                  final Portability portability) {
    super(classInfo, spec, cv, caller, portability);
    this.instrumentationLogger = instrumentationLogger;
    this.physicalClassLogger = new PhysicalClassAdapterLogger(logger);
  }

  @Override
  protected void basicVisit(final int version, final int access, final String name, final String signature,
                            final String superClassName, final String[] interfaces) {

    try {
      logger.debug("ADAPTING CLASS: " + name);
      super.basicVisit(version, access, name, signature, superClassName, interfaces);

      if (!supportMethodsCreated) {
        // We can re-enter visit() and we don't want to do this action twice
        supportMethodsCreated = true;
        getTransparencyClassSpec().createClassSupportMethods(cv);
      }
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
      throw e;
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private void handleInstrumentationException(final Throwable e) {
    logger.fatal(e);
    logger.fatal("Calling System.exit(1)");
    System.exit(1);
  }

  private boolean isRoot(final int access, final String fieldName) {
    try {
      FieldInfo fieldInfo = spec.getFieldInfo(fieldName);
      boolean isRoot = fieldInfo == null ? false : getTransparencyClassSpec().isRootInThisClass(fieldInfo);
      boolean isTransient = getTransparencyClassSpec().isTransient(access, spec.getClassInfo(), fieldName);
      if (isTransient && isRoot) {
        if (instrumentationLogger.getTransientRootWarning()) {
          instrumentationLogger.transientRootWarning(this.spec.getClassNameDots(), fieldName);
        }
      }
      return isRoot;
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
      throw e;
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private String rootNameFor(final String className, final String fieldName) {
    try {
      return getTransparencyClassSpec().rootNameFor(spec.getFieldInfo(fieldName));
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
      throw e;
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  @Override
  protected FieldVisitor basicVisitField(int access, final String name, final String desc, final String signature,
                                         final Object value) {
    try {
      if ((spec.isClassPortable() && spec.isPhysical() && !ByteCodeUtil.isTCSynthetic(name))
          || (spec.isClassAdaptable() && isRoot(access, name))) {
        if (Vm.isJRockit() && fieldCanBeFlushed(access, name, desc)) {
          access &= ~Modifier.FINAL;
        }
        generateGettersSetters(access, name, desc, Modifier.isStatic(access));
      }
    } catch (RuntimeException e) {
      e.printStackTrace();
      handleInstrumentationException(e);
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
    return cv.visitField(access, name, desc, signature, value);
  }

  private boolean fieldCanBeFlushed(int access, String name, String desc) {
    boolean isStatic = Modifier.isStatic(access);
    boolean isTransient = getTransparencyClassSpec().isTransient(access, spec.getClassInfo(), name);
    boolean isPrimitive = ByteCodeUtil.isPrimitive(Type.getType(desc));
    return (!isStatic && !isTransient && !isPrimitive) || isRoot(access, name);
  }

  private void generateGettersSetters(final int fieldAccess, final String name, final String desc,
                                      final boolean isStatic) {
    boolean isTransient = getTransparencyClassSpec().isTransient(fieldAccess, spec.getClassInfo(), name);
    // Plain getter and setters are generated for transient fields as other instrumented classes might call them.
    boolean createPlainAccessors = isTransient && !isStatic;
    boolean createInstrumentedAccessors = !isTransient && !isStatic;
    boolean createRootAccessors = isRoot(fieldAccess, name);

    int methodAccess = fieldAccess & (~ACC_TRANSIENT);
    methodAccess &= (~ACC_FINAL); // remove final modifier since variable might be shadowed
    methodAccess &= (~ACC_VOLATILE);
    methodAccess |= ACC_SYNTHETIC;

    if (createRootAccessors) {
      createRootGetter(methodAccess, name, desc);
    } else if (createInstrumentedAccessors) {
      if (!ByteCodeUtil.isPrimitive(Type.getType(desc))) {
        createInstrumentedGetter(methodAccess, fieldAccess, name, desc);
      } else {
        createPlainGetter(methodAccess, fieldAccess, name, desc);
      }
    } else if (createPlainAccessors) {
      createPlainGetter(methodAccess, fieldAccess, name, desc);
    }

    if (createInstrumentedAccessors || createRootAccessors) {
      createInstrumentedSetter(methodAccess, fieldAccess, name, desc);
    } else if (createPlainAccessors) {
      createPlainSetter(methodAccess, fieldAccess, name, desc);
    }
  }

  private boolean isPrimitive(final Type t) {
    return ByteCodeUtil.isPrimitive(t);
  }

  private MethodVisitor ignoreMethodIfNeeded(final int access, final String name, final String desc,
                                             final String signature, final String[] exceptions,
                                             final MemberInfo memberInfo) {
    if (name.startsWith(ByteCodeUtil.TC_METHOD_PREFIX) || doNotInstrument.contains(name + desc)
        || getTransparencyClassSpec().doNotInstrument(name)) {
      if (!getTransparencyClassSpec().hasCustomMethodAdapter(memberInfo)) {
        physicalClassLogger.logVisitMethodIgnoring(name, desc);
        return cv.visitMethod(access, name, desc, signature, exceptions);
      }
    }
    return null;
  }

  @Override
  protected MethodVisitor basicVisitMethod(int access, String name, final String desc, final String signature,
                                           final String[] exceptions) {
    String originalName = name;
    MethodVisitor mv = null;

    try {
      physicalClassLogger.logVisitMethodBegin(access, name, desc, signature, exceptions);

      MemberInfo memberInfo = getInstrumentationSpec().getMethodInfo(access, name, desc);

      mv = ignoreMethodIfNeeded(access, name, desc, signature, exceptions, memberInfo);
      if (mv != null) { return mv; }

      LockDefinition[] locks = getTransparencyClassSpec().lockDefinitionsFor(memberInfo);
      LockDefinition ld = getTransparencyClassSpec().getAutoLockDefinition(locks);
      boolean isAutolock = (ld != null);
      int lockLevel = -1;
      if (isAutolock) {
        lockLevel = ld.getLockLevelAsInt();
        if (instrumentationLogger.getLockInsertion()) {
          instrumentationLogger.autolockInserted(this.spec.getClassNameDots(), name, desc, ld);
        }
      }
      boolean isAutoReadLock = isAutolock && (lockLevel == LockLevel.READ.toInt());

      if (isAutoSynchronized(ld) && !"<init>".equals(name)) {
        access |= ACC_SYNCHRONIZED;
      }

      boolean isLockMethod = isAutolock && Modifier.isSynchronized(access) && !Modifier.isStatic(access);
      physicalClassLogger.logVisitMethodCheckIsLockMethod();

      if (!isLockMethod || spec.isClassAdaptable()) {
        isLockMethod = (getTransparencyClassSpec().getNonAutoLockDefinition(locks) != null);
      }

      // handle lock method by re-writing the original method as a wrapper method and rename the original method.
      if (isLockMethod && !"<init>".equals(name)) {
        physicalClassLogger.logVisitMethodCreateLockMethod(name);
        // This method is a lock method.
        Assert.assertNotNull(locks);
        Assert.eval(locks.length > 0 || isLockMethod);
        createLockMethod(access, name, desc, signature, exceptions, locks, isAutoReadLock);

        logCustomerLockMethod(name, desc, locks);
        name = ByteCodeUtil.METHOD_RENAME_PREFIX + name;
        access |= ACC_PRIVATE;
        access &= (~ACC_PUBLIC);
        access &= (~ACC_PROTECTED);
        if (isAutoReadLock) {
          access &= (~ACC_SYNCHRONIZED);
        }
      } else {
        physicalClassLogger.logVisitMethodNotALockMethod(access, this.spec.getClassNameDots(), name, desc, exceptions);
      }

      // Visit the original method by either using a custom adapter or a TransparencyCodeAdapter or both.
      if (getTransparencyClassSpec().hasCustomMethodAdapter(memberInfo)) {
        MethodAdapter ma = getTransparencyClassSpec().customMethodAdapterFor(access, name, originalName, desc,
                                                                             signature, exceptions,
                                                                             instrumentationLogger, memberInfo);
        mv = ma.adapt(cv);

        if (!ma.doesOriginalNeedAdapting()) return mv;
      }

      if (mv == null) {
        mv = cv.visitMethod(access, name, desc, signature, exceptions);
      }

      // return mv == null ? null : new TransparencyCodeAdapter(spec, isAutolock, lockLevel, mv, memberInfo,
      // originalName);
      return mv == null ? null : new TransparencyCodeAdapter(spec, ld, mv, memberInfo, originalName);
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
      throw e;
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private boolean isAutoSynchronized(final LockDefinition ld) {
    if (ld == null) { return false; }

    ConfigLockLevel lockLevel = ld.getLockLevel();
    return ConfigLockLevel.AUTO_SYNCHRONIZED_READ.equals(lockLevel)
           || ConfigLockLevel.AUTO_SYNCHRONIZED_WRITE.equals(lockLevel)
           || ConfigLockLevel.AUTO_SYNCHRONIZED_CONCURRENT.equals(lockLevel)
           || ConfigLockLevel.AUTO_SYNCHRONIZED_SYNCHRONOUS_WRITE.equals(lockLevel);
  }

  // protected void basicVisitEnd() {
  // // if adaptee has DMI
  // boolean hasCustomMethodAdapter = getTransparencyClassSpec().hasCustomMethodAdapter(access, originalName, desc,
  // exceptions);
  // super.basicVisitEnd();
  // }

  private void logCustomerLockMethod(final String name, final String desc, final LockDefinition[] locks) {
    if (instrumentationLogger.getLockInsertion()) {
      instrumentationLogger.lockInserted(this.spec.getClassNameDots(), name, desc, locks);
    }
  }

  private void createLockMethod(int access, final String name, final String desc, final String signature,
                                final String[] exceptions, final LockDefinition[] locks, final boolean skipLocalJVMLock) {
    try {
      physicalClassLogger.logCreateLockMethodBegin(access, name, desc, signature, exceptions, locks);
      doNotInstrument.add(name + desc);
      recreateMethod(access, name, desc, signature, exceptions, locks, skipLocalJVMLock);
      if (skipLocalJVMLock) {
        access |= ACC_PRIVATE;
        access &= (~ACC_PUBLIC);
        access &= (~ACC_PROTECTED);

        createSyncMethod(access, name, desc, signature, exceptions);
      }

    } catch (RuntimeException e) {
      handleInstrumentationException(e);
      throw e;
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private String getTCSyncMethodName(final String name) {
    return ByteCodeUtil.SYNC_METHOD_RENAME_PREFIX + name;
  }

  private void createSyncMethod(final int access, final String name, final String desc, final String signature,
                                final String[] exceptions) {
    Type returnType = Type.getReturnType(desc);
    // access should have the synchronized modifier
    MethodVisitor mv = cv.visitMethod(access, getTCSyncMethodName(name), desc, signature, exceptions);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    callRenamedMethod(access & (~Modifier.SYNCHRONIZED), name, desc, mv);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitInsn(returnType.getOpcode(IRETURN));
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLocalVariable("this", "L" + spec.getClassNameSlashes() + ";", null, l0, l2, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void recreateMethod(final int access, final String name, final String desc, final String signature,
                              final String[] exceptions, final LockDefinition[] locks, final boolean skipLocalJVMLock) {
    Type returnType = Type.getReturnType(desc);
    physicalClassLogger.logCreateLockMethodVoidBegin(access, name, desc, signature, exceptions, locks);
    MethodVisitor c = cv.visitMethod(access & (~Modifier.SYNCHRONIZED), name, desc, signature, exceptions);

    Label l1 = new Label();
    if (skipLocalJVMLock) {
      ByteCodeUtil.pushThis(c);
      c.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isDsoMonitored", "(Ljava/lang/Object;)Z");
      c.visitJumpInsn(IFEQ, l1);
    }

    if (returnType.getSort() == Type.VOID) {
      addDsoLockMethodInsnVoid(access, name, desc, signature, exceptions, locks, c);
    } else {
      addDsoLockMethodInsnReturn(access, name, desc, signature, exceptions, locks, returnType, c);
    }

    if (skipLocalJVMLock) {
      c.visitLabel(l1);
      // access should have the synchronized modifier
      callRenamedMethod(access, "sync" + "_" + name, desc, c);

      c.visitInsn(returnType.getOpcode(IRETURN));
    }

    c.visitMaxs(0, 0);
    c.visitEnd();
  }

  private int addBooleanLocalVariablesIfMoreThanOneLock(final int access, final String desc,
                                                        final LockDefinition[] locks, final MethodVisitor c,
                                                        final int[] localBooleanVariables) {
    int nextLocalVariable = ByteCodeUtil.getFirstLocalVariableOffset(access, desc);
    if (locks.length > 1) {
      for (int i = 0; i < locks.length; i++) {
        localBooleanVariables[i] = nextLocalVariable;
        ByteCodeUtil.pushDefaultValue(localBooleanVariables[i], c, Type.BOOLEAN_TYPE);
        nextLocalVariable += Type.BOOLEAN_TYPE.getSize();
      }
    }
    return nextLocalVariable;
  }

  private void startDsoLockTryBlock(final int access, final String name, final String desc,
                                    final LockDefinition[] locks, final MethodVisitor c,
                                    final int[] localBooleanVariables, final Label startTryBlockLabel) {
    if (locks.length > 1) {
      c.visitLabel(startTryBlockLabel);
      callTCBeginWithLocks(access, name, desc, locks, c, localBooleanVariables);
    } else {
      callTCBeginWithLocks(access, name, desc, locks, c, localBooleanVariables);
      c.visitLabel(startTryBlockLabel);
    }
  }

  /**
   * Creates a tc lock method for the given method that returns void.
   */
  private void addDsoLockMethodInsnVoid(final int access, final String name, final String desc, final String signature,
                                        final String[] exceptions, final LockDefinition[] locks, final MethodVisitor c) {
    int[] localBooleanVariables = new int[locks.length];
    int localVariableOffset = addBooleanLocalVariablesIfMoreThanOneLock(access, desc, locks, c, localBooleanVariables);

    try {

      Label l0 = new Label();
      startDsoLockTryBlock(access, name, desc, locks, c, localBooleanVariables, l0);
      callRenamedMethod(access, name, desc, c);
      // This label creation has something to do with try/finally
      Label l1 = new Label();
      c.visitJumpInsn(GOTO, l1);
      Label l2 = new Label();
      c.visitLabel(l2);
      c.visitVarInsn(ASTORE, 1 + localVariableOffset);
      Label l3 = new Label();
      c.visitJumpInsn(JSR, l3);
      c.visitVarInsn(ALOAD, 1 + localVariableOffset);
      c.visitInsn(ATHROW);
      c.visitLabel(l3);
      c.visitVarInsn(ASTORE, 0 + localVariableOffset);
      callTCCommit(access, name, desc, locks, c, localBooleanVariables);
      c.visitVarInsn(RET, 0 + localVariableOffset);
      c.visitLabel(l1);
      c.visitJumpInsn(JSR, l3);
      Label l4 = new Label();
      c.visitLabel(l4);
      c.visitInsn(RETURN);
      c.visitTryCatchBlock(l0, l2, l2, null);
      c.visitTryCatchBlock(l1, l4, l2, null);
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private void handleInstrumentationException(final RuntimeException e) {
    // XXX: Yucky.
    if (e instanceof DefinitionException) {
      logger.fatal(e.getLocalizedMessage());
    } else {
      logger.fatal(e);
    }

    e.printStackTrace(System.err);
    System.err.flush();
    String msg = "Error detected -- Calling System.exit(1)";
    Banner.errorBanner(msg);

    logger.fatal(msg);
    System.exit(1);
  }

  private void callRenamedMethod(final int callingMethodModifier, final String name, final String desc,
                                 final MethodVisitor c) {
    // Call the renamed original method.
    ByteCodeUtil.prepareStackForMethodCall(callingMethodModifier, desc, c);
    if (Modifier.isStatic(callingMethodModifier)) {
      c.visitMethodInsn(INVOKESTATIC, spec.getClassNameSlashes(), ByteCodeUtil.METHOD_RENAME_PREFIX + name, desc);
    } else {
      c.visitMethodInsn(INVOKESPECIAL, spec.getClassNameSlashes(), ByteCodeUtil.METHOD_RENAME_PREFIX + name, desc);
    }
  }

  /**
   * Creates a tc lock method for the given method that returns a value (doesn't return void).
   */
  private void addDsoLockMethodInsnReturn(final int access, final String name, final String desc,
                                          final String signature, final String[] exceptions,
                                          final LockDefinition[] locks, final Type returnType, final MethodVisitor c) {
    int[] localBooleanVariables = new int[locks.length];
    int localVariableOffset = addBooleanLocalVariablesIfMoreThanOneLock(access, desc, locks, c, localBooleanVariables);

    try {
      Label l0 = new Label();
      startDsoLockTryBlock(access, name, desc, locks, c, localBooleanVariables, l0);
      callRenamedMethod(access, name, desc, c);
      c.visitVarInsn(returnType.getOpcode(ISTORE), 2 + localVariableOffset);
      Label l1 = new Label();
      c.visitJumpInsn(JSR, l1);
      Label l2 = new Label();
      c.visitLabel(l2);
      c.visitVarInsn(returnType.getOpcode(ILOAD), 2 + localVariableOffset);
      c.visitInsn(returnType.getOpcode(IRETURN));
      Label l3 = new Label();
      c.visitLabel(l3);
      c.visitVarInsn(ASTORE, 1 + localVariableOffset);
      c.visitJumpInsn(JSR, l1);
      c.visitVarInsn(ALOAD, 1 + localVariableOffset);
      c.visitInsn(ATHROW);
      c.visitLabel(l1);
      c.visitVarInsn(ASTORE, 0 + localVariableOffset);
      callTCCommit(access, name, desc, locks, c, localBooleanVariables);
      c.visitVarInsn(RET, 0 + localVariableOffset);
      c.visitTryCatchBlock(l0, l2, l3, null);
      // c.visitMaxs(0, 0);
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }

  }

  private void callTCBeginWithLocks(final int access, final String name, final String desc,
                                    final LockDefinition[] locks, final MethodVisitor c,
                                    final int[] localBooleanVariables) {
    physicalClassLogger.logCallTCBeginWithLocksStart(access, name, desc, locks, c);
    for (int i = 0; i < locks.length; i++) {
      LockDefinition lock = locks[i];
      if (lock.isAutolock() && spec.isClassPortable()) {
        physicalClassLogger.logCallTCBeginWithLocksAutolock();
        if (Modifier.isSynchronized(access) && !Modifier.isStatic(access)) {
          physicalClassLogger.logCallTCBeginWithLocksAutolockSynchronized(name, desc);
          callTCMonitorEnter(access, locks[i], c);
        } else {
          physicalClassLogger.logCallTCBeginWithLocksAutolockNotSynchronized(name, desc);
        }
      } else if (!lock.isAutolock()) {
        physicalClassLogger.logCallTCBeginWithLocksNoAutolock(lock);
        callTCBeginWithLock(lock, c);
      }
      if (locks.length > 1) {
        c.visitInsn(ICONST_1);
        c.visitVarInsn(ISTORE, localBooleanVariables[i]);
      }
    }
  }

  private void callTCCommit(final int access, final String name, final String desc, final LockDefinition[] locks,
                            final MethodVisitor c, final int[] localBooleanVariables) {
    physicalClassLogger.logCallTCCommitBegin(access, name, desc, locks, c);
    Label returnLabel = new Label();
    for (int i = 0; i < locks.length; i++) {
      if (locks.length > 1) {
        c.visitVarInsn(ILOAD, localBooleanVariables[i]);
        c.visitJumpInsn(IFEQ, returnLabel);
      }
      LockDefinition lock = locks[i];
      if (lock.isAutolock() && spec.isClassPortable()) {
        if (Modifier.isSynchronized(access) && !Modifier.isStatic(access)) {
          callTCMonitorExit(access, lock, c);
        }
      } else if (!lock.isAutolock()) {
        c.visitLdcInsn(ByteCodeUtil.generateNamedLockName(lock.getLockName()));
        c.visitLdcInsn(new Integer(lock.getLockLevelAsInt()));
        c.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "commitLock", "(Ljava/lang/String;I)V");
      }
    }
    c.visitLabel(returnLabel);
  }

  private void callTCCommitWithLockName(final String lockName, final int type, final MethodVisitor mv) {
    mv.visitLdcInsn(lockName);
    mv.visitLdcInsn(new Integer(type));
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "commitLock", "(Ljava/lang/String;I)V");
  }

  private void callTCBeginWithLock(final LockDefinition lock, final MethodVisitor c) {
    c.visitLdcInsn(ByteCodeUtil.generateNamedLockName(lock.getLockName()));
    c.visitLdcInsn(new Integer(lock.getLockLevelAsInt()));
    c.visitLdcInsn(lock.getLockContextInfo());
    c.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "beginLock", "(Ljava/lang/String;ILjava/lang/String;)V");
  }

  private void callTCBeginWithLockName(final String lockName, final int lockLevel, final MethodVisitor mv) {
    mv.visitLdcInsn(lockName);
    mv.visitLdcInsn(new Integer(lockLevel));
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "beginLock", "(Ljava/lang/String;I)V");
  }

  private void callVolatileBegin(final String fieldName, final int lockLevel, final MethodVisitor mv) {
    getManaged(mv);
    mv.visitLdcInsn(fieldName);
    mv.visitIntInsn(BIPUSH, lockLevel);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "beginVolatile",
                       "(Lcom/tc/object/TCObjectExternal;Ljava/lang/String;I)V");
  }

  private void callVolatileCommit(final String fieldName, final int lockLevel, final MethodVisitor mv) {
    getManaged(mv);
    mv.visitLdcInsn(fieldName);
    mv.visitIntInsn(BIPUSH, lockLevel);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "commitVolatile",
                       "(Lcom/tc/object/TCObjectExternal;Ljava/lang/String;I)V");
  }

  private void createPlainGetter(final int methodAccess, final int fieldAccess, final String name, final String desc) {
    boolean isVolatile = isVolatile(fieldAccess, name);

    String gDesc = "()" + desc;
    MethodVisitor gv = this.visitMethod(methodAccess, ByteCodeUtil.fieldGetterMethod(name), gDesc, null, null);
    Type t = Type.getType(desc);

    Label l4 = new Label();

    if (isVolatile) {
      getManaged(gv);
      gv.visitInsn(DUP);
      gv.visitVarInsn(ASTORE, 2);
      gv.visitJumpInsn(IFNULL, l4);

      Label l0 = new Label();
      Label l1 = new Label();
      Label l2 = new Label();
      gv.visitTryCatchBlock(l0, l1, l2, null);
      gv.visitLabel(l0);

      callVolatileBegin(spec.getClassNameDots() + "." + name, LockLevel.READ.toInt(), gv);

      Label l6 = new Label();
      gv.visitJumpInsn(JSR, l6);
      gv.visitLabel(l1);
      ByteCodeUtil.pushThis(gv);
      gv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), name, desc);
      gv.visitInsn(t.getOpcode(IRETURN));
      gv.visitLabel(l2);
      gv.visitVarInsn(ASTORE, 2);
      gv.visitJumpInsn(JSR, l6);
      gv.visitVarInsn(ALOAD, 2);
      gv.visitInsn(ATHROW);
      gv.visitLabel(l6);
      gv.visitVarInsn(ASTORE, 1);

      callVolatileCommit(spec.getClassNameDots() + "." + name, LockLevel.READ.toInt(), gv);
      gv.visitVarInsn(RET, 1);
    }

    gv.visitLabel(l4);
    ByteCodeUtil.pushThis(gv);
    gv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), name, desc);
    gv.visitInsn(t.getOpcode(IRETURN));

    gv.visitMaxs(0, 0);
    gv.visitEnd();
  }

  private void checkReturnObjectType(final String fieldName, final String rootName, final String targetType,
                                     final int loadVariableNumber, final Label matchLabel, final MethodVisitor mv) {
    mv.visitVarInsn(ALOAD, loadVariableNumber);
    mv.visitTypeInsn(INSTANCEOF, targetType);
    mv.visitJumpInsn(IFNE, matchLabel);
    mv.visitTypeInsn(NEW, "java/lang/ClassCastException");
    mv.visitInsn(DUP);
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("The field '");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    mv.visitLdcInsn(fieldName);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitLdcInsn("' with root name '");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitLdcInsn(rootName);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitLdcInsn("' cannot be assigned to a variable of type ");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitLdcInsn(targetType);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitLdcInsn(". This root has a type ");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitVarInsn(ALOAD, loadVariableNumber);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitLdcInsn(". ");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitLdcInsn("Perhaps you have the same root name assigned more than once to variables of different types.");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassCastException", "<init>", "(Ljava/lang/String;)V");
    mv.visitInsn(ATHROW);

  }

  private void createRootGetter(final int methodAccess, final String name, final String desc) {
    Type t = Type.getType(desc);
    boolean isPrimitive = isPrimitive(t);
    boolean isDSOFinal = isRootDSOFinal(name);

    String rootName = rootNameFor(spec.getClassNameSlashes(), name);
    String targetType = isPrimitive ? ByteCodeUtil.sortToWrapperName(t.getSort()) : convertToCheckCastDesc(desc);

    boolean isStatic = Modifier.isStatic(methodAccess);

    Label l1 = new Label();
    Label l3 = new Label();
    Label l5 = new Label();
    Label l6 = new Label();
    Label l7 = new Label();
    Label l8 = new Label();

    try {
      MethodVisitor mv = cv.visitMethod(methodAccess, ByteCodeUtil.fieldGetterMethod(name), "()" + desc, null, null);

      if (isDSOFinal) {
        callGetFieldInsn(isStatic, name, desc, mv);
        if (isPrimitive) {
          addPrimitiveTypeZeroCompare(mv, t, l1);
        } else {
          mv.visitJumpInsn(IFNONNULL, l1);
        }
      }

      callTCBeginWithLockName(rootName, LockLevel.WRITE.toInt(), mv);

      mv.visitLabel(l3);
      mv.visitLdcInsn(rootName);
      mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "lookupRoot", "(Ljava/lang/String;)Ljava/lang/Object;");
      mv.visitVarInsn(ASTORE, 1);

      mv.visitVarInsn(ALOAD, 1);
      mv.visitJumpInsn(IFNULL, l5);

      checkReturnObjectType(name, rootName, targetType, 1, l6, mv);

      mv.visitLabel(l6);
      callPutFieldInsn(isStatic, t, 1, name, desc, mv);
      mv.visitJumpInsn(GOTO, l5);

      mv.visitLabel(l7);
      mv.visitVarInsn(ASTORE, 3);
      mv.visitJumpInsn(JSR, l8);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitInsn(ATHROW);

      mv.visitLabel(l8);
      mv.visitVarInsn(ASTORE, 2);
      callTCCommitWithLockName(rootName, LockLevel.WRITE.toInt(), mv);
      mv.visitVarInsn(RET, 2);

      mv.visitLabel(l5);
      mv.visitJumpInsn(JSR, l8);

      mv.visitLabel(l1);
      callGetFieldInsn(isStatic, name, desc, mv);
      mv.visitInsn(t.getOpcode(IRETURN));
      mv.visitTryCatchBlock(l3, l7, l7, null);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private boolean isVolatile(final int access, final String fieldName) {
    return getTransparencyClassSpec().isVolatile(access, spec.getClassInfo(), fieldName);
  }

  private void createInstrumentedGetter(final int methodAccess, final int fieldAccess, final String name,
                                        final String desc) {
    Assert.eval(!getTransparencyClassSpec().isLogical());
    MethodVisitor gv = this.visitMethod(methodAccess, ByteCodeUtil.fieldGetterMethod(name), "()" + desc, null, null);
    if (useFastFinalFields && Modifier.isFinal(fieldAccess)) {
      createInstrumentedFinalGetter(gv, fieldAccess, name, desc);
    } else {
      createInstrumentedNonFinalGetter(gv, fieldAccess, name, desc);
    }
  }

  private void createInstrumentedNonFinalGetter(final MethodVisitor gv, final int fieldAccess, final String name,
                                                final String desc) {
    try {
      final int THIS_SLOT = 0;
      final int RESOLVE_LOCK_SLOT = 2;
      final int TC_OBJECT_SLOT = 3;

      boolean isVolatile = isVolatile(fieldAccess, name);

      Type fieldType = Type.getType(desc);

      Label syncBegin = new Label();
      Label syncEnd = new Label();

      Label notManaged = new Label();
      Label resolved = new Label();

      // Check if `this' is managed
      getManaged(gv);
      gv.visitInsn(DUP);
      gv.visitVarInsn(ASTORE, TC_OBJECT_SLOT);
      gv.visitJumpInsn(IFNULL, notManaged);

      // lock appropriate locks (the resolve lock and possibly the volatile lock)
      if (isVolatile) {
        callVolatileBegin(spec.getClassNameDots() + '.' + name, LockLevel.READ.toInt(), gv);
      }
      gv.visitVarInsn(ALOAD, TC_OBJECT_SLOT);
      gv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "getResolveLock", "()Ljava/lang/Object;");
      gv.visitInsn(DUP);
      gv.visitVarInsn(ASTORE, RESOLVE_LOCK_SLOT);
      gv.visitInsn(MONITORENTER);
      gv.visitLabel(syncBegin);

      // check if field is null under resolve lock (null means we must resolve)
      gv.visitVarInsn(ALOAD, THIS_SLOT);
      gv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), name, desc);
      gv.visitJumpInsn(IFNONNULL, resolved);

      // resolve the reference (possibly talk to the server)
      gv.visitVarInsn(ALOAD, TC_OBJECT_SLOT);
      gv.visitLdcInsn(spec.getClassNameDots() + '.' + name);
      gv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "resolveReference", "(Ljava/lang/String;)V");

      // read the resolved field
      gv.visitLabel(resolved);
      gv.visitVarInsn(ALOAD, THIS_SLOT);
      gv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), name, desc);

      // unlock appropriate locks and return
      gv.visitVarInsn(ALOAD, RESOLVE_LOCK_SLOT);
      gv.visitInsn(MONITOREXIT);
      if (isVolatile) {
        callVolatileCommit(spec.getClassNameDots() + "." + name, LockLevel.READ.toInt(), gv);
      }
      gv.visitInsn(fieldType.getOpcode(IRETURN));
      gv.visitLabel(syncEnd);

      // unlock appropriate locks and throw exception (exception handler)
      gv.visitVarInsn(ALOAD, RESOLVE_LOCK_SLOT);
      gv.visitInsn(MONITOREXIT);
      if (isVolatile) {
        callVolatileCommit(spec.getClassNameDots() + "." + name, LockLevel.READ.toInt(), gv);
      }
      gv.visitInsn(ATHROW);

      // `this' instance is not managed - read the field normally
      gv.visitLabel(notManaged);
      gv.visitVarInsn(ALOAD, THIS_SLOT);
      gv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), name, desc);
      gv.visitInsn(fieldType.getOpcode(IRETURN));

      gv.visitTryCatchBlock(syncBegin, syncEnd, syncEnd, null);
      gv.visitMaxs(0, 0);
      gv.visitEnd();
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private void createInstrumentedFinalGetter(final MethodVisitor gv, final int fieldAccess, final String name,
                                             final String desc) {
    try {
      final int THIS_SLOT = 0;

      Type fieldType = Type.getType(desc);
      Label regularGet = new Label();

      // Do a dirty read of the variable
      gv.visitVarInsn(ALOAD, THIS_SLOT);
      gv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), name, desc);
      gv.visitInsn(DUP);
      gv.visitJumpInsn(IFNULL, regularGet);
      gv.visitInsn(fieldType.getOpcode(IRETURN));

      gv.visitLabel(regularGet);
      gv.visitInsn(POP);
      createInstrumentedNonFinalGetter(gv, fieldAccess, name, desc);
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private void getManaged(final MethodVisitor mv) {
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, spec.getClassNameSlashes(), MANAGED_METHOD, "()" + MANAGED_FIELD_TYPE);
  }

  private void createPlainSetter(final int methodAccess, final int fieldAccess, final String name, final String desc) {
    boolean isVolatile = isVolatile(fieldAccess, name);

    String sDesc = "(" + desc + ")V";
    MethodVisitor scv = cv.visitMethod(methodAccess, ByteCodeUtil.fieldSetterMethod(name), sDesc, null, null);
    Type t = Type.getType(desc);

    Label l4 = new Label();

    if (isVolatile) {
      getManaged(scv);
      scv.visitInsn(DUP);
      scv.visitVarInsn(ASTORE, 2);
      scv.visitJumpInsn(IFNULL, l4);

      Label l0 = new Label();
      Label l1 = new Label();
      Label l2 = new Label();
      scv.visitTryCatchBlock(l0, l1, l2, null);
      scv.visitLabel(l0);

      callVolatileBegin(spec.getClassNameDots() + "." + name, LockLevel.WRITE.toInt(), scv);

      Label l6 = new Label();
      scv.visitJumpInsn(JSR, l6);
      scv.visitLabel(l1);
      ByteCodeUtil.pushThis(scv);
      scv.visitVarInsn(t.getOpcode(ILOAD), 1);

      scv.visitFieldInsn(PUTFIELD, spec.getClassNameSlashes(), name, desc);
      scv.visitInsn(RETURN);
      scv.visitLabel(l2);
      scv.visitVarInsn(ASTORE, 2);
      scv.visitJumpInsn(JSR, l6);
      scv.visitVarInsn(ALOAD, 2);
      scv.visitInsn(ATHROW);
      scv.visitLabel(l6);
      scv.visitVarInsn(ASTORE, 1);
      callVolatileCommit(spec.getClassNameDots() + "." + name, LockLevel.WRITE.toInt(), scv);

      scv.visitVarInsn(RET, 1);
    }

    scv.visitLabel(l4);
    ByteCodeUtil.pushThis(scv);
    scv.visitVarInsn(t.getOpcode(ILOAD), 1);

    scv.visitFieldInsn(PUTFIELD, spec.getClassNameSlashes(), name, desc);
    scv.visitInsn(RETURN);
    scv.visitMaxs(0, 0);
    scv.visitEnd();
  }

  private void createInstrumentedSetter(final int methodAccess, final int fieldAccess, final String name,
                                        final String desc) {
    try {
      Type t = Type.getType(desc);
      if (isRoot(methodAccess, name)) {
        createObjectSetter(methodAccess, fieldAccess, name, desc);
      }
      // if (((t.getSort() == Type.OBJECT) || (t.getSort() == Type.ARRAY)) && !isLiteral(desc)) {
      else if (((t.getSort() == Type.OBJECT) || (t.getSort() == Type.ARRAY)) && !isPrimitive(t)) {
        createObjectSetter(methodAccess, fieldAccess, name, desc);
      } else {
        createLiteralSetter(methodAccess, fieldAccess, name, desc);
      }
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private void createObjectSetter(final int methodAccess, final int fieldAccess, final String name, final String desc) {
    try {
      if (isRoot(methodAccess, name)) {
        boolean isStaticRoot = Modifier.isStatic(methodAccess);
        if (instrumentationLogger.getRootInsertion()) {
          instrumentationLogger.rootInserted(spec.getClassNameDots(), name, desc, isStaticRoot);
        }

        createRootSetter(methodAccess, name, desc, isStaticRoot);
      } else {
        createObjectFieldSetter(methodAccess, fieldAccess, name, desc);
      }
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private boolean isRootDSOFinal(final String name) {
    return spec.getTransparencyClassSpec().isRootDSOFinal(spec.getFieldInfo(name));
  }

  private void createRootSetter(final int methodAccess, final String name, final String desc, final boolean isStatic) {
    Type t = Type.getType(desc);
    boolean isPrimitive = isPrimitive(t);
    boolean isDSOFinal = isRootDSOFinal(name);

    try {
      String sDesc = "(" + desc + ")V";
      String targetType = isPrimitive ? ByteCodeUtil.sortToWrapperName(t.getSort()) : convertToCheckCastDesc(desc);
      MethodVisitor scv = cv.visitMethod(methodAccess, ByteCodeUtil.fieldSetterMethod(name), sDesc, null, null);

      Label tryStart = new Label();
      Label end = new Label();
      Label normalExit = new Label();
      Label finallyStart = new Label();
      Label exceptionHandler = new Label();

      final int rootInstance = isStatic ? 0 : 1;

      if (!isPrimitive) {
        scv.visitVarInsn(ALOAD, rootInstance);
        scv.visitJumpInsn(IFNULL, end); // Always ignore request to set roots to null
      }

      String rootName = rootNameFor(spec.getClassNameSlashes(), name);
      callTCBeginWithLockName(rootName, LockLevel.WRITE.toInt(), scv);

      scv.visitLabel(tryStart);

      scv.visitLdcInsn(rootName);
      if (isPrimitive) {
        ByteCodeUtil.addTypeSpecificParameterLoad(scv, t, rootInstance);
      } else {
        scv.visitVarInsn(ALOAD, rootInstance);
      }
      if (isDSOFinal) {
        scv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "lookupOrCreateRoot",
                            "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
      } else {
        scv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "createOrReplaceRoot",
                            "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
      }

      int localVar = rootInstance + 1;
      scv.visitVarInsn(ASTORE, localVar);

      Label l0 = new Label();
      checkReturnObjectType(name, rootName, targetType, localVar, l0, scv);

      scv.visitLabel(l0);
      callPutFieldInsn(isStatic, t, localVar, name, desc, scv);
      scv.visitJumpInsn(GOTO, normalExit);

      scv.visitLabel(exceptionHandler);
      scv.visitVarInsn(ASTORE, 3);
      scv.visitJumpInsn(JSR, finallyStart);
      scv.visitVarInsn(ALOAD, 3);
      scv.visitInsn(ATHROW);

      scv.visitLabel(finallyStart);
      scv.visitVarInsn(ASTORE, 2);
      callTCCommitWithLockName(rootName, LockLevel.WRITE.toInt(), scv);
      scv.visitVarInsn(RET, 2);

      scv.visitLabel(normalExit);
      scv.visitJumpInsn(JSR, finallyStart);
      scv.visitLabel(end);
      scv.visitInsn(RETURN);
      scv.visitTryCatchBlock(tryStart, exceptionHandler, exceptionHandler, null);
      scv.visitMaxs(0, 0);
      scv.visitEnd();
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private void callGetFieldInsn(final boolean isStatic, final String name, final String desc, final MethodVisitor mv) {
    int getInsn = isStatic ? GETSTATIC : GETFIELD;

    if (!isStatic) ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(getInsn, spec.getClassNameSlashes(), name, desc);
  }

  private void callPutFieldInsn(final boolean isStatic, final Type targetType, final int localVar, final String name,
                                final String desc, final MethodVisitor mv) {
    int putInsn = isStatic ? PUTSTATIC : PUTFIELD;

    if (!isStatic) ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, localVar);

    if (isPrimitive(targetType)) {
      mv.visitTypeInsn(CHECKCAST, ByteCodeUtil.sortToWrapperName(targetType.getSort()));
      mv.visitMethodInsn(INVOKEVIRTUAL, ByteCodeUtil.sortToWrapperName(targetType.getSort()),
                         ByteCodeUtil.sortToPrimitiveMethodName(targetType.getSort()), "()" + desc);
    } else {
      mv.visitTypeInsn(CHECKCAST, convertToCheckCastDesc(desc));
    }

    mv.visitFieldInsn(putInsn, spec.getClassNameSlashes(), name, desc);
  }

  private void generateCodeForVolatileTransactionBegin(final Label l1, final Label l2, final Label l3, final Label l4,
                                                       final String fieldName, final int lockLevel,
                                                       final MethodVisitor scv) {
    scv.visitTryCatchBlock(l4, l1, l1, null);
    scv.visitTryCatchBlock(l2, l3, l1, null);
    scv.visitLabel(l4);
    callVolatileBegin(fieldName, lockLevel, scv);
  }

  private void generateCodeForVolativeTransactionCommit(final Label l1, final Label l2, final MethodVisitor scv,
                                                        final int newVar1, final int newVar2, final String fieldName,
                                                        final int lockLevel) {
    scv.visitJumpInsn(GOTO, l2);
    scv.visitLabel(l1);
    scv.visitVarInsn(ASTORE, newVar2);
    Label l5 = new Label();
    scv.visitJumpInsn(JSR, l5);
    scv.visitVarInsn(ALOAD, newVar2);
    scv.visitInsn(ATHROW);
    scv.visitLabel(l5);
    scv.visitVarInsn(ASTORE, newVar1);
    callVolatileCommit(fieldName, lockLevel, scv);
    scv.visitVarInsn(RET, newVar1);
    scv.visitLabel(l2);
    scv.visitJumpInsn(JSR, l5);
  }

  private boolean isInjected(final String fieldName) {
    return getTransparencyClassSpec().isInjectedField(fieldName);
  }

  private void createObjectFieldSetter(final int methodAccess, final int fieldAccess, final String name,
                                       final String desc) {
    try {
      boolean isVolatile = isVolatile(fieldAccess, name);
      Label l1 = new Label();
      Label l2 = new Label();
      Label l4 = new Label();

      // Generates setter method
      String sDesc = "(" + desc + ")V";
      MethodVisitor scv = cv.visitMethod(methodAccess, ByteCodeUtil.fieldSetterMethod(name), sDesc, null, null);
      getManaged(scv);
      scv.visitInsn(DUP);
      scv.visitVarInsn(ASTORE, 2);

      // In case this field is injected by DSO, only allow the value to be set once.
      // This will be done the first time by the injection class adapters and users
      // shouldn't be able to replace this with their own instance afterwards.
      // This allows non instrumented code to use mock instances for testing and
      // instrumented code to always have the DSO instance.
      Label labelMethodEnd = new Label();
      Label labelFieldNotYetInjected = new Label();
      if (isInjected(name)) {
        scv.visitVarInsn(ALOAD, 0);
        scv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), name, desc);

        scv.visitJumpInsn(IFNULL, labelFieldNotYetInjected);
        scv.visitInsn(POP);
        scv.visitJumpInsn(GOTO, labelMethodEnd);
        scv.visitLabel(labelFieldNotYetInjected);
      }

      // Handle null arguments
      Label labelArgumentIsNull = new Label();
      scv.visitJumpInsn(IFNULL, labelArgumentIsNull);

      if (isVolatile) {
        generateCodeForVolatileTransactionBegin(l1, l2, labelArgumentIsNull, l4, spec.getClassNameDots() + "." + name,
                                                LockLevel.WRITE.toInt(), scv);
      }

      scv.visitVarInsn(ALOAD, 2);
      scv.visitLdcInsn(spec.getClassNameDots());
      scv.visitLdcInsn(spec.getClassNameDots() + "." + name);
      scv.visitVarInsn(ALOAD, 1);
      scv.visitInsn(ICONST_M1);
      scv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "objectFieldChanged",
                          "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;I)V");

      if (isVolatile) {
        generateCodeForVolativeTransactionCommit(l1, l2, scv, 3, 4, spec.getClassNameDots() + "." + name,
                                                 LockLevel.WRITE.toInt());
      }

      scv.visitLabel(labelArgumentIsNull);
      scv.visitVarInsn(ALOAD, 0);
      scv.visitVarInsn(ALOAD, 1);
      scv.visitFieldInsn(PUTFIELD, spec.getClassNameSlashes(), name, desc);
      scv.visitLabel(labelMethodEnd);
      scv.visitInsn(RETURN);
      scv.visitMaxs(0, 0);
      scv.visitEnd();
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private void createLiteralSetter(final int methodAccess, final int fieldAccess, final String name, final String desc) {
    try {
      // generates setter method
      boolean isVolatile = isVolatile(fieldAccess, name);

      Label l1 = new Label();
      Label l2 = new Label();
      Label l4 = new Label();

      String sDesc = "(" + desc + ")V";
      Type t = Type.getType(desc);

      MethodVisitor mv = cv.visitMethod(methodAccess, ByteCodeUtil.fieldSetterMethod(name), sDesc, null, null);
      getManaged(mv);
      mv.visitInsn(DUP);
      mv.visitVarInsn(ASTORE, 1 + t.getSize());
      Label l0 = new Label();
      mv.visitJumpInsn(IFNULL, l0);

      if (isVolatile) {
        generateCodeForVolatileTransactionBegin(l1, l2, l0, l4, spec.getClassNameDots() + "." + name,
                                                LockLevel.WRITE.toInt(), mv);
      }

      mv.visitVarInsn(ALOAD, 1 + t.getSize());
      mv.visitLdcInsn(spec.getClassNameDots());
      mv.visitLdcInsn(spec.getClassNameDots() + "." + name);
      mv.visitVarInsn(t.getOpcode(ILOAD), 1);
      mv.visitInsn(ICONST_M1);
      String method = ByteCodeUtil.codeToName(desc) + "FieldChanged";
      mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", method, "(Ljava/lang/String;Ljava/lang/String;"
                                                                            + desc + "I)V");

      if (isVolatile) {
        generateCodeForVolativeTransactionCommit(l1, l2, mv, 2 + t.getSize(), 3 + t.getSize(), spec.getClassNameDots()
                                                                                               + "." + name,
                                                 LockLevel.WRITE.toInt());
      }

      mv.visitLabel(l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(t.getOpcode(ILOAD), 1);
      mv.visitFieldInsn(PUTFIELD, spec.getClassNameSlashes(), name, desc);
      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    } catch (RuntimeException e) {
      handleInstrumentationException(e);
    } catch (Error e) {
      handleInstrumentationException(e);
      throw e;
    }
  }

  private void callTCMonitorExit(final int callingMethodModifier, final LockDefinition def, final MethodVisitor c) {
    Assert.eval("Can't call tc monitorenter from a static method.", !Modifier.isStatic(callingMethodModifier));
    ByteCodeUtil.pushThis(c);
    c.visitLdcInsn(new Integer(def.getLockLevelAsInt()));
    c.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "instrumentationMonitorExit", "(Ljava/lang/Object;I)V");
  }

  private void callTCMonitorEnter(final int callingMethodModifier, final LockDefinition def, final MethodVisitor c) {
    Assert.eval("Can't call tc monitorexit from a static method.", !Modifier.isStatic(callingMethodModifier));
    ByteCodeUtil.pushThis(c);
    c.visitLdcInsn(new Integer(def.getLockLevelAsInt()));
    c.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "instrumentationMonitorEnter", "(Ljava/lang/Object;I)V");
  }

  private void addPrimitiveTypeZeroCompare(final MethodVisitor mv, final Type type, final Label notZeroLabel) {
    switch (type.getSort()) {
      case Type.LONG:
        mv.visitInsn(LCONST_0);
        mv.visitInsn(LCMP);
        mv.visitJumpInsn(IFNE, notZeroLabel);
        break;
      case Type.DOUBLE:
        mv.visitInsn(DCONST_0);
        mv.visitInsn(DCMPL);
        mv.visitJumpInsn(IFNE, notZeroLabel);
        break;
      case Type.FLOAT:
        mv.visitInsn(FCONST_0);
        mv.visitInsn(FCMPL);
        mv.visitJumpInsn(IFNE, notZeroLabel);
        break;
      default:
        mv.visitJumpInsn(IFNE, notZeroLabel);
    }
  }

  public MethodVisitor basicVisitMethodHack(int access, String name, String desc, String signature, String[] exceptions) {
    return basicVisitMethod(access, name, desc, signature, exceptions);
  }
}
