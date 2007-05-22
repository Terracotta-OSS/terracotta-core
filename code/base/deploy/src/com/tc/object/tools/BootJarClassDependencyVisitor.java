/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tools;

import com.tc.asm.AnnotationVisitor;
import com.tc.asm.Attribute;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import java.util.Map;
import java.util.Set;

public class BootJarClassDependencyVisitor implements ClassVisitor {

  private Set bootJarClassNames;
  private Map offendingClasses;
  private String currentClassName = "";

  private static final String classSlashNameToDotName(final String name) {
    return name.replace('/', '.');
  }

  private final boolean check(final String name, String desc) {
    if (name.startsWith("com/tc/") || name.startsWith("com/tcclient/")) {
      boolean exists = bootJarClassNames.contains(BootJarClassDependencyVisitor.classSlashNameToDotName(name));
      if (!exists) {
        this.offendingClasses.put(classSlashNameToDotName(name), desc + " from " + this.currentClassName);
      }
      return exists;
    } else {
      return true;
    }
  }

  public BootJarClassDependencyVisitor(Set bootJarClassNames, Map offendingClasses) {
    this.bootJarClassNames = bootJarClassNames;
    this.offendingClasses  = offendingClasses;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.currentClassName = BootJarClassDependencyVisitor.classSlashNameToDotName(name);
    
    // check the referenced class
    check(name, "reference to the class itself");

    // check it's superclass
    check(superName, "reference to 'extend' class declaration");
    
    // check it's interfaces
    for (int i=0; i<interfaces.length; i++) {
      check(interfaces[i], "reference to 'implements' interface declaration");
    }
  }

  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return null;
  }

  public void visitAttribute(Attribute attr) {
    //throw new ImplementMe();
  }

  public void visitEnd() {
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    return null;
  }

  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    check(name, "reference to inner-class");
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = new BootJarClassDependencyMethodVisitor();
    return mv;
  }

  public void visitOuterClass(String owner, String name, String desc) {
    check(owner, "reference to outer-class");
  }

  public void visitSource(String source, String debug) {
  }
    
  private class BootJarClassDependencyMethodVisitor implements MethodVisitor, Opcodes {

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      //throw new ImplementMe();
      return null;
    }

    public AnnotationVisitor visitAnnotationDefault() {
      return null;
    }

    public void visitAttribute(Attribute attr) {
      //throw new ImplementMe();
    }

    public void visitCode() {
    }

    public void visitEnd() {
      //throw new ImplementMe();
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      // check referenced classes in field-(gets|puts)
      check(owner, "reference in field get or put");
    }

    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
    }

    public void visitIincInsn(int var, int increment) {
    }

    public void visitInsn(int opcode) {
    }

    public void visitIntInsn(int opcode, int operand) {
    }

    public void visitJumpInsn(int opcode, Label label) {
    }

    public void visitLabel(Label label) {
    }

    public void visitLdcInsn(Object cst) {
      //throw new ImplementMe();
    }

    public void visitLineNumber(int line, Label start) {
    }

    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
      // check referenced classes for local variable declarations  
      check(desc, "reference in local variable declaration");
    }

    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    }

    public void visitMaxs(int maxStack, int maxLocals) {
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      // check referenced classes in virtual, interface, constructor, or static, calls
      check(owner, "reference in either virtual, interface, constructor, or static invocation");
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
      // check referenced classes in multi-array type declarations  
      check(desc, "reference in mutli-array type declaration");
    }

    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
      //throw new ImplementMe();
      return null;
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
      // check referenced class in catch-blocks
      if (type != null) {
        check(type, "reference in try-catch block");
      }
    }

    public void visitTypeInsn(int opcode, String desc) {
      // check referenced classes in type-casts, class instantiation,  
      // instance type-check, or type-array declarations
      check(desc, "reference in type-cast, class instantiation, type-check, or type-array declaration");
    }

    public void visitVarInsn(int opcode, int var) {
      //throw new ImplementMe();
    }
  }
}
