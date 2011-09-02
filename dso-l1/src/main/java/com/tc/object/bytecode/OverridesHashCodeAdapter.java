/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;

import java.util.HashSet;
import java.util.Set;

public class OverridesHashCodeAdapter extends ClassAdapter {

  private static final Set EXCLUDED          = new HashSet();
  static {
    EXCLUDED.add("java/lang/Object");
    EXCLUDED.add("java/lang/Enum");
  }

  private boolean          overridesHashCode = false;
  private VisitCall        originalVisitCall;

  public OverridesHashCodeAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.originalVisitCall = new VisitCall(version, access, name, signature, superName, interfaces);
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ("hashCode".equals(name) && "()I".equals(desc)) {
      this.overridesHashCode = true;
    }
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  public void visitEnd() {
    if (overridesHashCode && !EXCLUDED.contains(originalVisitCall.name)) {
      super.visit(originalVisitCall.getVersion(), originalVisitCall.getAccess(), originalVisitCall.getName(),
                  originalVisitCall.getSignature(), originalVisitCall.getSuperName(), ByteCodeUtil
                      .addInterfaces(originalVisitCall.getInterfaces(), new String[] { OverridesHashCode.class.getName()
                          .replace('.', '/') }));
    }
    super.visitEnd();
  }

  private static class VisitCall {
    private final int      version;
    private final int      access;
    private final String   name;
    private final String   signature;
    private final String   superName;
    private final String[] interfaces;

    VisitCall(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.version = version;
      this.access = access;
      this.name = name;
      this.signature = signature;
      this.superName = superName;
      this.interfaces = interfaces;
    }

    int getVersion() {
      return version;
    }

    int getAccess() {
      return access;
    }

    String getName() {
      return name;
    }

    String getSignature() {
      return signature;
    }

    String getSuperName() {
      return superName;
    }

    String[] getInterfaces() {
      return interfaces;
    }
  }

}
