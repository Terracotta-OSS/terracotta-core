/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect;


import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.transform.TransformationConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility method for manipulating and managing ClassInfo hierarchies.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class ClassInfoHelper {
  private static final List EMPTY_ARRAY_LIST = new ArrayList();
  private static final String OBJECT_CLASS_NAME = "java.lang.Object";

  /**
   * Checks if a class has a certain class as super class or interface, somewhere up in the class hierarchy.
   *
   * @param classInfo      the meta-data for the class to parse
   * @param superclassName the name of the super class or interface
   * @return true if we have a parse else false
   */
  public static boolean instanceOf(final ClassInfo classInfo, final String superclassName) {
    return implementsInterface(classInfo, superclassName) || extendsSuperClass(classInfo, superclassName);
  }

  /**
   * Checks if a class implements a certain inteface, somewhere up in the class hierarchy, excluding
   * itself.
   *
   * @param classInfo
   * @param interfaceName
   * @return true if we have a parse else false
   */
  public static boolean implementsInterface(final ClassInfo classInfo, final String interfaceName) {
    if ((classInfo == null) || (interfaceName == null)) {
      return false;
    } else {
      //TODO: we could lookup in names onlny FIRST to not trigger lazy getInterfaces() stuff
      ClassInfo[] interfaces = classInfo.getInterfaces();
      for (int i = 0; i < interfaces.length; i++) {
        ClassInfo anInterface = interfaces[i];
        if (interfaceName.equals(anInterface.getName())) {
          return true;
        } else if (ClassInfoHelper.implementsInterface(anInterface, interfaceName)) {
          return true;
        }
      }
      return ClassInfoHelper.implementsInterface(classInfo.getSuperclass(), interfaceName);
    }
  }

  /**
   * Checks if a class has a certain class as super class, somewhere up in the class hierarchy.
   *
   * @param classInfo the meta-data for the class to parse
   * @param className the name of the super class
   * @return true if we have a parse else false
   */
  public static boolean extendsSuperClass(final ClassInfo classInfo, final String className) {
    if ((classInfo == null) || (className == null)) {
      return false;
      // TODO odd comparison
//        } else if (classInfo.getName().equals(null)) {
//            return true;
    } else if (className.equals(classInfo.getName())) {
      return true;
    } else {
      return ClassInfoHelper.extendsSuperClass(classInfo.getSuperclass(), className);
    }
  }

  /**
   * Creates a method list of all the methods in the class and super classes, including package private ones.
   * Inherited methods are last in the list.
   *
   * @param klass the class with the methods
   * @return the sorted method list
   */
  public static List createMethodList(final ClassInfo klass) {
    if (klass == null) {
      return EMPTY_ARRAY_LIST;
    }

    // getDefault this klass methods
    List methods = new ArrayList();
    MethodInfo[] methodInfos = klass.getMethods();
    for (int i = 0; i < methodInfos.length; i++) {
      MethodInfo methodInfo = methodInfos[i];
      if (isUserDefinedMethod(methodInfo)) {
        methods.add(methodInfo);
      }
    }

    // get all the inherited methods, as long as they are user defined ones
    ClassInfo superClass = klass.getSuperclass();
    if (superClass != null && !superClass.getName().equals(OBJECT_CLASS_NAME)) {
      List parentMethods = createMethodList(superClass);
      // merge the method list (parent discovered methods are not added if overrided in this klass)
      for (Iterator iterator = parentMethods.iterator(); iterator.hasNext();) {
        MethodInfo parentMethod = (MethodInfo) iterator.next();
        if (!methods.contains(parentMethod)) { // TODO seems to work but ? since tied to declaringTypeName
          methods.add(parentMethod);
        }
      }
    }
    return methods;
  }

  /**
   * Collects the methods from all the interface and its super interfaces.
   *
   * @param interfaceClassInfo
   * @return list of methods declared in given class interfaces
   */
  public static List collectMethodsFromInterface(final ClassInfo interfaceClassInfo) {
    final List interfaceDeclaredMethods = new ArrayList();
    final List sortedMethodList = createMethodList(interfaceClassInfo);
    for (Iterator it = sortedMethodList.iterator(); it.hasNext();) {
      MethodInfo methodInfo = (MethodInfo) it.next();
      if (methodInfo.getDeclaringType().getName().equals(OBJECT_CLASS_NAME)) {
        continue;
      }
      interfaceDeclaredMethods.add(methodInfo);
    }
    // grab methods from all super classes' interfaces
    ClassInfo superClass = interfaceClassInfo.getSuperclass();
    if (superClass != null && !superClass.getName().equals(OBJECT_CLASS_NAME)) {
      interfaceDeclaredMethods.addAll(collectMethodsFromInterfacesImplementedBy(superClass));
    }
    return interfaceDeclaredMethods;
  }

  /**
   * Collects the methods from all the interfaces of the class and its super interfaces.
   *
   * @param classInfo
   * @return list of methods declared in given class interfaces
   */
  public static List collectMethodsFromInterfacesImplementedBy(final ClassInfo classInfo) {
    final List interfaceDeclaredMethods = new ArrayList();
    ClassInfo[] interfaces = classInfo.getInterfaces();

    // grab methods from all interfaces and their super interfaces
    for (int i = 0; i < interfaces.length; i++) {
      final List sortedMethodList = createMethodList(interfaces[i]);
      for (Iterator it = sortedMethodList.iterator(); it.hasNext();) {
        MethodInfo methodInfo = (MethodInfo) it.next();
        if (methodInfo.getDeclaringType().getName().equals(OBJECT_CLASS_NAME)) {
          continue;
        }
        interfaceDeclaredMethods.add(methodInfo);
      }
    }
    // grab methods from all super classes' interfaces
    ClassInfo superClass = classInfo.getSuperclass();
    if (superClass != null && !superClass.getName().equals(OBJECT_CLASS_NAME)) {
      interfaceDeclaredMethods.addAll(collectMethodsFromInterfacesImplementedBy(superClass));
    }
    return interfaceDeclaredMethods;
  }

  /**
   * Creates a method list of all the methods in the class and super classes, if and only
   * if those are part of the given list of interfaces declared methods.
   *
   * @param klass                    the class with the methods
   * @param interfaceDeclaredMethods the list of interface declared methods
   * @return the sorted method list
   */
  public static List createInterfaceDefinedMethodList(final ClassInfo klass,
                                                      final List interfaceDeclaredMethods) {
    if (klass == null) {
      throw new IllegalArgumentException("class to sort method on can not be null");
    }
    // getDefault all methods including the inherited methods
    List methodList = new ArrayList();
    for (Iterator iterator = createMethodList(klass).iterator(); iterator.hasNext();) {
      MethodInfo methodInfo = (MethodInfo) iterator.next();
      if (isDeclaredByInterface(methodInfo, interfaceDeclaredMethods)) {
        methodList.add(methodInfo);
      }
    }
    return methodList;
  }

  /**
   * Returns true if the method is not of on java.lang.Object and is not an AW generated one
   *
   * @param method
   * @return bool
   */
  private static boolean isUserDefinedMethod(final MethodInfo method) {
    if (!method.getName().startsWith(TransformationConstants.SYNTHETIC_MEMBER_PREFIX)
            && !method.getName().startsWith(TransformationConstants.ORIGINAL_METHOD_PREFIX)
            && !method.getName().startsWith(TransformationConstants.ASPECTWERKZ_PREFIX)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns true if the method is declared by one of the given method declared in an interface class
   *
   * @param method
   * @param interfaceDeclaredMethods
   * @return bool
   */
  private static boolean isDeclaredByInterface(final MethodInfo method, final List interfaceDeclaredMethods) {
    boolean match = false;
    for (Iterator iterator = interfaceDeclaredMethods.iterator(); iterator.hasNext();) {
      MethodInfo methodIt = (MethodInfo) iterator.next();
      if (method.getName().equals(methodIt.getName())) {
        // TODO - using param type NAME should be enough - optimize
        if (method.getParameterTypes().length == methodIt.getParameterTypes().length) {
          boolean matchArgs = true;
          for (int i = 0; i < method.getParameterTypes().length; i++) {
            ClassInfo parameterType = method.getParameterTypes()[i];
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
   * Collects all the interface from the given class including the one from its super class.
   *
   * @param classInfo
   * @return list of interface classInfo declared in given class and its hierarchy in correct order
   */
  public static List collectInterfaces(final ClassInfo classInfo) {
    final List interfaceList = new ArrayList();
    final Set interfaceNames = new HashSet();
    for (int i = 0; i < classInfo.getInterfaces().length; i++) {
      ClassInfo interfaceInfo = classInfo.getInterfaces()[i];
      interfaceList.add(interfaceInfo);
      interfaceNames.add(interfaceInfo.getName());
    }
    for (ClassInfo superClass = classInfo.getSuperclass(); superClass != null; superClass =
            superClass.getSuperclass()) {
      for (int i = 0; i < superClass.getInterfaces().length; i++) {
        ClassInfo interfaceInfo = superClass.getInterfaces()[i];
        if (!interfaceNames.contains(interfaceInfo.getName())) {
          interfaceList.add(interfaceInfo);
          interfaceNames.add(interfaceInfo.getName());
        }
      }
    }
    return interfaceList;
  }

  /**
   * Checks if a set of interfaces has any clashes, meaning any methods with the same name and signature.
   *
   * @param interfacesToAdd
   * @param loader
   * @return boolean
   */
  public static boolean hasMethodClash(final Set interfacesToAdd, final ClassLoader loader) {
    // build up the validation structure
    Map methodMap = new HashMap();
    for (Iterator it = interfacesToAdd.iterator(); it.hasNext();) {
      ClassInfo classInfo = AsmClassInfo.getClassInfo((String) it.next(), loader);

      List methods = collectMethodsFromInterface(classInfo);

      for (Iterator it2 = methods.iterator(); it2.hasNext();) {
        MethodInfo methodInfo = (MethodInfo) it2.next();
        String key = methodInfo.getName() + ':' + methodInfo.getSignature();
        if (!methodMap.containsKey(key)) {
          methodMap.put(key, new ArrayList());
        }
        ((List) methodMap.get(key)).add(classInfo.getName());
      }
    }

    // validate the structure
    for (Iterator it = methodMap.entrySet().iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry) it.next();
      String key = (String) entry.getKey();
      List interfaceNames = (List) entry.getValue();
      if (interfaceNames.size() > 1) {
        StringBuffer msg = new StringBuffer();
        msg.append("can not add interfaces [");
        for (Iterator it2 = interfaceNames.iterator(); it2.hasNext();) {
          String interfaceName = (String) it2.next();
          msg.append(interfaceName);
          if (it2.hasNext()) {
            msg.append(',');
          }
        }
        msg.append("] since they all have method [");
        msg.append(key);
        msg.append(']');
        System.out.println("AW::WARNING - " + msg.toString());
        return true;
      }
    }
    return false;
  }
}