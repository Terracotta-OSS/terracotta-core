/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.tree.ClassNode;
import com.tc.asm.tree.FieldNode;
import com.tc.asm.tree.InnerClassNode;
import com.tc.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MergeTCToJavaClassAdapter extends ChangeClassNameHierarchyAdapter implements Opcodes {
  private final ClassNode          tcClassNode;
  private final String             jFullClassSlashes;
  private final String             tcFullClassSlashes;
  private final Map                instrumentedContext;
  private final Set                visitedMethods;
  private String                   superName;
  private TransparencyClassAdapter dsoAdapter;

  public MergeTCToJavaClassAdapter(ClassVisitor cv, TransparencyClassAdapter dsoAdapter, String jFullClassDots, String tcFullClassDots,
                                   ClassNode tcClassNode, Map instrumentedContext) {
    super(cv);
    this.tcClassNode = tcClassNode;
    this.jFullClassSlashes = jFullClassDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    this.tcFullClassSlashes = tcFullClassDots.replace(DOT_DELIMITER, SLASH_DELIMITER);

    addNewContextIfNotExist(tcFullClassDots, jFullClassDots, instrumentedContext);
    this.instrumentedContext = instrumentedContext;
    this.visitedMethods = new HashSet();
    this.dsoAdapter = dsoAdapter;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.superName = superName;

    List tcInterfaces = tcClassNode.interfaces;
    List jInterfaces = new ArrayList();
    for (int i = 0; i < interfaces.length; i++) {
      if (!tcInterfaces.contains(interfaces[i])) {
        jInterfaces.add(interfaces[i]);
      }
    }
    for (Iterator i = tcInterfaces.iterator(); i.hasNext();) {
      String intf = (String) i.next();
      jInterfaces.add(intf);
    }
    interfaces = new String[jInterfaces.size()];
    jInterfaces.toArray(interfaces);
    //super.visit(version, access, name, signature, superName, interfaces);
    dsoAdapter.visit(version, access, name, signature, superName, interfaces);
  }

  private String getNewName(String methodName) {
    if (isInitMethod(methodName)) { return methodName; }
    return ByteCodeUtil.TC_METHOD_PREFIX + methodName;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    String methodDesc = name + desc;
    this.visitedMethods.add(methodDesc);

    List tcMethods = tcClassNode.methods;
    if (!isInitMethod(name)) {
      for (Iterator i = tcMethods.iterator(); i.hasNext();) {
        MethodNode mNode = (MethodNode) i.next();
        if (mNode.name.equals(name) && mNode.desc.equals(desc)) { return super
            .visitMethod(ACC_PRIVATE, getNewName(name), desc, signature, exceptions); }
      }
    }
    if (LogicalClassSerializationAdapter.WRITE_OBJECT_SIGNATURE.equals(methodDesc)
        || LogicalClassSerializationAdapter.READ_OBJECT_SIGNATURE.equals(methodDesc)) { return new LogicalClassSerializationAdapter.LogicalClassSerializationMethodAdapter(
                                                                                                                                                                           super
                                                                                                                                                                               .visitMethod(
                                                                                                                                                                                            access,
                                                                                                                                                                                            name,
                                                                                                                                                                                            desc,
                                                                                                                                                                                            signature,
                                                                                                                                                                                            exceptions),
                                                                                                                                                                           jFullClassSlashes); }

    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    List tcFields = tcClassNode.fields;
    for (Iterator i = tcFields.iterator(); i.hasNext();) {
      FieldNode fieldNode = (FieldNode) i.next();
      if (access == fieldNode.access && name.equals(fieldNode.name) && desc.equals(fieldNode.desc)) {
        i.remove();
        break;
      }
    }

    // hack for now
    if ("java/util/LinkedHashMap".equals(jFullClassSlashes) && "accessOrder".equals(name)) {
      access = ~Modifier.FINAL & access;
    }
    return super.visitField(access, name, desc, signature, value);
  }

  public void visitEnd() {
    addTCFields();
    addTCMethods();
    addTCInnerClasses();
    super.visitEnd();
  }

  private void addTCMethods() {
    List tcMethods = tcClassNode.methods;
    for (Iterator i = tcMethods.iterator(); i.hasNext();) {
      MethodNode mNode = (MethodNode) i.next();
      if (isInitMethod(mNode.name)) {
        continue;
      }
      mNode.accept(new TCSuperClassAdapter(cv));
    }
    LogicalClassSerializationAdapter.addCheckSerializationOverrideMethod(cv, false);
  }

  private boolean isInitMethod(String methodName) {
    return "<init>".equals(methodName);
  }

  private void addTCFields() {
    List tcFields = tcClassNode.fields;
    for (Iterator i = tcFields.iterator(); i.hasNext();) {
      FieldNode fNode = (FieldNode) i.next();
      fNode.accept(cv);
    }
  }

  private void addTCInnerClasses() {
    List tcInnerClasses = tcClassNode.innerClasses;
    for (Iterator i = tcInnerClasses.iterator(); i.hasNext();) {
      InnerClassNode innerClass = (InnerClassNode) i.next();
      innerClass.accept(new TCSuperClassAdapter(cv));
    }
  }

  private class TCSuperClassAdapter extends ClassAdapter implements Opcodes {
    public TCSuperClassAdapter(ClassVisitor cv) {
      super(cv);
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      name = replaceClassName(name);
      superName = replaceClassName(superName);
      super.visit(version, access & ~ACC_ABSTRACT, name, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      desc = replaceClassName(desc);
      signature = replaceClassName(signature);
      //return new TCSuperMethodAdapter(super.visitMethod(access, name, desc, signature, exceptions));
      return new TCSuperMethodAdapter(dsoAdapter.basicVisitMethod(access, name, desc, signature, exceptions));
    }

    private String replaceClassName(String classNameDots) {
      return replaceClassName(classNameDots, tcFullClassSlashes, jFullClassSlashes);
    }

    private String replaceClassName(String classNameDots, String srcClassNameDots, String targetClassNameDots) {
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

    private class TCSuperMethodAdapter extends MethodAdapter implements Opcodes {
      public TCSuperMethodAdapter(MethodVisitor mv) {
        super(mv);
      }

      public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        int index = owner.indexOf(tcFullClassSlashes);
        if (opcode == INVOKESPECIAL && index == -1) {
          if (!visitedMethods.contains(name + desc) && !isInitMethod(name)) {
            owner = superName;
          } else {
            ChangeContext context = (ChangeContext) instrumentedContext.get(owner);
            if (context != null) {
              owner = context.convertedClassNameSlashes;
            }
            name = getNewName(name);
          }
          super.visitMethodInsn(opcode, owner, name, desc);
        } else {
          owner = replaceClassName(owner);
          desc = replaceClassName(desc);
          super.visitMethodInsn(opcode, owner, name, desc);
        }
      }

      public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        owner = replaceClassName(owner);
        desc = replaceClassName(desc);
        super.visitFieldInsn(opcode, owner, name, desc);
      }

      public void visitTypeInsn(int opcode, String desc) {
        ChangeContext context = (ChangeContext) instrumentedContext.get(desc);
        if (context != null) {
          desc = context.convertedClassNameSlashes;
        } else {
          desc = replaceClassName(desc);
        }
        super.visitTypeInsn(opcode, desc);
      }

    }
  }
}
