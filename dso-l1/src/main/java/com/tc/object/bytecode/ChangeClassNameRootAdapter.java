/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import java.util.Collection;
import java.util.Map;

public class ChangeClassNameRootAdapter extends ChangeClassNameHierarchyAdapter implements Opcodes {
  private final Collection    innerClassesNames;

  private final String        srcClassNameSlashes;
  private final String        targetClassNameSlashes;
  private final String        fullClassNameSlashes;
  private final String        srcInnerClassName;
  private final String        targetInnerClassName;
  private final Map           instrumentedContext;
  private final ChangeContext changeContext;

  public static String replaceClassName(String className, String srcClassName, String targetClassName,
                                        String srcInnerClassName, String targetInnerClassName) {
    if (className == null || className.length() == 0) { return className; }

    String returnStr = replaceInnerClassName(replaceClassNameInner(className, srcClassName, targetClassName),
                                             srcInnerClassName, targetInnerClassName);
    return returnStr.replace(SLASH_DELIMITER, DOT_DELIMITER);
  }

  private static String replaceClassNameInner(String classNameDots, String srcClassNameDots, String targetClassNameDots) {
    if (classNameDots == null) { return classNameDots; }

    classNameDots = classNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    srcClassNameDots = srcClassNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    targetClassNameDots = targetClassNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);

    return classNameDots.replace(srcClassNameDots, targetClassNameDots);
  }

  private static String replaceInnerClassName(String classNameDots, String srcInnerClassName,
                                              String targetInnerClassName) {
    if (classNameDots == null || srcInnerClassName == null || targetInnerClassName == null) { return classNameDots; }

    int index = classNameDots.indexOf(INNER_CLASS_DELIMITER);
    if (index == -1) { return classNameDots; }

    StringBuffer newClassName = new StringBuffer();
    newClassName.append(classNameDots.substring(0, index + 1));
    String innerClassName = classNameDots.substring(index + 1);
    innerClassName = replaceClassNameInner(innerClassName, srcInnerClassName, targetInnerClassName);
    newClassName.append(innerClassName);
    return newClassName.toString();
  }

  /**
   * @param fullClassNameDots The fully qualified class name that this class adapter is working on, e.g.,
   *        java.util.LinkedHashMap.
   * @param srcClassNameSlashes The fully qualified class name that needs to be changed, e.g., java.util.HashMap$Entry.
   * @param targetClassNameSlashes The fully qualified new class name, e.g., java.util.HashMap_J$Entry.
   */
  public ChangeClassNameRootAdapter(ClassVisitor cv, String fullClassNameDots, String srcClassNameDots,
                                    String targetClassNameDots, String srcInnerClassName, String targetInnerClassName,
                                    Map instrumentedContext, Collection innerClassesHolder) {
    super(cv);
    this.srcClassNameSlashes = srcClassNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    this.targetClassNameSlashes = targetClassNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    this.srcInnerClassName = srcInnerClassName;
    this.targetInnerClassName = targetInnerClassName;
    this.fullClassNameSlashes = fullClassNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    this.innerClassesNames = innerClassesHolder;
    this.instrumentedContext = instrumentedContext;
    this.changeContext = addNewContextIfNotExist(fullClassNameSlashes,
                                                 replaceInnerClassName(replaceClassNameInner(fullClassNameSlashes,
                                                                                             srcClassNameSlashes,
                                                                                             targetClassNameSlashes),
                                                                       srcInnerClassName, targetInnerClassName),
                                                 instrumentedContext);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.changeContext.setOriginalSuperClass(superName);
    name = replaceInnerClassName(replaceClassNameInner(name, srcClassNameSlashes, targetClassNameSlashes),
                                 srcInnerClassName, targetInnerClassName);
    superName = replaceClassNameInner(superName, srcClassNameSlashes, targetClassNameSlashes);
    super.visit(version, access & ~ACC_ABSTRACT, name, signature, superName, interfaces);
  }

  @Override
  public void visitSource(String source, String debug) {
    if (source == null) {
      super.visitSource(null, debug);
    } else {
      int lastIndex = srcClassNameSlashes.lastIndexOf(SLASH_DELIMITER);
      String srcName = srcClassNameSlashes.substring(lastIndex + 1);
      lastIndex = targetClassNameSlashes.lastIndexOf(SLASH_DELIMITER);
      String targetName = targetClassNameSlashes.substring(lastIndex + 1);

      super.visitSource(source.replace(srcName, targetName), debug);
    }
  }

  @Override
  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    if (innerClassesNames != null && !innerClassesNames.contains(name) && fullClassNameSlashes.equals(outerName)) {
      innerClassesNames.add(name);
    }
    name = replaceInnerClassName(replaceClassNameInner(name, srcClassNameSlashes, targetClassNameSlashes),
                                 srcInnerClassName, targetInnerClassName);
    outerName = replaceInnerClassName(replaceClassNameInner(outerName, srcClassNameSlashes, targetClassNameSlashes),
                                      srcInnerClassName, targetInnerClassName);
    super.visitInnerClass(name, outerName, innerName, access);
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    String convertedDesc = replaceInnerClassName(replaceClassNameInner(desc, srcClassNameSlashes,
                                                                       targetClassNameSlashes), srcInnerClassName,
                                                 targetInnerClassName);
    String convertedSign = replaceClassName(signature, srcClassNameSlashes, targetClassNameSlashes, srcInnerClassName,
                                            targetInnerClassName);
    if (!convertedDesc.equals(desc) || (convertedSign != null && !convertedSign.equals(signature))) {
      changeContext.addModifiedFieldInfo(name, desc, convertedDesc, signature, convertedSign);
    }
    return super.visitField(access, name, convertedDesc, convertedSign, value);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    String convertedDesc = replaceInnerClassName(replaceClassNameInner(desc, srcClassNameSlashes,
                                                                       targetClassNameSlashes), srcInnerClassName,
                                                 targetInnerClassName);
    String convertedSign = replaceInnerClassName(replaceClassNameInner(signature, srcClassNameSlashes,
                                                                       targetClassNameSlashes), srcInnerClassName,
                                                 targetInnerClassName);

    if (!convertedDesc.equals(desc) || (convertedSign != null && !convertedSign.equals(signature))) {
      changeContext.addModifiedMethodInfo(name, desc, convertedDesc, signature, convertedSign);
    }
    return new ChangeClassNameMethodAdapter(super.visitMethod(access, name, convertedDesc, convertedSign, exceptions));
  }

  private class ChangeClassNameMethodAdapter extends MethodAdapter implements Opcodes {
    public ChangeClassNameMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      owner = replaceInnerClassName(replaceClassNameInner(owner, srcClassNameSlashes, targetClassNameSlashes),
                                    srcInnerClassName, targetInnerClassName);
      desc = replaceInnerClassName(replaceClassNameInner(desc, srcClassNameSlashes, targetClassNameSlashes),
                                   srcInnerClassName, targetInnerClassName);
      super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitTypeInsn(int opcode, String desc) {
      ChangeContext context = (ChangeContext) instrumentedContext.get(desc);
      if (context != null) {
        desc = context.convertedClassNameSlashes;
      } else {
        desc = replaceInnerClassName(replaceClassNameInner(desc, srcClassNameSlashes, targetClassNameSlashes),
                                     srcInnerClassName, targetInnerClassName);
      }
      super.visitTypeInsn(opcode, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      ChangeContext context = (ChangeContext) instrumentedContext.get(owner);
      if (context != null) {
        owner = context.convertedClassNameSlashes;
      } else {
        owner = replaceInnerClassName(replaceClassNameInner(owner, srcClassNameSlashes, targetClassNameSlashes),
                                      srcInnerClassName, targetInnerClassName);
      }

      desc = replaceInnerClassName(replaceClassNameInner(desc, srcClassNameSlashes, targetClassNameSlashes),
                                   srcInnerClassName, targetInnerClassName);

      super.visitMethodInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
      desc = replaceClassNameInner(desc, srcClassNameSlashes, targetClassNameSlashes);
      super.visitLocalVariable(name, desc, signature, start, end, index);
    }
  }
}
