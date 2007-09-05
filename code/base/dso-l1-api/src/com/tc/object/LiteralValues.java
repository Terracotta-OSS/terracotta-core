/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.util.Assert;
import com.tc.util.ClassUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for handling literals
 */
public class LiteralValues {

  public final static String ENUM_CLASS_DOTS              = "java.lang.Enum";

  /*********************************************************************************************************************
   * NOTE:: READ THIS IF YOU ARE ADDING NEW TYPES TO THIS FILE. XXX:: If you are adding more types, please see
   * PhysicalStateClassLoader and DNAEncoding. You need to be adding New code in both those classes or else some things
   * will be broken.
   ********************************************************************************************************************/
  public final static int    INTEGER                      = 0;
  public final static int    LONG                         = 1;
  public final static int    CHARACTER                    = 2;
  public final static int    FLOAT                        = 3;
  public final static int    DOUBLE                       = 4;
  public final static int    BYTE                         = 5;
  public final static int    STRING                       = 6;
  public final static int    BOOLEAN                      = 7;
  public final static int    SHORT                        = 8;
  public final static int    ARRAY                        = 9;
  public final static int    OBJECT                       = 10;
  public final static int    OBJECT_ID                    = 11;
  public final static int    STRING_BYTES                 = 12;
  public final static int    JAVA_LANG_CLASS              = 13;
  public final static int    JAVA_LANG_CLASS_HOLDER       = 14;
  public final static int    STACK_TRACE_ELEMENT          = 15;
  public final static int    BIG_INTEGER                  = 16;
  public final static int    BIG_DECIMAL                  = 17;
  public final static int    JAVA_LANG_CLASSLOADER        = 18;
  public final static int    JAVA_LANG_CLASSLOADER_HOLDER = 19;
  public final static int    ENUM                         = 20;
  public final static int    ENUM_HOLDER                  = 21;
  public final static int    CURRENCY                     = 22;

  private final Map          values;

  public LiteralValues() {
    super();

    Map tmp = new HashMap();

    addMapping(tmp, Integer.class.getName(), INTEGER);
    addMapping(tmp, int.class.getName(), INTEGER);
    addMapping(tmp, Long.class.getName(), LONG);
    addMapping(tmp, long.class.getName(), LONG);
    addMapping(tmp, Character.class.getName(), CHARACTER);
    addMapping(tmp, char.class.getName(), CHARACTER);
    addMapping(tmp, Float.class.getName(), FLOAT);
    addMapping(tmp, float.class.getName(), FLOAT);
    addMapping(tmp, Double.class.getName(), DOUBLE);
    addMapping(tmp, double.class.getName(), DOUBLE);
    addMapping(tmp, Byte.class.getName(), BYTE);
    addMapping(tmp, byte.class.getName(), BYTE);
    addMapping(tmp, String.class.getName(), STRING);

    addMapping(tmp, "com.tc.object.dna.impl.UTF8ByteDataHolder", STRING_BYTES);

    addMapping(tmp, Short.class.getName(), SHORT);
    addMapping(tmp, short.class.getName(), SHORT);
    addMapping(tmp, Boolean.class.getName(), BOOLEAN);
    addMapping(tmp, boolean.class.getName(), BOOLEAN);

    addMapping(tmp, BigInteger.class.getName(), BIG_INTEGER);
    addMapping(tmp, BigDecimal.class.getName(), BIG_DECIMAL);

    addMapping(tmp, java.lang.Class.class.getName(), JAVA_LANG_CLASS);

    addMapping(tmp, "com.tc.object.dna.impl.ClassInstance", JAVA_LANG_CLASS_HOLDER);

    addMapping(tmp, ObjectID.class.getName(), OBJECT_ID);
    addMapping(tmp, StackTraceElement.class.getName(), STACK_TRACE_ELEMENT);

    addMapping(tmp, "com.tc.object.dna.impl.ClassLoaderInstance", JAVA_LANG_CLASSLOADER_HOLDER);

    addMapping(tmp, ENUM_CLASS_DOTS, ENUM);

    addMapping(tmp, "com.tc.object.dna.impl.EnumInstance", ENUM_HOLDER);

    addMapping(tmp, Currency.class.getName(), CURRENCY);

    values = Collections.unmodifiableMap(tmp);
  }

  /**
   * Determine LiteralValue code for an instance object
   * @param pojo Object instance, should never be null
   * @return Literal value code for the pojo's class
   */
  public int valueFor(Object pojo) {
    if (pojo instanceof ClassLoader) { return JAVA_LANG_CLASSLOADER; }

    Class clazz = pojo.getClass();
    int i = valueForClassName(clazz.getName());
    if (i == OBJECT && ClassUtils.isEnum(pojo.getClass())) { return ENUM; }
    return i;
  }

  /**
   * Determine whether a class is a literal
   * @param className Class name
   * @return True if literal value class
   */
  public boolean isLiteral(String className) {
    int i = valueForClassName(className);
    return i != OBJECT && i != ARRAY;
  }

  /**
   * Determine whether the instance is a literal
   * @param obj Instance object, may be null
   * @return True if literal value instance, false if null or not literal value instance
   */
  public boolean isLiteralInstance(Object obj) {
    if (obj == null) { return false; }
    int i = valueFor(obj);
    return i != OBJECT && i != ARRAY;
  }

  private static void addMapping(Map map, String className, int i) {
    Object prev = map.put(className, new Integer(i));
    Assert.assertNull(className, prev);
  }

  /**
   * Get literal value code for class name
   * @param className Class name, may be null
   * @return Literal value marker or {@link #OBJECT} if className is null
   */
  public int valueForClassName(String className) {
    if ((className != null) && className.startsWith("[")) { return ARRAY; }
    Integer i = (Integer) values.get(className);
    if (i == null) return OBJECT;
    return i.intValue();
  }
}
