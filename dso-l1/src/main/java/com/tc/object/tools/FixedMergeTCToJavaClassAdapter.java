/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tools;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.tree.ClassNode;
import com.tc.asm.tree.InnerClassNode;
import com.tc.object.bytecode.MergeTCToJavaClassAdapter;
import com.tc.object.bytecode.TransparencyClassAdapterHack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FixedMergeTCToJavaClassAdapter extends MergeTCToJavaClassAdapter {
  private final List<String> jInnerClassNames = new ArrayList<String>();
  private final ClassNode    tcClassNode;
  private final String       jFullClassSlashes;
  private final String       tcFullClassSlashes;

  public FixedMergeTCToJavaClassAdapter(ClassVisitor cv, TransparencyClassAdapterHack dsoAdapter,
                                        String jFullClassDots, String tcFullClassDots, ClassNode tcClassNode,
                                        Map instrumentedContext, String methodPrefix, boolean insertTCinit) {
    super(cv, dsoAdapter, jFullClassDots, tcFullClassDots, tcClassNode, instrumentedContext, methodPrefix, insertTCinit);
    this.tcClassNode = tcClassNode;
    this.jFullClassSlashes = jFullClassDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    this.tcFullClassSlashes = tcFullClassDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
  }

  public FixedMergeTCToJavaClassAdapter(ClassVisitor cv, TransparencyClassAdapterHack dsoAdapter,
                                        String jFullClassDots, String tcFullClassDots, ClassNode tcClassNode,
                                        Map instrumentedContext) {
    super(cv, dsoAdapter, jFullClassDots, tcFullClassDots, tcClassNode, instrumentedContext);
    this.tcClassNode = tcClassNode;
    this.jFullClassSlashes = jFullClassDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    this.tcFullClassSlashes = tcFullClassDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
  }

  @Override
  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    jInnerClassNames.add(name);
    super.visitInnerClass(name, outerName, innerName, access);
  }

  @Override
  public void visitEnd() {
    addTCInnerClasses();
    super.visitEnd();
  }

  private void addTCInnerClasses() {
    List tcInnerClasses = tcClassNode.innerClasses;
    for (Iterator i = tcInnerClasses.iterator(); i.hasNext();) {
      InnerClassNode innerClass = (InnerClassNode) i.next();
      if (!tcInnerClassExistInJavaClass(innerClass)) {
        innerClass.accept(new TCInnerClassAdapter(cv));
      }
    }
  }

  private boolean tcInnerClassExistInJavaClass(InnerClassNode tcInnerClass) {
    return jInnerClassNames.contains(replaceClassName(tcInnerClass.name));
  }

  private String replaceClassName(String classNameDots) {
    if (classNameDots == null || classNameDots.length() == 0) { return classNameDots; }

    classNameDots = classNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    String srcClassNameDots = tcFullClassSlashes.replace(DOT_DELIMITER, SLASH_DELIMITER);
    String targetClassNameDots = jFullClassSlashes.replace(DOT_DELIMITER, SLASH_DELIMITER);

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

  private class TCInnerClassAdapter extends ClassAdapter implements Opcodes {
    public TCInnerClassAdapter(ClassVisitor cv) {
      super(cv);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
      super.visitInnerClass(replaceClassName(name), replaceClassName(outerName), replaceClassName(innerName), access);
    }
  }
}
