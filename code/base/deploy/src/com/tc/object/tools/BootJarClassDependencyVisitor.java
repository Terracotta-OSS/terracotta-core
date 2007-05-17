/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tools;

import com.tc.asm.AnnotationVisitor;
import com.tc.asm.Attribute;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.exception.ImplementMe;

import java.util.Set;

class BootJarClassDependencyVisitor implements ClassVisitor {

  private Set bootJarClassNames;

  public BootJarClassDependencyVisitor(Set bootJarClassNames) {
    this.bootJarClassNames = bootJarClassNames;
  }

  private final boolean inBootJar(final String className) {
    return bootJarClassNames.contains(className);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    throw new ImplementMe();
  }

  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    throw new ImplementMe();
  }

  public void visitAttribute(Attribute attr) {
    throw new ImplementMe();
  }

  public void visitEnd() {
    throw new ImplementMe();
    
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    throw new ImplementMe();
  }

  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    throw new ImplementMe();
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    throw new ImplementMe();
  }

  public void visitOuterClass(String owner, String name, String desc) {
    throw new ImplementMe();
  }

  public void visitSource(String source, String debug) {
    throw new ImplementMe();
  }
}
