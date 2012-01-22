/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import org.apache.commons.lang.ArrayUtils;

import com.tc.asm.MethodVisitor;
import com.tc.logging.TCLogger;
import com.tc.object.config.LockDefinition;

/**
 * Logger class for PhysicalClassAdapter
 */
public class PhysicalClassAdapterLogger {
  private final TCLogger logger;

  PhysicalClassAdapterLogger(TCLogger l) {
    this.logger = l;
  }

  void logVisitMethodCreateLockMethod(String name) {
    if (logger.isDebugEnabled()) logger.debug("Creating lock:" + name);
  }

  void logVisitMethodNotALockMethod(final int access, final String ownerClass, String name, final String desc,
                                    final String[] exceptions) {
    if (logger.isDebugEnabled()) logger.debug("visitMethod(): Not a lock method: " + access + " " + ownerClass + "."
                                              + name + desc + " " + arrayToString(exceptions));
  }

  void logVisitMethodBegin(final int access, String name, String signature, final String desc, final String[] exceptions) {
    if (logger.isDebugEnabled()) logger.debug("visitMethod(" + access + ", " + name + ", " + desc + ", "
                                              + arrayToString(exceptions) + ", " + signature + ")");
  }

  void logVisitMethodIgnoring(String name, final String desc) {
    if (logger.isDebugEnabled()) logger.debug("Ignoring:" + name + " desc:" + desc);
  }

  void logVisitMethodCheckIsLockMethod() {
    if (logger.isDebugEnabled()) logger.debug("Checking isLockMethod()");
  }

  void logCallTCBeginWithLocksStart(int access, String name, String desc, LockDefinition[] locks, MethodVisitor c) {
    if (logger.isDebugEnabled()) {
      logger.debug("callTCBeginWithLocks(access=" + access + ", name=" + name + ", desc=" + desc + ", locks="
                   + arrayToString(locks) + ", c= " + c + ")");
    }
  }

  void logCallTCBeginWithLocksAutolock() {
    if (logger.isDebugEnabled()) logger.debug("callTCBeginWithLocks(): lock is autolock.");
  }

  void logCallTCBeginWithLocksAutolockSynchronized(String name, String desc) {
    if (logger.isDebugEnabled()) {
      logger.debug("callTCBeginWithLocks(): method is synchronized, calling __tcmonitorenter() for method " + name
                   + "." + desc);
    }
  }

  void logCallTCBeginWithLocksAutolockNotSynchronized(String name, String desc) {
    if (logger.isDebugEnabled()) {
      logger.debug("callTCBeginWithLocks(): method is not synchronized, ignoring autolock for method " + name + "."
                   + desc);
    }
  }

  void logCallTCBeginWithLocksNoAutolock(LockDefinition lock) {
    if (logger.isDebugEnabled()) logger.debug("calling callTCBeginWithLock() for lock " + lock);
  }

  void logCreateLockMethodBegin(int access, String name, String signature, String desc, final String[] exceptions,
                                LockDefinition[] locks) {
    if (logger.isDebugEnabled()) logger.debug("createLockMethod(access=" + access + ", name=" + name + ", desc=" + desc
                                              + ", exceptions=" + arrayToString(exceptions) + ", " + signature + ", "
                                              + arrayToString(locks) + ")");
  }

  void logCreateLockMethodVoidBegin(int access, String name, String signature, String desc, final String[] exceptions,
                                    LockDefinition[] locks) {
    if (logger.isDebugEnabled()) {
      logger.debug("createLockMethodVoid(access=" + access + ", name=" + name + ", desc=" + desc + ", exceptions="
                   + arrayToString(exceptions) + ", sig=" + signature + ", locks=" + arrayToString(locks));
    }
  }

  void logCallTCCommitBegin(int access, String name, String desc, LockDefinition[] locks, MethodVisitor c) {
    if (logger.isDebugEnabled()) logger.debug("callTCCommit(access=" + access + ", name=" + name + ", desc=" + desc
                                              + ", locks=" + arrayToString(locks) + ", c=" + c + ")");
  }

  void logCreateLockMethodReturnBegin(int access, String name, String signature, String desc,
                                      final String[] exceptions, LockDefinition[] locks) {
    if (logger.isDebugEnabled()) logger.debug("createLockMethodReturn(access=" + access + ", name=" + name + ", desc="
                                              + desc + ", exceptions=" + arrayToString(exceptions) + ", signature="
                                              + signature + ", locks=" + arrayToString(locks) + ")");
  }

  private String arrayToString(Object obj) {
    return obj == null ? "null" : ArrayUtils.toString(obj);
  }
}
