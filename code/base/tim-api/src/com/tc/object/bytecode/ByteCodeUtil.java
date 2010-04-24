/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.object.ObjectID;
import com.tc.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility methods for working with byte code.
 */
public class ByteCodeUtil implements Opcodes {
  private static final String AUTOLOCK_PREFIX                   = "@";
  private static final String NAMED_LOCK_PREFIX                 = "^";
  private static final String LITERAL_LOCK_PREFIX               = "#";

  public static final String  TC_FIELD_PREFIX                   = "$__tc_";
  public static final String  TC_METHOD_PREFIX                  = "__tc_";
  public static final String  METHOD_RENAME_PREFIX              = TC_METHOD_PREFIX + "wrapped_";
  public static final String  SYNC_METHOD_RENAME_PREFIX         = ByteCodeUtil.METHOD_RENAME_PREFIX + "sync_";
  public static final String  DMI_METHOD_RENAME_PREFIX          = TC_METHOD_PREFIX + "dmi_";

  public static final String  VALUES_GETTER                     = TC_METHOD_PREFIX + "getallfields";
  public static final String  VALUES_GETTER_DESCRIPTION         = "(Ljava/util/Map;)V";
  public static final String  VALUES_SETTER                     = TC_METHOD_PREFIX + "setfield";
  public static final String  VALUES_SETTER_DESCRIPTION         = "(Ljava/lang/String;Ljava/lang/Object;)V";
  public static final String  MANAGED_VALUES_GETTER             = TC_METHOD_PREFIX + "getmanagedfield";
  public static final String  MANAGED_VALUES_GETTER_DESCRIPTION = "(Ljava/lang/String;)Ljava/lang/Object;";
  public static final String  MANAGED_VALUES_SETTER             = TC_METHOD_PREFIX + "setmanagedfield";

  public static final String  MANAGEABLE_CLASS                  = "com/tc/object/bytecode/Manageable";
  public static final String  MANAGEABLE_TYPE                   = "L" + MANAGEABLE_CLASS + ";";

  public static final String  TRANSPARENT_ACCESS_CLASS          = "com/tc/object/bytecode/TransparentAccess";
  public static final String  TRANSPARENT_ACCESS_TYPE           = "L" + TRANSPARENT_ACCESS_CLASS + ";";

  public static final String  NAMEDCLASSLOADER_CLASS            = "com/tc/object/loaders/NamedClassLoader";
  public static final String  NAMEDCLASSLOADER_TYPE             = "L" + NAMEDCLASSLOADER_CLASS + ";";

  public static Method[] purgeTCMethods(final Method[] methods) {
    if ((methods == null) || (methods.length == 0)) { return methods; }

    List rv = new ArrayList();
    for (Method method : methods) {
      if (!method.getName().startsWith(TC_METHOD_PREFIX)) {
        rv.add(method);
      }
    }

    return (Method[]) rv.toArray(new Method[rv.size()]);
  }

  public static String[] addInterface(final String[] existing, final String toAdd) {
    return addInterfaces(existing, new String[] { toAdd });
  }

  /**
   * Given a set of existing interfaces, add some more (without duplicates)
   * 
   * @param existing The existing interfaces
   * @param toAdd The interfaces to add
   * @return A set of interfaces containing all of existing and toAdd with no dups
   */
  public static String[] addInterfaces(final String[] existing, final String[] toAdd) {
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

  /**
   * Check whether the type is a primitve
   * 
   * @param t The ASM type
   * @return True if primitive
   */
  public static boolean isPrimitive(final Type t) {
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

  /**
   * Map from primite type to wrapper class type
   * 
   * @param sort Kind of primitve type as in {@link com.tc.asm.Type#getSort()}
   * @return Wrapper class name, like "java/lang/Boolean"
   */
  public static String sortToWrapperName(final int sort) {
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

  /**
   * Translate type code to type name
   * 
   * @param typeCode Code from bytecode like B, C, etc
   * @return Primitive type name: "byte", "char", etc
   */
  public static String codeToName(final String typeCode) {
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

  /**
   * Determine whether a lock is an autolock based on its name
   * 
   * @param lockName The lock name
   * @return True if an autolock
   */
  public static boolean isAutolockName(final String lockName) {
    return lockName == null ? false : lockName.startsWith(AUTOLOCK_PREFIX);
  }

  /**
   * Get lock ID from autolock name
   * 
   * @param lockName The lock name
   * @return Lock ID
   * @throws IllegalArgumentException If not an autolock
   */
  public static long objectIdFromLockName(final String lockName) {
    if (lockName == null || (!lockName.startsWith(AUTOLOCK_PREFIX))) {
      // make formatter sane
      throw new IllegalArgumentException("not an autolock name: " + lockName);
    }
    return Long.valueOf(lockName.substring(AUTOLOCK_PREFIX.length())).longValue();

  }

  /**
   * Determine whether a field is synthetic
   * 
   * @param fieldName The field name
   * @return True if synthetic
   */
  public static boolean isSynthetic(final String fieldName) {
    return fieldName.indexOf("$") >= 0;
  }

  /**
   * Determine whether a field is synthetic and was added by Terracotta
   * 
   * @param fieldName The field name
   * @return True if synthetic and added by Terracotta
   */
  public static boolean isTCSynthetic(final String fieldName) {
    return fieldName.startsWith(TC_FIELD_PREFIX) || isParent(fieldName);
  }

  /**
   * Determine whether an access modifier code indicates synthetic
   * 
   * @param access Access modifier code
   * @return True if synthetic flag is set
   */
  public static boolean isSynthetic(final int access) {
    return (ACC_SYNTHETIC & access) > 0;
  }

  /**
   * Check whether the field name indicates that this is an inner classes synthetic field referring to the parent "this"
   * reference.
   * 
   * @param fieldName The field name
   * @return True if this field refers to the parent this
   */
  public static boolean isParent(final String fieldName) {
    return fieldName.matches("^this\\$\\d+$");

    // return SERIALIZATION_UTIL.isParent(fieldName);
  }

  /**
   * Add instruction to retrieve "this" from the local vars and load onto the stack
   * 
   * @param c The current method visitor
   */
  public static void pushThis(final MethodVisitor c) {
    c.visitVarInsn(ALOAD, 0);
  }

  /**
   * Add instruction to retrieve specified field in the object on the stack and replace with the field value.
   * 
   * @param c Current method visitor
   * @param className The field class
   * @param fieldName The field name
   * @param description The field type
   */
  public static void pushInstanceVariable(final MethodVisitor c, final String className, final String fieldName,
                                          final String description) {
    c.visitFieldInsn(GETFIELD, className, fieldName, description);
  }

  /**
   * Add instructions to convert the local variables typed with parameters into an array assuming values start at local
   * variable offset of 1
   * 
   * @param c Method visitor
   * @param parameters Paramater to convert
   */
  public static void createParametersToArrayByteCode(final MethodVisitor c, final Type[] parameters) {
    createParametersToArrayByteCode(c, parameters, 1);
  }

  /**
   * Add instructions to convert the parameters into an array
   * 
   * @param c Method visitor
   * @param parameters Paramater types to convert
   * @param offset Offset into local variables for values
   */
  public static void createParametersToArrayByteCode(final MethodVisitor c, final Type[] parameters, int offset) {
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

  /**
   * Add instructions to load type-specific value from local variable onto stack. Primitve values are wrapped into their
   * wrapper object.
   * 
   * @param c Method visitor
   * @param type The type of the variable
   * @param offset The local variable offset
   */
  public static void addTypeSpecificParameterLoad(final MethodVisitor c, final Type type, final int offset) {

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

  /**
   * Add instructions to load method args into the stack
   * 
   * @param callingMethodModifier Calling method modifier
   * @param desc Method descriptor
   * @param c Current method visitor
   */
  public static void pushMethodArguments(final int callingMethodModifier, final String desc, final MethodVisitor c) {
    int localVariableOffset = getLocalVariableOffset(callingMethodModifier);
    Type[] args = Type.getArgumentTypes(desc);

    int pos = 0;
    for (Type arg : args) {
      c.visitVarInsn(arg.getOpcode(ILOAD), pos + localVariableOffset);
      pos += arg.getSize();
    }
  }

  /**
   * Get offset of first local variable after method args
   * 
   * @param callingMethodModifier Calling method modifier
   * @param desc Method descriptor
   * @return First local variable offset
   */
  public static int getFirstLocalVariableOffset(final int callingMethodModifier, final String desc) {
    int localVariableOffset = getLocalVariableOffset(callingMethodModifier);
    Type[] args = Type.getArgumentTypes(desc);
    for (Type arg : args) {
      localVariableOffset += arg.getSize();
    }
    return localVariableOffset;
  }

  /**
   * Push this (if not static) and all method args onto stack
   * 
   * @param callingMethodModifier Calling method modifier
   * @param desc Method descriptor
   * @param c Calling method visitor
   */
  public static void prepareStackForMethodCall(final int callingMethodModifier, final String desc, final MethodVisitor c) {
    if (!Modifier.isStatic(callingMethodModifier)) {
      pushThis(c);
    }
    pushMethodArguments(callingMethodModifier, desc, c);
  }

  /**
   * Returns 0 if the method is static. 1 If the method is not.
   * 
   * @param methodModifier
   * @return 0 if static, 1 if not
   */
  public static int getLocalVariableOffset(final int methodModifier) {
    return Modifier.isStatic(methodModifier) ? 0 : 1;
  }

  /**
   * Get volatile lock name
   * 
   * @param id Object identifier
   * @param field Volatile field
   * @return Lock name
   */
  public static String generateVolatileLockName(final ObjectID id, final String fieldName) {
    Assert.assertNotNull(id);
    return AUTOLOCK_PREFIX + id.toLong() + fieldName;
  }

  /**
   * Get auto lock name for object identifier
   * 
   * @param id Identifier
   * @return Auto lock name
   */
  public static String generateAutolockName(final ObjectID id) {
    Assert.assertNotNull(id);
    return generateAutolockName(id.toLong());
  }

  /**
   * Get named lock name for the lock object
   * 
   * @param obj Lock object
   * @return Named lock name
   */
  public static String generateNamedLockName(final Object obj) {
    Assert.assertNotNull(obj);
    return NAMED_LOCK_PREFIX + obj;
  }

  /**
   * The first argument should be "LiteralValues.valueFor(obj).name()", but I didn't want to slurp in a whole mess of
   * classes into the boot jar by including LiteralValues. It's gross, but ManagerImpl just makes the call itself.
   * 
   * @param literalValueTypeStr Literal value code
   * @param obj The lock object
   */
  public static String generateLiteralLockName(final String literalValueTypeStr, final Object obj) {
    Assert.assertNotNull(obj);
    return literalValueTypeStr + LITERAL_LOCK_PREFIX + obj;
  }

  private static String generateAutolockName(final long objectId) {
    return AUTOLOCK_PREFIX + objectId;
  }

  /**
   * Strip generated lock header from lock name
   * 
   * @param lockName Lock name
   * @return Real lock name
   */
  public static String stripGeneratedLockHeader(final String lockName) {
    int index = lockName.indexOf(LITERAL_LOCK_PREFIX);
    index = index < 0 ? 1 : index;
    return lockName.substring(index);
  }

  /**
   * Convert from {@link com.tc.asm.Type#getSort()} to a primitive method name like "booleanValue".
   * 
   * @param Type kind
   * @return Primitive method name
   */
  public static String sortToPrimitiveMethodName(final int sort) {
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

  /**
   * Get return type (class name) from method descriptor
   * 
   * @param desc Method descriptor
   * @return Return type class name
   */
  public static String methodDescriptionToReturnType(final String desc) {
    Type type = Type.getReturnType(desc);
    return type.getClassName();
  }

  /**
   * Turn method description with byte code types into a readable signature
   * 
   * @param desc The bytecode description
   * @return The method argument form
   */
  public static String methodDescriptionToMethodArgument(final String desc) {
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

  /**
   * Get name of synthetic field getter method added by Terracotta
   * 
   * @param fieldName The field name
   * @return Getter method name
   */
  public static String fieldGetterMethod(final String fieldName) {
    return TC_METHOD_PREFIX + "get" + fieldName;
  }

  /**
   * Get name of synthetic field setter method added by Terracotta
   * 
   * @param fieldName The field name
   * @return Setter method name
   */
  public static String fieldSetterMethod(final String fieldName) {
    return TC_METHOD_PREFIX + "set" + fieldName;
  }

  /**
   * Add instructions to print msg to System.out
   * 
   * @param mv Method visitor
   * @param msg Message to print
   */
  public static void systemOutPrintln(final MethodVisitor mv, final String msg) {
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitLdcInsn(msg);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
  }

  /**
   * Translate class name to file name
   * 
   * @param className The class name "java.lang.String"
   * @return The file name on the classpath: "java/lang/String.class"
   */
  public static final String classNameToFileName(final String className) {
    return className.replace('.', '/') + ".class";
  }

  /**
   * Translate class name to an internal name as used by ASM
   * 
   * @param className The class name "java.lang.String"
   * @return The internal name of the class as required by ASM: "Ljava/lang/String"
   */
  public static final String classNameToInternalName(final String className) {
    return className.replace('.', '/');
  }

  /**
   * Read the bytes defining the class
   * 
   * @param className The class
   * @param loader The classloader
   * @return The underlying bytes
   */
  public static final byte[] getBytesForClass(final String className, final ClassLoader loader)
      throws ClassNotFoundException {
    String resource = classNameToFileName(className);
    InputStream is = loader.getResourceAsStream(resource);
    if (is == null) { throw new ClassNotFoundException("No resource found for class: " + className); }
    try {
      return getBytesForInputstream(is);
    } catch (IOException e) {
      throw new ClassNotFoundException("Error reading bytes for " + resource, e);
    }
  }

  /**
   * Read input stream into a byte array using a 4k buffer. Close stream when done.
   * 
   * @param is Input stream
   * @return Bytes read from stream
   * @throws IOException If there is an error reading the stream
   */
  public static final byte[] getBytesForInputstream(final InputStream is) throws IOException {
    final int size = 4096;
    byte[] buffer = new byte[size];
    ByteArrayOutputStream baos = new ByteArrayOutputStream(size);

    int read;
    try {
      while ((read = is.read(buffer, 0, size)) > 0) {
        baos.write(buffer, 0, read);
      }
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
    }

    return baos.toByteArray();
  }

  /**
   * Assign the default value to the variable
   * 
   * @param variable The local variable to which the default value will be assigned
   * @param c MethodVisitor
   * @param type Type of the variable
   */
  public static void pushDefaultValue(final int variable, final MethodVisitor c, final Type type) {
    if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
      c.visitInsn(ACONST_NULL);
      c.visitVarInsn(ASTORE, variable);
    } else {
      c.visitInsn(getConstant0(type));
      c.visitVarInsn(type.getOpcode(ISTORE), variable);
    }
  }

  /**
   * Return the constant 0 value according to the type.
   * 
   * @param type
   */
  private static int getConstant0(final Type type) {
    if (type.getSort() == Type.INT) { return ICONST_0; }
    if (type.getSort() == Type.LONG) { return LCONST_0; }
    if (type.getSort() == Type.SHORT) { return ICONST_0; }
    if (type.getSort() == Type.DOUBLE) { return DCONST_0; }
    if (type.getSort() == Type.BOOLEAN) { return ICONST_0; }
    if (type.getSort() == Type.FLOAT) { return FCONST_0; }
    if (type.getSort() == Type.BYTE) { return ICONST_0; }
    if (type.getSort() == Type.CHAR) { return ICONST_0; }

    throw new AssertionError("Cannot determine constant 0 of type: " + type.getDescriptor());
  }
}