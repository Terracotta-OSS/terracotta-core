/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Iterator;


import com.tc.aspectwerkz.exception.WrappedRuntimeException;
import com.tc.aspectwerkz.transform.TransformationConstants;

/**
 * Helper class with utility methods for working with the java.lang.reflect.* package.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class ReflectHelper {

  private final static Method OBJECT_EQUALS;
  private final static Method OBJECT_HASH_CODE;
  private final static Method OBJECT_GET_CLASS;
  private final static Method OBJECT_TO_STRING;
  private final static Method OBJECT_CLONE;
  private final static Method OBJECT_WAIT_1;
  private final static Method OBJECT_WAIT_2;
  private final static Method OBJECT_WAIT_3;
  private final static Method OBJECT_NOTIFY;
  private final static Method OBJECT_NOTIFY_ALL;
  private final static Method OBJECT_FINALIZE;

  static {
    Class clazz = Object.class;
    try {
      OBJECT_EQUALS = clazz.getDeclaredMethod("equals", new Class[]{clazz});
      OBJECT_HASH_CODE = clazz.getDeclaredMethod("hashCode", new Class[]{});
      OBJECT_GET_CLASS = clazz.getDeclaredMethod("getClass", new Class[]{});
      OBJECT_CLONE = clazz.getDeclaredMethod("clone", new Class[]{});
      OBJECT_TO_STRING = clazz.getDeclaredMethod("toString", new Class[]{});
      OBJECT_WAIT_1 = clazz.getDeclaredMethod("wait", new Class[]{});
      OBJECT_WAIT_2 = clazz.getDeclaredMethod("wait", new Class[]{long.class});
      OBJECT_WAIT_3 = clazz.getDeclaredMethod("wait", new Class[]{long.class, int.class});
      OBJECT_NOTIFY = clazz.getDeclaredMethod("notify", new Class[]{});
      OBJECT_NOTIFY_ALL = clazz.getDeclaredMethod("notifyAll", new Class[]{});
      OBJECT_FINALIZE = clazz.getDeclaredMethod("finalize", new Class[]{});
    } catch (NoSuchMethodException e) {
      throw new WrappedRuntimeException(e);
    }
  }

//    /**
//     * Creates a sorted method list of all the methods in the class and super classes, including package private ones.
//     *
//     * @param klass the class with the methods
//     * @return the sorted method list
//     */
//    public static List createSortedMethodList(final Class klass) {
//        if (klass == null) {
//            throw new IllegalArgumentException("class to sort method on can not be null");
//        }
//
//        // getDefault all public methods including the inherited methods
//        java.lang.reflect.Method[] methods = klass.getMethods();
//        java.lang.reflect.Method[] privateMethods = klass.getDeclaredMethods();
//        List methodList = new ArrayList(methods.length);
//        for (int i = 0; i < methods.length; i++) {
//            Method method = methods[i];
//            if (ReflectHelper.isUserDefinedMethod(method)) {
//                methodList.add(method);
//            }
//        }
//        // lookup in declared method to add "package private" method (which can be Pointcut with signatures)
//        for (int i = 0; i < privateMethods.length; i++) {
//            Method method = privateMethods[i];
//            if (ReflectHelper.isUserDefinedMethod(method) && !methodList.contains(method)) {
//                methodList.add(method);
//            }
//        }
//
//        Collections.sort(methodList, MethodComparator.getInstance(MethodComparator.NORMAL_METHOD));
//        return methodList;
//    }

//    /**
//     * Creates a sorted method list of all the methods in the class and super classes, if and only
//     * if those are part of the given list of interfaces declared method
//     *
//     * @param klass                    the class with the methods
//     * @param interfaceDeclaredMethods the list of interface declared methods
//     * @return the sorted method list
//     */
//    public static List createInterfaceDefinedSortedMethodList(final Class klass, List interfaceDeclaredMethods) {
//        if (klass == null) {
//            throw new IllegalArgumentException("class to sort method on can not be null");
//        }
//
//        // getDefault all public methods including the inherited methods
//        java.lang.reflect.Method[] methods = klass.getMethods();
//        java.lang.reflect.Method[] privateMethods = klass.getDeclaredMethods();
//        List methodList = new ArrayList(methods.length);
//        for (int i = 0; i < methods.length; i++) {
//            Method method = methods[i];
//            if (ReflectHelper.isUserDefinedMethod(method) && isDeclaredByInterface(method, interfaceDeclaredMethods)) {
//                methodList.add(method);
//            }
//        }
//        // lookup in declared method to add "package private" method (which can be Pointcut with signatures)
//        for (int i = 0; i < privateMethods.length; i++) {
//            Method method = privateMethods[i];
//            if (ReflectHelper.isUserDefinedMethod(method) && isDeclaredByInterface(method, interfaceDeclaredMethods)
//                && !methodList.contains(method)) {
//                methodList.add(method);
//            }
//        }
//
//        Collections.sort(methodList, MethodComparator.getInstance(MethodComparator.NORMAL_METHOD));
//        return methodList;
//    }

  /**
   * Returns true if the method is declared by one of the given method declared in an interface class
   *
   * @param method
   * @param interfaceDeclaredMethods
   * @return
   */
  private static boolean isDeclaredByInterface(Method method, List interfaceDeclaredMethods) {
    boolean match = false;
    for (Iterator iterator = interfaceDeclaredMethods.iterator(); iterator.hasNext();) {
      Method methodIt = (Method) iterator.next();
      if (method.getName().equals(methodIt.getName())) {
        if (method.getParameterTypes().length == methodIt.getParameterTypes().length) {
          boolean matchArgs = true;
          for (int i = 0; i < method.getParameterTypes().length; i++) {
            // BAD ! will lead to nested loading while system not ready
            // => if introduced method has target class in its signature weaving will not occur
            // properly
            // ?? should we use ASMInfo ?
            Class parameterType = method.getParameterTypes()[i];
            if (parameterType.getName().equals(methodIt.getParameterTypes()[i].getName())) {
              ;
            } else {
              matchArgs = false;
              break;
            }
          }
          if (matchArgs) {
            match = true;
            break;
          }
        }
      }
    }
    return match;
  }

  /**
   * Returns true if the method is not of on java.lang.Object and is not an AW generated one
   *
   * @param method
   * @return
   */
  private static boolean isUserDefinedMethod(final Method method) {
    if (!method.equals(OBJECT_EQUALS)
            && !method.equals(OBJECT_HASH_CODE)
            && !method.equals(OBJECT_GET_CLASS)
            && !method.equals(OBJECT_TO_STRING)
            && !method.equals(OBJECT_CLONE)
            && !method.equals(OBJECT_WAIT_1)
            && !method.equals(OBJECT_WAIT_2)
            && !method.equals(OBJECT_WAIT_3)
            && !method.equals(OBJECT_NOTIFY)
            && !method.equals(OBJECT_NOTIFY_ALL)
            && !method.getName().startsWith(TransformationConstants.SYNTHETIC_MEMBER_PREFIX)
            && !method.getName().startsWith(TransformationConstants.ORIGINAL_METHOD_PREFIX)
            && !method.getName().startsWith(TransformationConstants.ASPECTWERKZ_PREFIX)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Converts modifiers represented in a string array to an int.
   *
   * @param modifiers the modifiers as strings
   * @return the modifiers as an int
   */
  public static int getModifiersAsInt(final String[] modifiers) {
    int accessFlags = 0;
    for (int i = 0; i < modifiers.length; i++) {
      if (modifiers[i].equals("abstract")) {
        accessFlags |= Modifier.ABSTRACT;
      } else if (modifiers[i].equals("final")) {
        accessFlags |= Modifier.FINAL;
      } else if (modifiers[i].equals("interface")) {
        accessFlags |= Modifier.INTERFACE;
      } else if (modifiers[i].equals("native")) {
        accessFlags |= Modifier.NATIVE;
      } else if (modifiers[i].equals("private")) {
        accessFlags |= Modifier.PRIVATE;
      } else if (modifiers[i].equals("protected")) {
        accessFlags |= Modifier.PROTECTED;
      } else if (modifiers[i].equals("public")) {
        accessFlags |= Modifier.PUBLIC;
      } else if (modifiers[i].equals("static")) {
        accessFlags |= Modifier.STATIC;
      } else if (modifiers[i].equals("strict")) {
        accessFlags |= Modifier.STRICT;
      } else if (modifiers[i].equals("synchronized")) {
        accessFlags |= Modifier.SYNCHRONIZED;
      } else if (modifiers[i].equals("transient")) {
        accessFlags |= Modifier.TRANSIENT;
      } else if (modifiers[i].equals("volatile")) {
        accessFlags |= Modifier.VOLATILE;
      }
    }
    return accessFlags;
  }

  /**
   * Calculate the hash for a class.
   *
   * @param klass the class
   * @return the hash
   */
  public static int calculateHash(final Class klass) {
    return klass.getName().hashCode();
  }

  /**
   * Calculate the hash for a method.
   *
   * @param method the method
   * @return the hash
   */
  public static int calculateHash(final Method method) {
    int hash = 17;
    hash = (37 * hash) + method.getName().hashCode();
    for (int i = 0; i < method.getParameterTypes().length; i++) {
      Class type = method.getParameterTypes()[i];
      hash = (37 * hash) + type.getName().hashCode();
    }
    return hash;
  }

  /**
   * Calculate the hash for a constructor.
   *
   * @param constructor the constructor
   * @return the hash
   */
  public static int calculateHash(final Constructor constructor) {
    int hash = 17;
    hash = (37 * hash) + TransformationConstants.INIT_METHOD_NAME.hashCode();
    for (int i = 0; i < constructor.getParameterTypes().length; i++) {
      Class type = constructor.getParameterTypes()[i];
      hash = (37 * hash) + type.getName().replace('/', '.').hashCode();
    }
    return hash;
  }

  /**
   * Calculate the hash for a field.
   *
   * @param field the field
   * @return the hash
   */
  public static int calculateHash(final Field field) {
    int hash = 17;
    hash = (37 * hash) + field.getName().hashCode();
    Class type = field.getType();
    hash = (37 * hash) + type.getName().hashCode();
    return hash;
  }

  /**
   * Checks if the class is a of a primitive type, if so create and return the class for the type else return null.
   *
   * @param className
   * @return the class for the primitive type or null
   */
  public static Class getPrimitiveClass(final String className) {
    if (className.equals("void")) {
      return void.class;
    } else if (className.equals("long")) {
      return long.class;
    } else if (className.equals("int")) {
      return int.class;
    } else if (className.equals("short")) {
      return short.class;
    } else if (className.equals("double")) {
      return double.class;
    } else if (className.equals("float")) {
      return float.class;
    } else if (className.equals("byte")) {
      return byte.class;
    } else if (className.equals("boolean")) {
      return boolean.class;
    } else if (className.equals("char")) {
      return char.class;
    } else {
      return null;
    }
  }

  /**
   * Returns JVM type signature for given class.
   *
   * @param cl
   * @return
   */
  public static String getClassSignature(Class cl) {
    StringBuffer sbuf = new StringBuffer();
    while (cl.isArray()) {
      sbuf.append('[');
      cl = cl.getComponentType();
    }
    if (cl.isPrimitive()) {
      if (cl == Integer.TYPE) {
        sbuf.append('I');
      } else if (cl == Byte.TYPE) {
        sbuf.append('B');
      } else if (cl == Long.TYPE) {
        sbuf.append('J');
      } else if (cl == Float.TYPE) {
        sbuf.append('F');
      } else if (cl == Double.TYPE) {
        sbuf.append('D');
      } else if (cl == Short.TYPE) {
        sbuf.append('S');
      } else if (cl == Character.TYPE) {
        sbuf.append('C');
      } else if (cl == Boolean.TYPE) {
        sbuf.append('Z');
      } else if (cl == Void.TYPE) {
        sbuf.append('V');
      } else {
        throw new InternalError();
      }
    } else {
      sbuf.append('L' + cl.getName().replace('.', '/') + ';');
    }
    return sbuf.toString();
  }

  /**
   * Returns JVM type signature for a constructor.
   *
   * @param constructor
   * @return
   */
  public static String getConstructorSignature(final Constructor constructor) {
    return getMethodSignature(constructor.getParameterTypes(), Void.TYPE);
  }

  /**
   * Returns JVM type signature for a field.
   *
   * @param field
   * @return
   */
  public static String getFieldSignature(final Field field) {
    return getClassSignature(field.getType());
  }

  /**
   * Returns JVM type signature for a method.
   *
   * @param method
   * @return
   */
  public static String getMethodSignature(final Method method) {
    return getMethodSignature(method.getParameterTypes(), method.getReturnType());
  }

  /**
   * Returns JVM type signature for given list of parameters and return type.
   *
   * @param paramTypes
   * @param retType
   * @return
   */
  private static String getMethodSignature(Class[] paramTypes, Class retType) {
    StringBuffer sbuf = new StringBuffer();
    sbuf.append('(');
    for (int i = 0; i < paramTypes.length; i++) {
      sbuf.append(getClassSignature(paramTypes[i]));
    }
    sbuf.append(')');
    sbuf.append(getClassSignature(retType));
    return sbuf.toString();
  }
}
