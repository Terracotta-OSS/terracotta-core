/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.java;

import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.backport175.bytecode.AnnotationReader;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.ConstructorInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ReflectHelper;
import com.tc.aspectwerkz.reflect.StaticInitializationInfo;
import com.tc.aspectwerkz.reflect.StaticInitializationInfoImpl;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.transform.TransformationConstants;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Implementation of the ClassInfo interface for java.lang.reflect.*.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class JavaClassInfo implements ClassInfo {
  /**
   * The class.
   */
  // TODO might be safer to wrap this member in a weak ref
  private final Class m_class;

  /**
   * The name of the class.
   */
  private String m_name;

  /**
   * The signature of the class.
   */
  private String m_signature;

  /**
   * Is the class an interface.
   */
  private boolean m_isInterface = false;

  /**
   * Is the class a primitive type.
   */
  private boolean m_isPrimitive = false;

  /**
   * Is the class of type array.
   */
  private boolean m_isArray = false;

  /**
   * A list with the <code>ConstructorInfo</code> instances.
   */
  private final HashMap m_constructors = new HashMap();

  /**
   * A list with the <code>MethodInfo</code> instances.
   */
  private final HashMap m_methods = new HashMap();

  /**
   * A list with the <code>FieldInfo</code> instances.
   */
  private final HashMap m_fields = new HashMap();

  /**
   * A list with the interfaces.
   */
  private ClassInfo[] m_interfaces = null;

  /**
   * The super class.
   */
  private ClassInfo m_superClass = null;

  /**
   * The component type if array type.
   */
  private ClassInfo m_componentType = null;

  /**
   * The class info repository.
   */
  private final JavaClassInfoRepository m_classInfoRepository;

  /**
   * Lazy, the static initializer info or null if not present
   */
  private StaticInitializationInfo m_staticInitializer = null;

  /**
   * Creates a new class meta data instance.
   *
   * @param klass
   */
  JavaClassInfo(final Class klass) {
    if (klass == null) {
      throw new IllegalArgumentException("class can not be null");
    }
    m_class = klass;

    m_signature = ReflectHelper.getClassSignature(klass);

    m_classInfoRepository = JavaClassInfoRepository.getRepository(klass.getClassLoader());
    m_isInterface = klass.isInterface();
    if (klass.isPrimitive()) {
      m_name = klass.getName();
      m_isPrimitive = true;
    } else if (klass.getComponentType() != null) {
      m_name = convertJavaArrayTypeNameToHumanTypeName(klass.getName());
      m_isArray = true;
      m_interfaces = new ClassInfo[0];
    } else {
      m_name = klass.getName();
      Method[] methods = m_class.getDeclaredMethods();
      for (int i = 0; i < methods.length; i++) {
        Method method = methods[i];
        m_methods.put(new Integer(ReflectHelper.calculateHash(method)), new JavaMethodInfo(method, this));
      }
      Constructor[] constructors = m_class.getDeclaredConstructors();
      for (int i = 0; i < constructors.length; i++) {
        Constructor constructor = constructors[i];
        m_constructors.put(new Integer(ReflectHelper.calculateHash(constructor)), //
                           new JavaConstructorInfo(constructor, this));
      }
      Field[] fields = m_class.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].getName().startsWith(TransformationConstants.ASPECTWERKZ_PREFIX)) {
          continue;
        }
        Field field = fields[i];
        m_fields.put(new Integer(ReflectHelper.calculateHash(field)), new JavaFieldInfo(field, this));
      }
    }
    m_classInfoRepository.addClassInfo(this);
  }

  /**
   * Returns the class info for a specific class.
   *
   * @return the class info
   */
  public static ClassInfo getClassInfo(final Class clazz) {
    JavaClassInfoRepository repository = JavaClassInfoRepository.getRepository(clazz.getClassLoader());
    ClassInfo classInfo = repository.getClassInfo(clazz.getName());
    if (classInfo == null) {
      classInfo = new JavaClassInfo(clazz);
    }
    return classInfo;
  }

  /**
   * Returns the annotations.
   *
   * @return the annotations
   */
  public AnnotationElement.Annotation[] getAnnotations() {
    return getAnnotationReader().getAnnotationElements();
  }

  /**
   * Returns the name of the class.
   *
   * @return the name of the class
   */
  public String getName() {
    return m_name.replace('/', '.');
  }

  /**
   * Checks if the class has a static initalizer.
   *
   * @return
   */
  public boolean hasStaticInitializer() {
    ClassInfo classInfo = AsmClassInfo.getClassInfo(getName(), getClassLoader());
    return classInfo.hasStaticInitializer();
  }

  /**
   * Returns the static initializer info of the current underlying class if any.
   *
   * @see ClassInfo#staticInitializer()
   */
  public StaticInitializationInfo staticInitializer() {
    if (hasStaticInitializer() && m_staticInitializer == null) {
      m_staticInitializer = new StaticInitializationInfoImpl(this);
    }
    return m_staticInitializer;
  }

  /**
   * Returns the signature for the element.
   *
   * @return the signature for the element
   */
  public String getSignature() {
    return m_signature;
  }

  public String getGenericsSignature() {
    // ClassInfo classInfo = AsmClassInfo.getClassInfo(getName(), getClassLoader());
    return null;
  }

  /**
   * Returns the class modifiers.
   *
   * @return the class modifiers
   */
  public int getModifiers() {
    return m_class.getModifiers();
  }

  /**
   * Returns the class loader that loaded this class.
   *
   * @return the class loader
   */
  public ClassLoader getClassLoader() {
    return m_class.getClassLoader();
  }

  /**
   * Returns a constructor info by its hash.
   *
   * @param hash
   * @return
   */
  public ConstructorInfo getConstructor(final int hash) {
    ConstructorInfo constructor = (ConstructorInfo) m_constructors.get(new Integer(hash));
    if (constructor == null && getSuperclass() != null) {
      constructor = getSuperclass().getConstructor(hash);
    }
    return constructor;
  }

  /**
   * Returns a list with all the constructors info.
   *
   * @return the constructors info
   */
  public ConstructorInfo[] getConstructors() {
    ConstructorInfo[] methodInfos = new ConstructorInfo[m_constructors.size()];
//    Object[] values = m_constructors.getValues();
//    for (int i = 0; i < values.length; i++) {
//    methodInfos[i] = (ConstructorInfo) values[i];
    int i = 0;
    for (Iterator it = m_constructors.values().iterator(); it.hasNext();) {
      methodInfos[i++] = (ConstructorInfo) it.next();
    }
    return methodInfos;
  }

  /**
   * Returns a method info by its hash.
   *
   * @param hash
   * @return
   */
  public MethodInfo getMethod(final int hash) {
    MethodInfo method = (MethodInfo) m_methods.get(new Integer(hash));
    if (method == null) {
      for (int i = 0; i < getInterfaces().length; i++) {
        method = getInterfaces()[i].getMethod(hash);
        if (method != null) {
          break;
        }
      }
    }
    if (method == null && getSuperclass() != null) {
      method = getSuperclass().getMethod(hash);
    }
    return method;
  }

  /**
   * Returns a list with all the methods info.
   *
   * @return the methods info
   */
  public MethodInfo[] getMethods() {
    MethodInfo[] methodInfos = new MethodInfo[m_methods.size()];
//    Object[] values = m_methods.getValues();
//    for (int i = 0; i < values.length; i++) {
//      methodInfos[i] = (MethodInfo) values[i];
    int i = 0;
    for (Iterator it = m_methods.values().iterator(); it.hasNext();) {
      methodInfos[i++] = (MethodInfo) it.next();
    }
    return methodInfos;
  }

  /**
   * Returns a field info by its hash.
   *
   * @param hash
   * @return
   */
  public FieldInfo getField(final int hash) {
    FieldInfo field = (FieldInfo) m_fields.get(new Integer(hash));
    if (field == null && getSuperclass() != null) {
      field = getSuperclass().getField(hash);
    }
    if (field == null) {
      // Trying to find constants in Interfaces
      ClassInfo[] interfaces = getInterfaces();
      for (int i = 0; i < interfaces.length; i++) {
        ClassInfo ifc = interfaces[i];
        field = ifc.getField(hash);
        if (field != null)
          break;
      }
    }
    return field;
  }

  /**
   * Returns a list with all the field info.
   *
   * @return the field info
   */
  public FieldInfo[] getFields() {
    FieldInfo[] fieldInfos = new FieldInfo[m_fields.size()];
//    Object[] values = m_fields.getValues();
//    for (int i = 0; i < values.length; i++) {
//      fieldInfos[i] = (FieldInfo) values[i];
    int i = 0;
    for (Iterator it = m_methods.values().iterator(); it.hasNext();) {
      fieldInfos[i++] = (FieldInfo) it.next();
    }
    return fieldInfos;
  }

  /**
   * Returns the interfaces.
   *
   * @return the interfaces
   */
  public synchronized ClassInfo[] getInterfaces() {
    if (m_interfaces == null) {
      Class[] interfaces = m_class.getInterfaces();
      m_interfaces = new ClassInfo[interfaces.length];
      for (int i = 0; i < interfaces.length; i++) {
        Class anInterface = interfaces[i];
        ClassInfo classInfo = JavaClassInfo.getClassInfo(anInterface);
        m_interfaces[i] = classInfo;
        if (!m_classInfoRepository.hasClassInfo(anInterface.getName())) {
          m_classInfoRepository.addClassInfo(classInfo);
        }
      }
    }
    return m_interfaces;
  }

  /**
   * Returns the super class.
   *
   * @return the super class
   */
  public ClassInfo getSuperclass() {
    if (m_superClass == null) {
      Class superclass = m_class.getSuperclass();
      if (superclass != null) {
        if (m_classInfoRepository.hasClassInfo(superclass.getName())) {
          m_superClass = m_classInfoRepository.getClassInfo(superclass.getName());
        } else {
          m_superClass = JavaClassInfo.getClassInfo(superclass);
          m_classInfoRepository.addClassInfo(m_superClass);
        }
      }
    }
    return m_superClass;
  }

  /**
   * Returns the component type if array type else null.
   *
   * @return the component type
   */
  public ClassInfo getComponentType() {
    if (isArray() && (m_componentType == null)) {
      Class componentType = m_class.getComponentType();
      if (m_classInfoRepository.hasClassInfo(componentType.getName())) {
        m_componentType = m_classInfoRepository.getClassInfo(componentType.getName());
      } else {
        m_componentType = JavaClassInfo.getClassInfo(componentType);
        m_classInfoRepository.addClassInfo(m_componentType);
      }
    }
    return m_componentType;
  }

  /**
   * Is the class an interface.
   *
   * @return
   */
  public boolean isInterface() {
    return m_isInterface;
  }

  /**
   * Is the class a primitive type.
   *
   * @return
   */
  public boolean isPrimitive() {
    return m_isPrimitive;
  }

  /**
   * Is the class an array type.
   *
   * @return
   */
  public boolean isArray() {
    return m_isArray;
  }

  /**
   * Converts an internal Java array type name ([Lblabla) to the a the format used by the expression matcher
   * (blabla[])
   *
   * @param typeName is type name
   * @return
   */
  public static String convertJavaArrayTypeNameToHumanTypeName(final String typeName) {
    int index = typeName.lastIndexOf('[');
    if (index != -1) {
      StringBuffer arrayType = new StringBuffer();
      if (typeName.endsWith("I")) {
        arrayType.append("int");
      } else if (typeName.endsWith("J")) {
        arrayType.append("long");
      } else if (typeName.endsWith("S")) {
        arrayType.append("short");
      } else if (typeName.endsWith("F")) {
        arrayType.append("float");
      } else if (typeName.endsWith("D")) {
        arrayType.append("double");
      } else if (typeName.endsWith("Z")) {
        arrayType.append("boolean");
      } else if (typeName.endsWith("C")) {
        arrayType.append("char");
      } else if (typeName.endsWith("B")) {
        arrayType.append("byte");
      } else {
        arrayType.append(typeName.substring(index + 2, typeName.length() - 1));
      }
      for (int i = 0; i < (index + 1); i++) {
        arrayType.append("[]");
      }
      return arrayType.toString();
    } else {
      return typeName;
    }
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ClassInfo)) {
      return false;
    }
    ClassInfo classInfo = (ClassInfo) o;
    return m_class.getName().toString().equals(classInfo.getName().toString());
  }

  public int hashCode() {
    return m_class.getName().toString().hashCode();
  }

  public String toString() {
    return getName();
  }

  public AnnotationReader getAnnotationReader() {
    return AnnotationReader.getReaderFor(m_class);
  }
}