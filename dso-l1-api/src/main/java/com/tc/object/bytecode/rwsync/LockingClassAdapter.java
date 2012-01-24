/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.object.bytecode.rwsync;

import static com.tc.object.bytecode.rwsync.LockNames.READLOCK_DESC;
import static com.tc.object.bytecode.rwsync.LockNames.READLOCK_NAME;
import static com.tc.object.bytecode.rwsync.LockNames.RWLOCK_DESC;
import static com.tc.object.bytecode.rwsync.LockNames.RWLOCK_NAME;
import static com.tc.object.bytecode.rwsync.LockNames.WRITELOCK_DESC;
import static com.tc.object.bytecode.rwsync.LockNames.WRITELOCK_NAME;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.logging.TCLogger;
import com.tc.object.bytecode.ManagerUtil;

import java.util.Map;

/**
 * A base for class adapters that wrap methods inside read or write locks. Creates and initializes the lock fields, and
 * defines a table-based approach to specifying locking strategy.
 */
public abstract class LockingClassAdapter extends ClassAdapter implements Opcodes {

  // Common locking strategies
  protected static final MethodStrategy INIT_STRATEGY      = new InitMethodStrategy();
  protected static final MethodStrategy NOLOCK_STRATEGY    = new NoLockMethodStrategy();
  protected static final MethodStrategy READLOCK_STRATEGY  = new SimpleLockMethodStrategy(
                                                                                          SimpleLockMethodStrategy.LockType.READ);
  protected static final MethodStrategy WRITELOCK_STRATEGY = new SimpleLockMethodStrategy(
                                                                                          SimpleLockMethodStrategy.LockType.WRITE);

  /**
   * @return the slash and dollar delimited name of the type being instrumented, e.g.,
   *         "org/hibernate/collection/AbstractPersistentCollection$IteratorProxy"
   */
  abstract protected String getOwnerType();

  /**
   * @return the human-friendly name of the type being instrumented, e.g.,
   *         "org.hibernate.collection.AbstractPersistentCollection.IteratorProxy"
   */
  abstract protected String getOwnerTypeDots();

  /**
   * @return the slash-delimited name of the outer type, if the owner type (the type being instrumented) is an inner
   *         type; or null, if the owner type is a top-level type. For example,
   *         "org/hibernate/collection/AbstractPersistentCollection".
   */
  abstract protected String getOuterType();

  /**
   * @return the type descriptor of the outer type, if the owner type (the type being instrumented) is an inner type; or
   *         null, if the owner type is a top-level type. For example,
   *         "Lorg/hibernate/collection/AbstractPersistentCollection;".
   */
  abstract protected String getOuterDesc();

  /**
   * @return a read-only map of method identifier to locking technique.
   */
  abstract protected Map<MethodId, MethodStrategy> getLockingStrategy();

  public LockingClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  /**
   * Modify existing methods and create new wrapped methods.
   */
  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodStrategy strategy = getLockingStrategy().get(new MethodId(name, desc));
    if (strategy == null) {
      String msg = "Encountered unexpected method \""
                   + name
                   + "()\" "
                   + desc
                   + " while instrumenting class "
                   + getOwnerTypeDots()
                   + ": resulting code may not be threadsafe. "
                   + "Check that the version of the library that contains this class matches the version expected by the TIM.";
      TCLogger log = ManagerUtil.getLogger("com.tc.object.bytecode.rwsync.LockingClassAdapter");
      log.warn(msg);
      strategy = NOLOCK_STRATEGY;
    }
    MethodVisitor mv = strategy.visitMethod(cv, getOwnerType(), getOuterType(), getOuterDesc(), access, name, desc,
                                            signature, exceptions);
    return mv;
  }

  /**
   * Helper function to add the necessary lock fields to a class. This should be called from
   * {@link ClassVisitor#visitEnd()} of the class adapter that is instrumenting the base class of a hierarchy. That is,
   * if B extends A, and methods in A and B are both being locked, this should be called from A's class adapter. Don't
   * call it from B's adapter, or you'll be using different locks for the A and B methods on the same object!
   * <p>
   * Class adapters which call this method should also map constructor methods to the {@link InitMethodStrategy}
   * strategy to initialize the fields.
   * 
   * @param cv can be the originating ClassVisitor or its delegate, it doesn't matter
   * @see InitMethodStrategy
   */
  protected void addLockFields() {
    cv.visitField(ACC_PROTECTED + ACC_FINAL, RWLOCK_NAME, RWLOCK_DESC, null, null);
    cv.visitField(ACC_PROTECTED + ACC_FINAL, READLOCK_NAME, READLOCK_DESC, null, null);
    cv.visitField(ACC_PROTECTED + ACC_FINAL, WRITELOCK_NAME, WRITELOCK_DESC, null, null);
  }

}
