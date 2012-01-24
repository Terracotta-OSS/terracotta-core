/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * This class will change the class name in methodInsn.
 */
public abstract class ChangeClassNameHierarchyAdapter extends ClassAdapter implements Opcodes {
  public final static char DOT_DELIMITER          = '.';
  public final static char SLASH_DELIMITER        = '/';
  public final static char INNER_CLASS_DELIMITER  = '$';
  final static String      CLASS_DELIMITER        = ";";
  final static String      CLASS_START_CHAR       = "L";
  final static String      CLASS_START_DESC_CHAR  = "(L";
  final static String      CLASS_RETURN_DESC_CHAR = ")L";
  final static String      CLASS_ARRAY_DESC_CHAR  = "[L";

  ChangeContext addNewContextIfNotExist(String fullClassNameSlashes, String convertedFullClassNameSlashes,
                                        Map instrumentedContext) {
    ChangeContext changeContext = (ChangeContext) instrumentedContext.get(fullClassNameSlashes);
    if (changeContext == null) {
      changeContext = new ChangeContext(fullClassNameSlashes, convertedFullClassNameSlashes);
      instrumentedContext.put(fullClassNameSlashes, changeContext);
    } else if (!convertedFullClassNameSlashes.equals(fullClassNameSlashes)
               && !convertedFullClassNameSlashes.equals(changeContext.convertedClassNameSlashes)) {
      changeContext.convertedClassNameSlashes = convertedFullClassNameSlashes;
    }
    return changeContext;
  }

  ChangeContext addNewContext(String fullClassNameSlashes, String convertedFullClassNameSlashes, Map instrumentedContext) {
    ChangeContext changeContext = new ChangeContext(fullClassNameSlashes, convertedFullClassNameSlashes);
    instrumentedContext.put(fullClassNameSlashes, changeContext);

    return changeContext;
  }

  ModifiedMethodInfo getModifiedMethodInfo(ChangeContext changeContext, String methodName, String desc) {
    if (changeContext == null) { return null; }

    ModifiedMethodInfo methodInfo = (ModifiedMethodInfo) changeContext.modifiedMethodInfo.get(methodName + desc);
    if (methodInfo != null && methodName.equals(methodInfo.methodName) && desc.equals(methodInfo.originalMethodDesc)) { return methodInfo; }
    return null;
  }

  String getConvertedMethodDesc(Map instrumentedContext, String fullClassNameSlashes, String methodName,
                                String methodDesc) {
    ChangeContext context = (ChangeContext) instrumentedContext.get(fullClassNameSlashes);
    while (context != null) {
      ModifiedMethodInfo methodInfo = getModifiedMethodInfo(context, methodName, methodDesc);
      if (methodInfo != null) { return methodInfo.convertedMethodDesc; }
      String superClassName = context.originalSuperClassNameSlashes;
      if (superClassName != null) {
        context = (ChangeContext) instrumentedContext.get(superClassName);
      } else {
        context = null;
      }
    }
    return null;
  }

  static class ChangeContext {
    final String originalClassNameSlashes;
    String       convertedClassNameSlashes;
    String       originalSuperClassNameSlashes;

    final Map    modifiedMethodInfo = new HashMap();
    final Map    modifiedFieldInfo  = new HashMap();

    public ChangeContext(String originalClassNameSlashes, String convertedClassNameSlashes) {
      this.originalClassNameSlashes = originalClassNameSlashes;
      this.convertedClassNameSlashes = convertedClassNameSlashes;
    }

    public void setOriginalSuperClass(String superClassNameSlashes) {
      this.originalSuperClassNameSlashes = superClassNameSlashes;
    }

    void addModifiedMethodInfo(String name, String originalDesc, String convertedDesc, String signature,
                               String convertedSignature) {
      ModifiedMethodInfo methodInfo = (ModifiedMethodInfo) modifiedMethodInfo.get(name + originalDesc);
      if (methodInfo == null) {
        modifiedMethodInfo.put(name + originalDesc, new ModifiedMethodInfo(name, originalDesc, convertedDesc,
                                                                           signature, convertedSignature));
      } else {
        if (!convertedDesc.equals(methodInfo.convertedMethodDesc)) {
          methodInfo.convertedMethodDesc = convertedDesc;
        }
        if (signature != null && !signature.equals(methodInfo.originalSignature)) {
          methodInfo.originalSignature = signature;
        }
        if (convertedSignature != null && !convertedSignature.equals(methodInfo.convertedSignature)) {
          methodInfo.convertedSignature = signature;
        }
      }
    }

    void addModifiedFieldInfo(String name, String originalDesc, String convertedDesc, String signature,
                              String convertedSignature) {
      modifiedFieldInfo.put(name + originalDesc, new ModifiedFieldInfo(name, originalDesc, convertedDesc, signature,
                                                                       convertedSignature));
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append(super.toString());
      buf.append(" ChangeContext :- ");
      buf.append("originalClassNameSlashes: ");
      buf.append(originalClassNameSlashes);
      buf.append(", convertedClassNameSlashes: ");
      buf.append(convertedClassNameSlashes);
      buf.append(", modifiedMethodInfo size: ");
      buf.append(modifiedMethodInfo.size());
      buf.append(modifiedMethodInfo);
      return buf.toString();
    }
  }

  static class ModifiedFieldInfo {
    final String fieldName;
    final String originalFieldDesc;
    String       convertedFieldDesc;
    String       originalSignature;
    String       convertedSignature;

    ModifiedFieldInfo(String name, String originalDesc, String convertedDesc, String signature,
                      String convertedSignature) {
      this.fieldName = name;
      this.originalFieldDesc = originalDesc;
      this.convertedFieldDesc = convertedDesc;
      this.originalSignature = signature;
      this.convertedSignature = convertedSignature;
    }

    public boolean equals(Object obj) {
      ModifiedFieldInfo fieldInfo = (ModifiedFieldInfo) obj;
      boolean match = this.fieldName != null && this.fieldName.equals(fieldInfo.fieldName);
      match = match && this.originalFieldDesc != null && this.originalFieldDesc.equals(fieldInfo.originalFieldDesc);
      match = match && this.convertedFieldDesc != null && this.convertedFieldDesc.equals(fieldInfo.convertedFieldDesc);
      match = match
              && ((this.originalSignature == null && fieldInfo.originalSignature == null) || (this.originalSignature != null && this.originalSignature
                  .equals(fieldInfo.originalSignature)));
      match = match
              && ((this.convertedSignature == null && fieldInfo.convertedSignature == null) || (this.convertedSignature != null && this.convertedSignature
                  .equals(fieldInfo.convertedSignature)));
      return match;
    }

    public String toString() {
      return super.toString() + ", name: " + fieldName + ", desc: " + originalFieldDesc + ", convertedDesc: "
             + convertedFieldDesc;
    }
  }

  static class ModifiedMethodInfo {
    final String methodName;
    final String originalMethodDesc;
    String       convertedMethodDesc;
    String       originalSignature;
    String       convertedSignature;

    ModifiedMethodInfo(String name, String originalDesc, String convertedDesc, String signature,
                       String convertedSignature) {
      this.methodName = name;
      this.originalMethodDesc = originalDesc;
      this.convertedMethodDesc = convertedDesc;
      this.originalSignature = signature;
      this.convertedSignature = convertedSignature;
    }

    public boolean equals(Object obj) {
      ModifiedMethodInfo methodInfo = (ModifiedMethodInfo) obj;
      boolean match = this.methodName != null && this.methodName.equals(methodInfo.methodName);
      match = match && this.originalMethodDesc != null && this.originalMethodDesc.equals(methodInfo.originalMethodDesc);
      match = match && this.convertedMethodDesc != null
              && this.convertedMethodDesc.equals(methodInfo.convertedMethodDesc);
      match = match
              && ((this.originalSignature == null && methodInfo.originalSignature == null) || (this.originalSignature != null && this.originalSignature
                  .equals(methodInfo.originalSignature)));
      match = match
              && ((this.convertedSignature == null && methodInfo.convertedSignature == null) || (this.convertedSignature != null && this.convertedSignature
                  .equals(methodInfo.convertedSignature)));
      return match;
    }

    public String toString() {
      return super.toString() + ", name: " + methodName + ", desc: " + originalMethodDesc + ", convertedDesc: "
             + convertedMethodDesc;
    }
  }

  public ChangeClassNameHierarchyAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor invokeSuperVisitMethod(int access, String name, String desc, String signature,
                                              String[] exceptions, Map instrumentedContext, String fullClassSlashes) {
    ChangeContext context = (ChangeContext) instrumentedContext.get(fullClassSlashes);
    String origSuperClassName = context.originalSuperClassNameSlashes;
    // -- need this when superClassNameAdapter and RootAdapter are merged context =
    // (ChangeContext)instrumentedContext.get(origSuperClassName);
    // String convertedSuperClassName = context.convertedClassNameSlashes;

    Type returnType = Type.getReturnType(desc);
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    ByteCodeUtil.pushMethodArguments(access, desc, mv);
    mv.visitMethodInsn(INVOKESPECIAL, origSuperClassName, name, desc);
    mv.visitInsn(returnType.getOpcode(IRETURN));
    mv.visitMaxs(0, 0);
    mv.visitEnd();
    return mv;
  }

}
