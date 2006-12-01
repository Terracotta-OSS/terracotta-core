/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ChangeClassNameRootAdapter extends ChangeClassNameHierarchyAdapter implements Opcodes {
  private final Collection    innerClassesNames;

  private final String        srcClassNameSlashes;
  private final String        targetClassNameSlashes;
  private final String        fullClassNameSlashes;
  private final Map           instrumentedContext;
  private final ChangeContext changeContext;
  private final Collection    methodsToBeRemoved;

  public static String replaceClassName(String className, String srcClassName, String targetClassName) {
    if (className == null || className.length() == 0) { return className; }

    String returnStr = replaceClassNameInner(className, srcClassName, targetClassName);
    return returnStr.replace(SLASH_DELIMITER, DOT_DELIMITER);
  }

  private static String replaceClassNameInner(String classNameDots, String srcClassNameDots, String targetClassNameDots) {
    if (classNameDots == null || classNameDots.length() == 0) { return classNameDots; }

    classNameDots = classNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    srcClassNameDots = srcClassNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    targetClassNameDots = targetClassNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);

    int index = classNameDots.indexOf(srcClassNameDots);
    if (index == -1) { return classNameDots; }

    StringBuffer newClassName = new StringBuffer();
    while (index != -1) {
      if (index > 0) {
        newClassName.append(classNameDots.substring(0, index));
      }
      newClassName.append(targetClassNameDots);
      classNameDots = classNameDots.substring(index + srcClassNameDots.length());
      index = classNameDots.indexOf(srcClassNameDots);
    }
    newClassName.append(classNameDots);
    return newClassName.toString();
  }

  public ChangeClassNameRootAdapter(ClassVisitor cv, String fullClassNameDots, String srcClassNameDots,
                                    String targetClassNameDots, Map instrumentedContext, Collection innerClassesHolder) {
    this(cv, fullClassNameDots, srcClassNameDots, targetClassNameDots, instrumentedContext, innerClassesHolder,
         Collections.EMPTY_SET);
  }

  /**
   * @param fullClassNameDots The fully qualified class name that this class adapter is working on, e.g.,
   *        java.util.LinkedHashMap.
   * @param srcClassNameSlashes The fully qualified class name that needs to be changed, e.g., java.util.HashMap$Entry.
   * @param targetClassNameSlashes The fully qualified new class name, e.g., java.util.HashMap_J$Entry.
   */
  public ChangeClassNameRootAdapter(ClassVisitor cv, String fullClassNameDots, String srcClassNameDots,
                                    String targetClassNameDots, Map instrumentedContext, Collection innerClassesHolder,
                                    Collection methodsToBeRemoved) {
    super(cv);
    this.srcClassNameSlashes = srcClassNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    this.targetClassNameSlashes = targetClassNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    this.fullClassNameSlashes = fullClassNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    this.innerClassesNames = innerClassesHolder;
    this.methodsToBeRemoved = methodsToBeRemoved;
    this.instrumentedContext = instrumentedContext;
    this.changeContext = addNewContextIfNotExist(fullClassNameSlashes, replaceClassNameInner(fullClassNameSlashes,
                                                                                             srcClassNameSlashes,
                                                                                             targetClassNameSlashes),
                                                 instrumentedContext);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.changeContext.setOriginalSuperClass(superName);
    name = replaceClassNameInner(name, srcClassNameSlashes, targetClassNameSlashes);
    superName = replaceClassNameInner(superName, srcClassNameSlashes, targetClassNameSlashes);
    super.visit(version, access & ~ACC_ABSTRACT, name, signature, superName, interfaces);
  }

  public void visitSource(String source, String debug) {
    int lastIndex = srcClassNameSlashes.lastIndexOf(SLASH_DELIMITER);
    String srcName = srcClassNameSlashes.substring(lastIndex + 1);
    lastIndex = targetClassNameSlashes.lastIndexOf(SLASH_DELIMITER);
    String targetName = targetClassNameSlashes.substring(lastIndex + 1);

    source = replaceClassNameInner(source, srcName, targetName);
    super.visitSource(source, debug);
  }

  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    if (innerClassesNames != null && !innerClassesNames.contains(name) && fullClassNameSlashes.equals(outerName)) {
      innerClassesNames.add(name);
    }
    name = replaceClassNameInner(name, srcClassNameSlashes, targetClassNameSlashes);
    outerName = replaceClassNameInner(outerName, srcClassNameSlashes, targetClassNameSlashes);
    super.visitInnerClass(name, outerName, innerName, access);
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    String convertedDesc = replaceClassNameInner(desc, srcClassNameSlashes, targetClassNameSlashes);
    String convertedSign = replaceClassName(signature, srcClassNameSlashes, targetClassNameSlashes);
    if (!convertedDesc.equals(desc) || (convertedSign != null && !convertedSign.equals(signature))) {
      changeContext.addModifiedFieldInfo(name, desc, convertedDesc, signature, convertedSign);
    }
    return super.visitField(access, name, convertedDesc, convertedSign, value);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    // if (methodsToBeRemoved.contains(name+desc)) { return invokeSuperVisitMethod(access, name, desc, signature,
    // exceptions, instrumentedContext, fullClassNameSlashes); }

    String convertedDesc = replaceClassNameInner(desc, srcClassNameSlashes, targetClassNameSlashes);
    String convertedSign = replaceClassNameInner(signature, srcClassNameSlashes, targetClassNameSlashes);

    if (!convertedDesc.equals(desc) || (convertedSign != null && !convertedSign.equals(signature))) {
      changeContext.addModifiedMethodInfo(name, desc, convertedDesc, signature, convertedSign);
    }
    return new ChangeClassNameMethodAdapter(super.visitMethod(access, name, convertedDesc, convertedSign, exceptions));
  }

  private class ChangeClassNameMethodAdapter extends MethodAdapter implements Opcodes {
    public ChangeClassNameMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      owner = replaceClassNameInner(owner, srcClassNameSlashes, targetClassNameSlashes);
      desc = replaceClassNameInner(desc, srcClassNameSlashes, targetClassNameSlashes);
      super.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitTypeInsn(int opcode, String desc) {
      ChangeContext context = (ChangeContext) instrumentedContext.get(desc);
      if (context != null) {
        desc = context.convertedClassNameSlashes;
      } else {
        desc = replaceClassNameInner(desc, srcClassNameSlashes, targetClassNameSlashes);
      }
      super.visitTypeInsn(opcode, desc);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      ChangeContext context = (ChangeContext) instrumentedContext.get(owner);
      if (context != null) {
        owner = context.convertedClassNameSlashes;
      } else {
        owner = replaceClassNameInner(owner, srcClassNameSlashes, targetClassNameSlashes);
      }

      desc = replaceClassNameInner(desc, srcClassNameSlashes, targetClassNameSlashes);

      super.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
      desc = replaceClassNameInner(desc, srcClassNameSlashes, targetClassNameSlashes);
      super.visitLocalVariable(name, desc, signature, start, end, index);
    }
  }
}
