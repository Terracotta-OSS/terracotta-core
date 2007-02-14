/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.util.Assert;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ByteCodeUtil implements Opcodes {
  private static final String        AUTOLOCK_PREFIX                   = "@";
  private static final String        NAMED_LOCK_PREFIX                 = "^";
  private static final String        LITERAL_LOCK_PREFIX               = "#";

  public static final String         TC_FIELD_PREFIX                   = "$__tc_";
  public static final String         TC_METHOD_PREFIX                  = "__tc_";
  public static final String         METHOD_RENAME_PREFIX              = TC_METHOD_PREFIX + "wrapped_";

  public static final String         VALUES_GETTER                     = TC_METHOD_PREFIX + "getallfields";
  public static final String         VALUES_GETTER_DESCRIPTION         = "(Ljava/util/Map;)V";
  public static final String         VALUES_SETTER                     = TC_METHOD_PREFIX + "setfield";
  public static final String         VALUES_SETTER_DESCRIPTION         = "(Ljava/lang/String;Ljava/lang/Object;)V";
  public static final String         MANAGED_VALUES_GETTER             = TC_METHOD_PREFIX + "getmanagedfield";
  public static final String         MANAGED_VALUES_GETTER_DESCRIPTION = "(Ljava/lang/String;)Ljava/lang/Object;";
  public static final String         MANAGED_VALUES_SETTER             = TC_METHOD_PREFIX + "setmanagedfield";

  private static final LiteralValues LITERAL_VALUES                    = new LiteralValues();

  public static String[] addInterfaces(String[] existing, String[] toAdd) {
    if (existing == null) { return toAdd; }
    if (toAdd == null) { return existing; }

    List newList = new ArrayList(Arrays.asList(existing));
    Set existingAsSet = Collections.unmodifiableSet(new HashSet(newList));

    for (int i = 0, n = toAdd.length; i < n; i++) {
      if (!existingAsSet.contains(toAdd[i])) {
        newList.add(toAdd[i]);
      }
    }

    return (String[]) newList.toArray(new String[newList.size()]);
  }

  public static boolean isPrimitive(Type t) {
    final int sort = t.getSort();
    switch (sort) {
      case Type.BOOLEAN:
      case Type.BYTE:
      case Type.CHAR:
      case Type.DOUBLE:
      case Type.FLOAT:
      case Type.INT:
      case Type.LONG:
      case Type.SHORT:
        return true;
      default:
        return false;
    }

    // unreachable
  }

  public static String sortToWrapperName(int sort) {
    switch (sort) {
      case Type.BOOLEAN: // '\001'
        return "java/lang/Boolean";

      case Type.CHAR: // '\002'
        return "java/lang/Character";

      case Type.BYTE: // '\003'
        return "java/lang/Byte";

      case Type.SHORT: // '\004'
        return "java/lang/Short";

      case Type.INT: // '\005'
        return "java/lang/Integer";

      case Type.FLOAT: // '\006'
        return "java/lang/Float";

      case Type.LONG: // '\007'
        return "java/lang/Long";

      case Type.DOUBLE: // '\b'
        return "java/lang/Double";
      default:
        throw new AssertionError();
    }

  }

  public static String codeToName(String typeCode) {
    if ((typeCode == null) || (typeCode.length() != 1)) { throw new IllegalArgumentException("invalid type code: "
                                                                                             + typeCode); }
    char code = typeCode.charAt(0);

    switch (code) {
      case 'B': {
        return "byte";
      }
      case 'C': {
        return "char";
      }
      case 'D': {
        return "double";
      }
      case 'F': {
        return "float";
      }
      case 'I': {
        return "int";
      }
      case 'J': {
        return "long";
      }
      case 'S': {
        return "short";
      }
      case 'Z': {
        return "boolean";
      }
      default: {
        throw new IllegalArgumentException("unknown code: " + code);
      }

        // unreachable
    }
  }

  public static boolean isAutolockName(String lockName) {
    return lockName == null ? false : lockName.startsWith(AUTOLOCK_PREFIX);
  }

  public static long objectIdFromLockName(String lockName) {
    if (lockName == null || (!lockName.startsWith(AUTOLOCK_PREFIX))) {
      // make formatter sane
      throw new IllegalArgumentException("not an autolock name: " + lockName);
    }
    return Long.valueOf(lockName.substring(AUTOLOCK_PREFIX.length())).longValue();

  }

  public static boolean isSynthetic(String fieldName) {
    return fieldName.indexOf("$") >= 0;
  }

  public static boolean isTCSynthetic(String fieldName) {
    return fieldName.startsWith(TC_FIELD_PREFIX) || isParent(fieldName);
  }

  public static boolean isSynthetic(int access) {
    return (ACC_SYNTHETIC & access) > 0;
  }

  public static boolean isParent(String fieldName) {
    return fieldName.matches("^this\\$\\d+$");

    // return SERIALIZATION_UTIL.isParent(fieldName);
  }

  public static void pushThis(MethodVisitor c) {
    c.visitVarInsn(ALOAD, 0);
  }

  public static void pushInstanceVariable(MethodVisitor c, String className, String fieldName, String description) {
    c.visitFieldInsn(GETFIELD, className, fieldName, description);
  }

  public static void createParametersToArrayByteCode(MethodVisitor c, Type[] parameters) {
    createParametersToArrayByteCode(c, parameters, 1);
  }

  public static void createParametersToArrayByteCode(MethodVisitor c, Type[] parameters, int offset) {
    c.visitLdcInsn(new Integer(parameters.length));
    c.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    for (int i = 0; i < parameters.length; i++) {
      c.visitInsn(DUP);
      c.visitLdcInsn(new Integer(i));
      addTypeSpecificParameterLoad(c, parameters[i], offset);
      c.visitInsn(AASTORE);
      offset += parameters[i].getSize();
    }
  }

  public static void addTypeSpecificParameterLoad(MethodVisitor c, Type type, int offset) {

    switch (type.getSort()) {
      case Type.ARRAY:
      case Type.OBJECT:
        c.visitVarInsn(type.getOpcode(ILOAD), offset);
        break;
      case Type.BOOLEAN:
        c.visitTypeInsn(NEW, "java/lang/Boolean");
        c.visitInsn(DUP);
        c.visitVarInsn(type.getOpcode(ILOAD), offset);
        c.visitMethodInsn(INVOKESPECIAL, "java/lang/Boolean", "<init>", "(Z)V");
        break;
      case Type.BYTE:
        c.visitTypeInsn(NEW, "java/lang/Byte");
        c.visitInsn(DUP);
        c.visitVarInsn(type.getOpcode(ILOAD), offset);
        c.visitMethodInsn(INVOKESPECIAL, "java/lang/Byte", "<init>", "(B)V");
        break;
      case Type.CHAR:
        c.visitTypeInsn(NEW, "java/lang/Character");
        c.visitInsn(DUP);
        c.visitVarInsn(type.getOpcode(ILOAD), offset);
        c.visitMethodInsn(INVOKESPECIAL, "java/lang/Character", "<init>", "(C)V");
        break;
      case Type.DOUBLE:
        c.visitTypeInsn(NEW, "java/lang/Double");
        c.visitInsn(DUP);
        c.visitVarInsn(type.getOpcode(ILOAD), offset);
        c.visitMethodInsn(INVOKESPECIAL, "java/lang/Double", "<init>", "(D)V");
        break;
      case Type.FLOAT:
        c.visitTypeInsn(NEW, "java/lang/Float");
        c.visitInsn(DUP);
        c.visitVarInsn(type.getOpcode(ILOAD), offset);
        c.visitMethodInsn(INVOKESPECIAL, "java/lang/Float", "<init>", "(F)V");
        break;
      case Type.INT:
        c.visitTypeInsn(NEW, "java/lang/Integer");
        c.visitInsn(DUP);
        c.visitVarInsn(type.getOpcode(ILOAD), offset);
        c.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
        break;
      case Type.LONG:
        c.visitTypeInsn(NEW, "java/lang/Long");
        c.visitInsn(DUP);
        c.visitVarInsn(type.getOpcode(ILOAD), offset);
        c.visitMethodInsn(INVOKESPECIAL, "java/lang/Long", "<init>", "(J)V");
        break;
      case Type.SHORT:
        c.visitTypeInsn(NEW, "java/lang/Short");
        c.visitInsn(DUP);
        c.visitVarInsn(type.getOpcode(ILOAD), offset);
        c.visitMethodInsn(INVOKESPECIAL, "java/lang/Short", "<init>", "(S)V");
        break;
      default:
        throw new AssertionError("can't happen:" + type);
    }
  }

  public static void pushMethodArguments(int callingMethodModifier, String desc, MethodVisitor c) {
    int localVariableOffset = getLocalVariableOffset(callingMethodModifier);
    Type[] args = Type.getArgumentTypes(desc);

    int pos = 0;
    for (int i = 0; i < args.length; i++) {
      c.visitVarInsn(args[i].getOpcode(ILOAD), pos + localVariableOffset);
      pos += args[i].getSize();
    }
  }

  public static int getFirstLocalVariableOffset(int callingMethodModifier, String desc) {
    int localVariableOffset = getLocalVariableOffset(callingMethodModifier);
    Type[] args = Type.getArgumentTypes(desc);
    for (int i = 0; i < args.length; i++) {
      localVariableOffset += args[i].getSize();
    }
    return localVariableOffset;
  }

  public static void prepareStackForMethodCall(int callingMethodModifier, String desc, MethodVisitor c) {
    if (!Modifier.isStatic(callingMethodModifier)) {
      pushThis(c);
    }
    pushMethodArguments(callingMethodModifier, desc, c);
  }

  /**
   * Returns 0 if the method is static. 1 If the method is not.
   */
  public static int getLocalVariableOffset(int methodModifier) {
    return Modifier.isStatic(methodModifier) ? 0 : 1;
  }

  public static String generateVolatileLockName(ObjectID id, String fieldName) {
    Assert.assertNotNull(id);
    return AUTOLOCK_PREFIX + id.toLong() + fieldName;
  }

  public static String generateAutolockName(ObjectID id) {
    Assert.assertNotNull(id);
    return generateAutolockName(id.toLong());
  }

  public static String generateNamedLockName(Object obj) {
    Assert.assertNotNull(obj);
    return NAMED_LOCK_PREFIX + obj;
  }

  public static String generateLiteralLockName(Object obj) {
    Assert.assertNotNull(obj);
    return LITERAL_VALUES.valueFor(obj) + LITERAL_LOCK_PREFIX + obj;
  }

  private static String generateAutolockName(long objectId) {
    return AUTOLOCK_PREFIX + objectId;
  }

  public static String stripGeneratedLockHeader(String lockName) {
    int index = lockName.indexOf(LITERAL_LOCK_PREFIX);
    index = index < 0 ? 1 : index;
    return lockName.substring(index);
  }

  public static String sortToPrimitiveMethodName(int sort) {
    switch (sort) {
      case Type.BOOLEAN: // '\001'
        return "booleanValue";

      case Type.CHAR: // '\002'
        return "charValue";

      case Type.BYTE: // '\003'
        return "byteValue";

      case Type.SHORT: // '\004'
        return "shortValue";

      case Type.INT: // '\005'
        return "intValue";

      case Type.FLOAT: // '\006'
        return "floatValue";

      case Type.LONG: // '\007'
        return "longValue";

      case Type.DOUBLE: // '\b'
        return "doubleValue";
      default:
        throw new AssertionError();
    }
  }

  public static String methodDescriptionToReturnType(String desc) {
    Type type = Type.getReturnType(desc);
    return type.getClassName();
  }

  public static String methodDescriptionToMethodArgument(String desc) {
    Type[] types = Type.getArgumentTypes(desc);
    StringBuffer sb = new StringBuffer("(");
    for (int i = 0; i < types.length; i++) {
      sb.append(types[i].getClassName());
      if (i < types.length - 1) {
        sb.append(",");
      }
    }
    sb.append(")");
    return sb.toString();
  }

  public static String fieldGetterMethod(String fieldName) {
    return TC_METHOD_PREFIX + "get" + fieldName;
  }

  public static String fieldSetterMethod(String fieldName) {
    return TC_METHOD_PREFIX + "set" + fieldName;
  }

}