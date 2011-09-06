/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import java.util.Collection;

public class ChangePackageClassAdapter extends ClassAdapter implements Opcodes {
  private final static char DOT_DELIMITER   = '.';
  private final static char SLASH_DELIMITER = '/';

  private final String        newName;
  private final String        targetName;
  private final String        newType;
  private final String        targetType;
  
  private final Collection          innerClassNames;
  
  public static String replaceClassName(String name, String targetPackage, String newPackage) {
    name = name.replace(SLASH_DELIMITER, DOT_DELIMITER);
    int index = name.indexOf(targetPackage);
    if (index == -1) { return name; }
    return newPackage+DOT_DELIMITER+name.substring(targetPackage.length()+1);
  }

  /**
   * @param targetClassName The class name at which package needs to be changed, e.g., HashMap.
   * @param targetPackageName The package name at which package needs to be changed, e.g., com.tc.bootjar.java.util.
   * @param newPackage The new package name, e.g., java.util.
   * @param innerClassesHolder A collection which contains the inner class names of this class.
   * It could be null if the inner class can be ignored.
   */
  public ChangePackageClassAdapter(ClassVisitor cv, String targetClassName, String targetPackage, String newPackage,
                                   Collection innerClassesHolder) {
    super(cv);
    this.targetName = targetPackage.replace(DOT_DELIMITER, SLASH_DELIMITER) + SLASH_DELIMITER + targetClassName;
    this.newName = newPackage.replace(DOT_DELIMITER, SLASH_DELIMITER) + SLASH_DELIMITER + targetClassName;
    this.targetType = "L"+targetName;
    this.newType = "L"+newName;
    this.innerClassNames = innerClassesHolder;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    name = replaceName(name);
    superName = replaceName(superName);
    super.visit(version, access, name, signature, superName, interfaces);
  }
  
  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    desc = replaceName(desc);
    return super.visitField(access, name, desc, signature, value);
  }
  
  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    if (innerClassNames != null && !innerClassNames.contains(name) && targetName.equals(outerName)) {
      innerClassNames.add(name);
    }
    
    name = replaceName(name);
    outerName = replaceName(outerName);
    super.visitInnerClass(name, outerName, innerName, access);
  }
  
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    desc = replaceName(desc);
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    return new ChangePackageMethodVisitor(mv);
  }
  
  private String replaceName(String name) {
    if (targetName.equals(name)) {
      return newName;
    }
    int index = name.indexOf(targetName);
    while (index != -1) {
      name = name.substring(0, index)+newName+name.substring(index+targetName.length());
      index = name.indexOf(targetName);
    }
    return name;
  }
  
  private String replaceDesc(String desc) {
    if (targetType.equals(desc)) {
      return newType;
    }
    int index = desc.indexOf(targetType);
    while (index != -1) {
      desc = desc.substring(0, index)+newType+desc.substring(index+targetType.length());
      index = desc.indexOf(targetType);
    }
    return desc;
  }
  
  private class ChangePackageMethodVisitor extends MethodAdapter implements Opcodes {
    public ChangePackageMethodVisitor(MethodVisitor mv) {
      super(mv);
    }
    
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      owner = replaceName(owner);
      desc = replaceName(desc);
      super.visitFieldInsn(opcode, owner, name, desc);
    }
    
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      owner = replaceName(owner);
      desc = replaceDesc(desc);
      super.visitMethodInsn(opcode, owner, name, desc);
    }
    
    public void visitTypeInsn(int opcode, String desc) {
      desc = replaceName(desc);
      super.visitTypeInsn(opcode, desc);
    }
    
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
      desc = replaceName(desc);
      super.visitLocalVariable(name, desc, signature, start, end, index);
    }
  }
}