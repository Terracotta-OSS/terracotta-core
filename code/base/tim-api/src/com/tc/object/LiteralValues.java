/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.object.loaders.NamedClassLoader;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for handling literal values
 */
public enum LiteralValues {
  
  /*********************************************************************************************************************
   * NOTE:: READ THIS IF YOU ARE ADDING NEW TYPES TO THIS FILE. XXX:: If you are adding more types, please see DNAEncoding. 
   * You need to be adding New code in this class to properly handle encode/decode of the new type
   ********************************************************************************************************************/
  
  INTEGER() {

    @Override
    public String getTypeDesc() {
      return "I";
    }

    @Override
    public String getClassNameSlashForPrimitives() {
      return "java/lang/Integer";
    }

    @Override
    public String getMethodNameForPrimitives() {
      return "intValue";
    }

    @Override
    public String getInputMethodDescriptor() {
      return "()I";
    }

    @Override
    public String getInputMethodName() {
      return "readInt";
    }

    @Override
    public String getOutputMethodDescriptor() {
      return "(I)V";
    }

    @Override
    public String getOutputMethodName() {
      return "writeInt";
    }
    
    
  },
  LONG() {

    @Override
    public String getTypeDesc() {
      return "J";
    }

    @Override
    public String getClassNameSlashForPrimitives() {
      return "java/lang/Long";
    }

    @Override
    public String getMethodNameForPrimitives() {
      return "longValue";
    }

    @Override
    public String getInputMethodDescriptor() {
      return "()J";
    }

    @Override
    public String getInputMethodName() {
      return "readLong";
    }

    @Override
    public String getOutputMethodDescriptor() {
      return "(J)V";
    }

    @Override
    public String getOutputMethodName() {
      return "writeLong";
    }
    
    
  },
  CHARACTER() {

    @Override
    public String getTypeDesc() {
      return "C";
    }

    @Override
    public String getClassNameSlashForPrimitives() {
      return "java/lang/Character";
    }

    @Override
    public String getMethodNameForPrimitives() {
      return "charValue";
    }

    @Override
    public String getInputMethodDescriptor() {
      return "()C";
    }

    @Override
    public String getInputMethodName() {
      return "readChar";
    }

    @Override
    public String getOutputMethodDescriptor() {
      return "(I)V";
    }

    @Override
    public String getOutputMethodName() {
      return "writeChar";
    }
    
    
  },
  FLOAT() {

    @Override
    public String getTypeDesc() {
      return "F";
    }

    @Override
    public String getClassNameSlashForPrimitives() {
      return "java/lang/Float";
    }

    @Override
    public String getMethodNameForPrimitives() {
      return "floatValue";
    }

    @Override
    public String getInputMethodDescriptor() {
      return "()F";
    }

    @Override
    public String getInputMethodName() {
      return "readFloat";
    }

    @Override
    public String getOutputMethodDescriptor() {
      return "(F)V";
    }

    @Override
    public String getOutputMethodName() {
      return "writeFloat";
    }
  },
  DOUBLE() {

    @Override
    public String getTypeDesc() {
      return "D";
    }

    @Override
    public String getClassNameSlashForPrimitives() {
      return "java/lang/Double";
    }

    @Override
    public String getMethodNameForPrimitives() {
      return "doubleValue";
    }

    @Override
    public String getInputMethodDescriptor() {
      return "()D";
    }

    @Override
    public String getInputMethodName() {
      return "readDouble";
    }

    @Override
    public String getOutputMethodDescriptor() {
      return "(D)V";
    }

    @Override
    public String getOutputMethodName() {
      return "writeDouble";
    }
  },
  BYTE() {

    @Override
    public String getTypeDesc() {
      return "B";
    }

    @Override
    public String getClassNameSlashForPrimitives() {
      return "java/lang/Byte";
    }

    @Override
    public String getMethodNameForPrimitives() {
      return "byteValue";
    }

    @Override
    public String getInputMethodDescriptor() {
      return "()B";
    }

    @Override
    public String getInputMethodName() {
      return "readByte";
    }

    @Override
    public String getOutputMethodDescriptor() {
      return "(I)V";
    }

    @Override
    public String getOutputMethodName() {
      return "writeByte";
    }
  },
  BOOLEAN() {

    @Override
    public String getTypeDesc() {
      return "Z";
    }

    @Override
    public String getClassNameSlashForPrimitives() {
      return "java/lang/Boolean";
    }

    @Override
    public String getMethodNameForPrimitives() {
      return "booleanValue";
    }

    @Override
    public String getInputMethodDescriptor() {
      return "()Z";
    }

    @Override
    public String getInputMethodName() {
      return "readBoolean";
    }

    @Override
    public String getOutputMethodDescriptor() {
      return "(Z)V";
    }

    @Override
    public String getOutputMethodName() {
      return "writeBoolean";
    }
  },
  SHORT() {

    @Override
    public String getTypeDesc() {
      return "S";
    }

    @Override
    public String getClassNameSlashForPrimitives() {
      return "java/lang/Short";
    }

    @Override
    public String getMethodNameForPrimitives() {
      return "shortValue";
    }

    @Override
    public String getInputMethodDescriptor() {
      return "()S";
    }

    @Override
    public String getInputMethodName() {
      return "readShort";
    }

    @Override
    public String getOutputMethodDescriptor() {
      return "(I)V";
    }

    @Override
    public String getOutputMethodName() {
      return "writeShort";
    }
    
  }, 
  STRING(), 
  ARRAY() {

    @Override
    public int calculateDsoHashCodeForLiteral(Object value) {
      throw new UnsupportedOperationException("Cannot calculate hashCode for " + this.name());
    }
    
  }, 
  OBJECT() {

    @Override
    public int calculateDsoHashCodeForLiteral(Object value) {
      throw new UnsupportedOperationException("Cannot calculate hashCode for " + this.name());
    }
    
  }, 
  OBJECT_ID(), 
  STRING_BYTES(), 
  JAVA_LANG_CLASS() {

    @Override
    public int calculateDsoHashCodeForLiteral(Object value) {
      return ((Class) value).getCanonicalName().hashCode();
    }
     
  },
  JAVA_LANG_CLASS_HOLDER(), 
  STACK_TRACE_ELEMENT(), 
  BIG_INTEGER(), 
  BIG_DECIMAL(), 
  JAVA_LANG_CLASSLOADER() {

    @Override
    public int calculateDsoHashCodeForLiteral(Object value) {
      return ((NamedClassLoader) value).__tc_getClassLoaderName().hashCode();
    }
    
  }, 
  JAVA_LANG_CLASSLOADER_HOLDER(), 
  ENUM() {

    @Override
    public int calculateDsoHashCodeForLiteral(Object value) {
      Enum e = (Enum) value;
      int hash = 17;
      hash = (31 * hash) + e.name().hashCode();
      hash = (31 * hash) + e.getDeclaringClass().getCanonicalName().hashCode();
      return hash;
    }
    
  }, 
  ENUM_HOLDER(), 
  CURRENCY() {

    @Override
    public int calculateDsoHashCodeForLiteral(Object value) {
      return ((Currency) value).getCurrencyCode().hashCode();
    }
    
  }, 
  STRING_BYTES_COMPRESSED();
  
  public String getInputMethodName() {
    return "readObject";
  }
  
  public String getInputMethodDescriptor() {
    return "()Ljava/lang/Object;";
  }
  
  public String getOutputMethodName() {
    return "writeObject";
  }
  
  public String getOutputMethodDescriptor() {
    return "(Ljava/lang/Object;)V";
  }
  
  public String getTypeDesc() {
    return "Ljava/lang/Object;";
  }
  
  // overridden by primitive types
  public String getClassNameSlashForPrimitives() {
    return "java/lang/Object"; 
  }
  
  // overridden by primitive types
  public String getMethodNameForPrimitives() {
    throw new AssertionError("Only Primitive types allowed. Invalid type: "
                             + this.toString());
  }
  
  /**
   * Calculate a stable hash code for the object.  Many literals (like Integer) have stable
   * hash codes already, but some (like Class) do not.
   * @param value must refer to an object for which {@link #isLiteralInstance()} returns true.
   * This implies that value must be non-null.
   */
  public int calculateDsoHashCodeForLiteral(Object value) {
    // Use caution when implementing DSO hash codes. This hash must be compatible with
    // the existing equals. In general a custom DSO hash should only be used if the
    // object does not already override hashCode (and thus does not override equals).
    // Most commonly this will apply to objects like Class and Enum, where the VM strictly
    // enforces identity equality and therefore uses System.identityHashCode.
    if (!isLiteralInstance(value)) { 
      throw new UnsupportedOperationException("Cannot calculate hashCode for non-literals"); 
    }
    return value.hashCode();
  }
  
  public final static String ENUM_CLASS_DOTS              = "java.lang.Enum";
  
  private static final Map<String, LiteralValues> literalsMap;
  
  static {
    Map<String, LiteralValues> tmp = new HashMap<String, LiteralValues>();

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
    addMapping(tmp, "com.tc.object.dna.impl.UTF8ByteCompressedDataHolder", STRING_BYTES_COMPRESSED);

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

    literalsMap = Collections.unmodifiableMap(tmp);
  }

  private static void addMapping(Map<String, LiteralValues> map, String className, LiteralValues type) {
    LiteralValues prev = map.put(className, type);
    Assert.assertNull(className, prev);
  }

  /**
   * Determine LiteralValue code for an instance object
   * 
   * @param pojo Object instance, should never be null
   * @return Literal value code for the pojo's class
   */
  public static LiteralValues valueFor(Object pojo) {
    if (pojo instanceof ClassLoader) { return JAVA_LANG_CLASSLOADER; }

    Class clazz = pojo.getClass();
    LiteralValues i = valueForClassName(clazz.getName());
    if (i == OBJECT && ClassUtils.isDsoEnum(pojo.getClass())) { return ENUM; }
    return i;
  }

  /**
   * Get literal value code for class name
   * 
   * @param className Class name, may be null
   * @return Literal value marker or {@link #OBJECT} if className is null
   */
  public static LiteralValues valueForClassName(String className) {
    if ((className != null) && className.startsWith("[")) { return ARRAY; }
    LiteralValues literalValueType = literalsMap.get(className);
    if (literalValueType == null) return OBJECT;
    return literalValueType;
  }

  /**
   * Determine whether a class is a literal
   * 
   * @param className Class name
   * @return True if literal value class
   */
  public static boolean isLiteral(String className) {
    LiteralValues i = valueForClassName(className);
    return i != OBJECT && i != ARRAY;
  }

  /**
   * Determine whether the instance is a literal
   * <p />
   * Returns false if the parameter is null
   * 
   * @param obj Instance object, may be null
   * @return True if literal value instance, false if null or not literal value instance
   */
  public static boolean isLiteralInstance(Object obj) {
    if (obj == null) { return false; }
    LiteralValues i = valueFor(obj);
    return i != OBJECT && i != ARRAY;
  }

  // for tests
  public static Collection<String> getTypes() {
    return Collections.unmodifiableSet(literalsMap.keySet());
  }

  /**
   * Calculate a stable hash code for the object.  Many literals (like Integer) have stable
   * hash codes already, but some (like Class) do not.
   * @param value must refer to an object for which {@link #isLiteralInstance()} returns true.
   * This implies that value must be non-null.
   */
  public static int calculateDsoHashCode(Object value) {
    final LiteralValues type = valueFor(value);
    return type.calculateDsoHashCodeForLiteral(value);
  }

}
