/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.java;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.ConstructorInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ReflectHelper;
import com.tc.aspectwerkz.reflect.StaticInitializationInfo;
import com.tc.aspectwerkz.reflect.StaticInitializationInfoImpl;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.backport175.bytecode.AnnotationReader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Implementation of the ClassInfo interface for java.lang.reflect.*.
 * 
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bon�r </a>
 */
public class JavaClassInfo implements ClassInfo {
  /**
   * The class.
   */
  // TODO might be safer to wrap this member in a weak ref
  private final Class                   m_class;

  /**
   * The name of the class.
   */
  private String                        m_name;

  /**
   * The signature of the class.
   */
  private final String                  m_signature;

  /**
   * Is the class an interface.
   */
  private boolean                       m_isInterface       = false;

  /**
   * Is the class a primitive type.
   */
  private boolean                       m_isPrimitive       = false;

  /**
   * Is the class of type array.
   */
  private boolean                       m_isArray           = false;

  /**
   * A list with the <code>ConstructorInfo</code> instances.
   */
  private final HashMap                 m_constructors      = new HashMap();

  /**
   * A list with the <code>MethodInfo</code> instances.
   */
  private final HashMap                 m_methods           = new HashMap();

  /**
   * A list with the <code>FieldInfo</code> instances.
   */
  private final HashMap                 m_fields            = new HashMap();
  private FieldInfo[]                   m_fieldsLazy        = null;

  /**
   * A list with the interfaces.
   */
  private ClassInfo[]                   m_interfaces        = null;

  /**
   * The super class.
   */
  private ClassInfo                     m_superClass        = null;

  /**
   * The component type if array type.
   */
  private ClassInfo                     m_componentType     = null;

  /**
   * The class info repository.
   */
  private final JavaClassInfoRepository m_classInfoRepository;

  /**
   * Lazy, the static initializer info or null if not present
   */
  private StaticInitializationInfo      m_staticInitializer = null;

  /**
   * Creates a new class meta data instance.
   * 
   * @param klass
   */
  JavaClassInfo(final Class klass) {
    if (klass == null) { throw new IllegalArgumentException("class can not be null"); }
    this.m_class = klass;

    this.m_signature = ReflectHelper.getClassSignature(klass);

    this.m_classInfoRepository = JavaClassInfoRepository.getRepository(klass.getClassLoader());
    this.m_isInterface = klass.isInterface();
    if (klass.isPrimitive()) {
      this.m_name = klass.getName();
      this.m_isPrimitive = true;
    } else if (klass.getComponentType() != null) {
      this.m_name = convertJavaArrayTypeNameToHumanTypeName(klass.getName());
      this.m_isArray = true;
      this.m_interfaces = new ClassInfo[0];
    } else {
      this.m_name = klass.getName();
      final Method[] methods = this.m_class.getDeclaredMethods();
      for (int i = 0; i < methods.length; i++) {
        final Method method = methods[i];
        this.m_methods.put(Integer.valueOf(ReflectHelper.calculateHash(method)), new JavaMethodInfo(method, this));
      }
      final Constructor[] constructors = this.m_class.getDeclaredConstructors();
      for (int i = 0; i < constructors.length; i++) {
        final Constructor constructor = constructors[i];
        this.m_constructors.put(Integer.valueOf(ReflectHelper.calculateHash(constructor)), //
                                new JavaConstructorInfo(constructor, this));
      }
      final Field[] fields = this.m_class.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].getName().startsWith(TransformationConstants.ASPECTWERKZ_PREFIX)) {
          continue;
        }
        final Field field = fields[i];
        this.m_fields.put(Integer.valueOf(ReflectHelper.calculateHash(field)), new JavaFieldInfo(field, this));
      }
    }
    this.m_classInfoRepository.addClassInfo(this);
  }

  /**
   * Returns the class info for a specific class.
   * 
   * @return the class info
   */
  public static ClassInfo getClassInfo(final Class clazz) {
    final JavaClassInfoRepository repository = JavaClassInfoRepository.getRepository(clazz.getClassLoader());
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
    return this.m_name.replace('/', '.');
  }

  /**
   * Checks if the class has a static initalizer.
   * 
   * @return
   */
  public boolean hasStaticInitializer() {
    final ClassInfo classInfo = AsmClassInfo.getClassInfo(getName(), getClassLoader());
    return classInfo.hasStaticInitializer();
  }

  /**
   * Returns the static initializer info of the current underlying class if any.
   * 
   * @see ClassInfo#staticInitializer()
   */
  public StaticInitializationInfo staticInitializer() {
    if (hasStaticInitializer() && this.m_staticInitializer == null) {
      this.m_staticInitializer = new StaticInitializationInfoImpl(this);
    }
    return this.m_staticInitializer;
  }

  /**
   * Returns the signature for the element.
   * 
   * @return the signature for the element
   */
  public String getSignature() {
    return this.m_signature;
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
    return this.m_class.getModifiers();
  }

  /**
   * Returns the class loader that loaded this class.
   * 
   * @return the class loader
   */
  public ClassLoader getClassLoader() {
    return this.m_class.getClassLoader();
  }

  /**
   * Returns a constructor info by its hash.
   * 
   * @param hash
   * @return
   */
  public ConstructorInfo getConstructor(final int hash) {
    ConstructorInfo constructor = (ConstructorInfo) this.m_constructors.get(Integer.valueOf(hash));
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
    final ConstructorInfo[] methodInfos = new ConstructorInfo[this.m_constructors.size()];
    // Object[] values = m_constructors.getValues();
    // for (int i = 0; i < values.length; i++) {
    // methodInfos[i] = (ConstructorInfo) values[i];
    int i = 0;
    for (final Iterator it = this.m_constructors.values().iterator(); it.hasNext();) {
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
    MethodInfo method = (MethodInfo) this.m_methods.get(Integer.valueOf(hash));
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
    final MethodInfo[] methodInfos = new MethodInfo[this.m_methods.size()];
    // Object[] values = m_methods.getValues();
    // for (int i = 0; i < values.length; i++) {
    // methodInfos[i] = (MethodInfo) values[i];
    int i = 0;
    for (final Iterator it = this.m_methods.values().iterator(); it.hasNext();) {
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
    FieldInfo field = (FieldInfo) this.m_fields.get(Integer.valueOf(hash));
    if (field == null && getSuperclass() != null) {
      field = getSuperclass().getField(hash);
    }
    if (field == null) {
      // Trying to find constants in Interfaces
      final ClassInfo[] interfaces = getInterfaces();
      for (int i = 0; i < interfaces.length; i++) {
        final ClassInfo ifc = interfaces[i];
        field = ifc.getField(hash);
        if (field != null) {
          break;
        }
      }
    }
    return field;
  }

  /**
   * Returns a list with all the field info.
   * 
   * @return the field info
   */
  public synchronized FieldInfo[] getFields() {
    if (this.m_fieldsLazy == null) {
      final FieldInfo[] fieldInfos = new FieldInfo[this.m_fields.size()];

      int i = 0;
      for (final Iterator it = this.m_fields.values().iterator(); it.hasNext();) {
        fieldInfos[i++] = (FieldInfo) it.next();
      }

      this.m_fieldsLazy = fieldInfos;
    }
    return this.m_fieldsLazy;
  }

  /**
   * Returns the interfaces.
   * 
   * @return the interfaces
   */
  public synchronized ClassInfo[] getInterfaces() {
    if (this.m_interfaces == null) {
      final Class[] interfaces = this.m_class.getInterfaces();
      this.m_interfaces = new ClassInfo[interfaces.length];
      for (int i = 0; i < interfaces.length; i++) {
        final Class anInterface = interfaces[i];
        final ClassInfo classInfo = JavaClassInfo.getClassInfo(anInterface);
        this.m_interfaces[i] = classInfo;
        if (!this.m_classInfoRepository.hasClassInfo(anInterface.getName())) {
          this.m_classInfoRepository.addClassInfo(classInfo);
        }
      }
    }
    return this.m_interfaces;
  }

  /**
   * Returns the super class.
   * 
   * @return the super class
   */
  public ClassInfo getSuperclass() {
    if (this.m_superClass == null) {
      final Class superclass = this.m_class.getSuperclass();
      if (superclass != null) {
        if (this.m_classInfoRepository.hasClassInfo(superclass.getName())) {
          this.m_superClass = this.m_classInfoRepository.getClassInfo(superclass.getName());
        } else {
          this.m_superClass = JavaClassInfo.getClassInfo(superclass);
          this.m_classInfoRepository.addClassInfo(this.m_superClass);
        }
      }
    }
    return this.m_superClass;
  }

  /**
   * Returns the component type if array type else null.
   * 
   * @return the component type
   */
  public ClassInfo getComponentType() {
    if (isArray() && (this.m_componentType == null)) {
      final Class componentType = this.m_class.getComponentType();
      if (this.m_classInfoRepository.hasClassInfo(componentType.getName())) {
        this.m_componentType = this.m_classInfoRepository.getClassInfo(componentType.getName());
      } else {
        this.m_componentType = JavaClassInfo.getClassInfo(componentType);
        this.m_classInfoRepository.addClassInfo(this.m_componentType);
      }
    }
    return this.m_componentType;
  }

  /**
   * Is the class an interface.
   * 
   * @return
   */
  public boolean isInterface() {
    return this.m_isInterface;
  }

  /**
   * Is the class a primitive type.
   * 
   * @return
   */
  public boolean isPrimitive() {
    return this.m_isPrimitive;
  }

  /**
   * Is the class an array type.
   * 
   * @return
   */
  public boolean isArray() {
    return this.m_isArray;
  }

  /**
   * Converts an internal Java array type name ([Lblabla) to the a the format used by the expression matcher (blabla[])
   * 
   * @param typeName is type name
   * @return
   */
  public static String convertJavaArrayTypeNameToHumanTypeName(final String typeName) {
    final int index = typeName.lastIndexOf('[');
    if (index != -1) {
      final StringBuffer arrayType = new StringBuffer();
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

  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (!(o instanceof ClassInfo)) { return false; }
    final ClassInfo classInfo = (ClassInfo) o;
    return this.m_class.getName().toString().equals(classInfo.getName().toString());
  }

  public int hashCode() {
    return this.m_class.getName().toString().hashCode();
  }

  public String toString() {
    return getName();
  }

  public AnnotationReader getAnnotationReader() {
    return AnnotationReader.getReaderFor(this.m_class);
  }
}
