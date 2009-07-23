/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.TCRuntimeException;
import com.tc.object.TCObject;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/*
 * Test cases for testing method invocations for BigInteger and BigDecimal classes. Testcases for testing the sharing of
 * BigInteger and BigDecimal objects are handled by TransparentTestApp.
 */
public class BigIntegerDecimalTestApp extends AbstractTransparentApp {
  private final static BigInteger refInt     = new BigInteger("100");
  private final static BigDecimal refDecimal = new BigDecimal("100.0");

  public BigIntegerDecimalTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    runBigIntegerTests();
    runBigDecimalTests();
  }

  private void runBigIntegerTests() {
    System.out.println("==========BigInteger tests Begin.==========");
    invokeAllBigIntegerConstructors();
    invokeAllBigIntegerMethods();
    System.out.println("==========BigInteger tests End.==========");
  }

  private void runBigDecimalTests() {
    System.out.println("==========BigDecimal tests Begin.==========");
    invokeAllBigDecimalConstructors();
    invokeAllBigDecimalMethods();
    System.out.println("==========BigDecimal tests End.==========");
  }

  private void invokeAllBigDecimalMethods() {
    invokeAllMethods(BigDecimal.class, false);
    invokeAllMethods(BigDecimal.class, true);
  }

  private void invokeAllBigIntegerMethods() {
    invokeAllMethods(BigInteger.class, false);
    invokeAllMethods(BigInteger.class, true);
  }

  private void invokeAllMethods(Class bClazz, boolean lock) {
    Object[] methodArguments = null;
    Object object = null;

    boolean bigIntegerClass = false;
    if (bClazz.equals(BigInteger.class)) {
      bigIntegerClass = true;
    }

    Method[] methods = bClazz.getDeclaredMethods();
    for (int i = 0; i < methods.length; i++) {
      try {
        if (Modifier.isPublic(methods[i].getModifiers())
            && !methods[i].getName().startsWith(ByteCodeUtil.TC_METHOD_PREFIX)
            && !methods[i].getName().endsWith("class$")) {
          System.out.println("Executing method: " + methods[i].getName());

          methodArguments = bigIntegerClass ? getBigIntegerMethodArguments(methods[i])
              : getBigDecimalMethodArguments(methods[i]);

          if (bigIntegerClass) {
            object = new BigInteger("101");
          } else {
            object = new BigDecimal("101.0");
          }

          invokeMethod(object, methods[i], methodArguments, lock);
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to invoke method " + methods[i].getName() + " with arguments: "
                                   + Arrays.asList(methodArguments), e);
      }
    }
  }

  private void invokeMethod(Object object, Method method, Object[] methodArguments, boolean lock)
      throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    if (lock) {
      synchronized (object) {
        method.invoke(object, methodArguments);
      }
    } else {
      method.invoke(object, methodArguments);
    }
  }

  // Brute force for getting the arguments for an BigDecimal method.
  private Object[] getBigDecimalMethodArguments(Method method) {
    Class[] parameterTypes = method.getParameterTypes();
    Object[] arguments = new Object[parameterTypes.length];
    String methodName = method.getName();

    Class mathContextClazz = getMathContextClass();
    Object decimal32MathContext = getDecimal32MathContext(mathContextClazz);

    Class roundingModeClazz = getRoundingModeClass();
    Object upRoundingMode = getUpRoundingMode(roundingModeClazz);

    if (("abs".equals(methodName) || "negate".equals(methodName) || "plus".equals(methodName)) && arguments.length == 1) {
      if (parameterTypes[0].equals(mathContextClazz)) {
        arguments[0] = decimal32MathContext;
      }
    } else if ("add".equals(methodName)) {
      arguments[0] = refDecimal;
      if (arguments.length == 2 && parameterTypes[1].equals(mathContextClazz)) {
        arguments[1] = decimal32MathContext;
      }
    } else if ("compareTo".equals(methodName) || "equals".equals(methodName) || "max".equals(methodName)
               || "min".equals(methodName)) {
      arguments[0] = refDecimal;
    } else if ("divide".equals(methodName) || "multiply".equals(methodName) || "subtract".equals(methodName)) {
      arguments[0] = refDecimal;
      if (arguments.length == 2) {
        if (parameterTypes[1].equals(mathContextClazz)) {
          arguments[1] = decimal32MathContext;
        } else if (parameterTypes[1].equals(roundingModeClazz)) {
          arguments[1] = upRoundingMode;
        } else {
          arguments[1] = new Integer(BigDecimal.ROUND_UP);
        }
      } else if (arguments.length == 3) {
        arguments[1] = new Integer(1);
        if (parameterTypes[2].equals(roundingModeClazz)) {
          arguments[2] = upRoundingMode;
        } else {
          arguments[2] = new Integer(BigDecimal.ROUND_UP);
        }
      }
    } else if ("divideAndRemainder".equals(methodName) || "divideToIntegralValue".equals(methodName)
               || "remainder".equals(methodName)) {
      arguments[0] = refDecimal;
      if (arguments.length == 2 && parameterTypes[1].equals(mathContextClazz)) {
        arguments[1] = decimal32MathContext;
      }
    } else if ("movePointLeft".equals(methodName) || "movePointRight".equals(methodName)
               || "scaleByPowerOfTen".equals(methodName)) {
      arguments[0] = new Integer(1);
    } else if ("pow".equals(methodName)) {
      arguments[0] = new Integer(1);
      if (arguments.length == 2 && parameterTypes[1].equals(mathContextClazz)) {
        arguments[1] = decimal32MathContext;
      }
    } else if ("round".equals(methodName)) {
      arguments[0] = decimal32MathContext;
    } else if ("setScale".equals(methodName)) {
      arguments[0] = new Integer(1);
      if (arguments.length == 2) {
        if (parameterTypes[1].equals(roundingModeClazz)) {
          arguments[1] = upRoundingMode;
        } else {
          arguments[1] = new Integer(BigDecimal.ROUND_UP);
        }
      }
    } else if ("valueOf".equals(methodName)) {
      if (parameterTypes[0].equals(Double.TYPE)) {
        arguments[0] = new Double(100.0);
      } else if (parameterTypes[0].equals(Long.TYPE)) {
        arguments[0] = new Long(100);
        if (arguments.length == 2) {
          arguments[1] = new Integer(1);
        }
      }
    }

    return arguments;
  }

  // Brute force for getting the arguments for an BigInteger method.
  private Object[] getBigIntegerMethodArguments(Method method) {
    Object[] arguments = new Object[method.getParameterTypes().length];
    String methodName = method.getName();
    if ("add".equals(methodName) || "and".equals(methodName) || "andNot".equals(methodName)) {
      arguments[0] = new BigInteger("200");
    } else if ("clearBit".equals(methodName)) {
      arguments[0] = new Integer(1);
    } else if ("compareTo".equals(methodName) || "divide".equals(methodName) || "divideAndRemainder".equals(methodName)
               || "equals".equals(methodName) || "gcd".equals(methodName) || "max".equals(methodName)
               || "min".equals(methodName) || "mod".equals(methodName) || "modInverse".equals(methodName)
               || "multiply".equals(methodName) || "or".equals(methodName) || "remainder".equals(methodName)
               || "subtract".equals(methodName) || "xor".equals(methodName)) {
      arguments[0] = refInt;
    } else if ("flipBit".equals(methodName) || "isProbablePrime".equals(methodName) || "pow".equals(methodName)
               || "setBit".equals(methodName) || "shiftLeft".equals(methodName) || "shiftRight".equals(methodName)
               || "testBit".equals(methodName) || "trailingZeroCnt".equals(methodName) || "bitLen".equals(methodName)
               || "primeToCertainty".equals(methodName) || "bitCnt".equals(methodName)) {
      arguments[0] = new Integer(1);
    } else if ("modPow".equals(methodName)) {
      arguments[0] = refInt;
      arguments[1] = refInt;
    } else if ("probablePrime".equals(methodName)) {
      arguments[0] = new Integer(5);
      arguments[1] = new Random();
    } else if ("toString".equals(methodName) && arguments.length == 1) {
      arguments[0] = new Integer(1);
    } else if ("valueOf".equals(methodName)) {
      arguments[0] = new Long(1);
    } else if ("mulAdd".equals(methodName)) {
      arguments[0] = new int[] { 1 };
      arguments[1] = new int[] { 1 };
      arguments[2] = new Integer(0);
      arguments[3] = new Integer(1);
      arguments[4] = new Integer(1);
    } else if ("addOne".equals(methodName)) {
      arguments[0] = new int[] { 1 };
      arguments[1] = new Integer(0);
      arguments[2] = new Integer(0);
      arguments[3] = new Integer(1);
    } else if ("primitiveLeftShift".equals(methodName) || "primitiveRightShift".equals(methodName)) {
      arguments[0] = new int[] { 1 };
      arguments[1] = new Integer(1);
      arguments[2] = new Integer(1);
    } else if ("javaIncrement".equals(methodName)) {
      arguments[0] = new int[] { 1 };
    } else if ("jacobiSymbol".equals(methodName)) {
      arguments[0] = new Integer(1);
      arguments[1] = refInt;
    }

    return arguments;
  }

  private void invokeAllBigDecimalConstructors() {
    invokeAllConstructors(BigDecimal.class);
  }

  private void invokeAllBigIntegerConstructors() {
    invokeAllConstructors(BigInteger.class);
  }

  private void invokeAllConstructors(Class bClazz) {
    boolean bigIntegerClass = false;
    if (bClazz.equals(BigInteger.class)) {
      bigIntegerClass = true;
    }
    Object[] arguments = null;
    Constructor[] constructors = bClazz.getConstructors();
    for (int i = 0; i < constructors.length; i++) {
      try {
        arguments = (bigIntegerClass) ? getBigIntegerConstructorArguments(constructors[i].getParameterTypes())
            : getBigDecimalConstructorArguments(constructors[i].getParameterTypes());
        invokeConstructor(constructors[i], arguments);
      } catch (IllegalArgumentException e) {
        throw new TCRuntimeException(e);
      } catch (InstantiationException e) {
        throw new TCRuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new TCRuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  private void invokeConstructor(Constructor constructor, Object[] arguments) throws IllegalArgumentException,
      InstantiationException, IllegalAccessException, InvocationTargetException {
    Class[] parameterTypes = constructor.getParameterTypes();
    if (parameterTypes[0] != TCObject.class) {
      constructor.newInstance(arguments);
    }
  }

  private Object[] getBigDecimalConstructorArguments(Class[] parameterTypes) {
    Class mathContextClazz = getMathContextClass();
    Object decimal32MathContext = getDecimal32MathContext(mathContextClazz);

    Object[] arguments = new Object[parameterTypes.length];
    if (parameterTypes[0].equals(BigInteger.class)) {
      arguments[0] = new BigInteger("100");
      if (parameterTypes.length == 2) {
        if (parameterTypes[1].equals(mathContextClazz)) {
          arguments[1] = decimal32MathContext;
        } else {
          arguments[1] = new Integer(1);
        }
      } else if (parameterTypes.length == 3) {
        arguments[1] = new Integer(1);
        arguments[2] = decimal32MathContext;
      }
    } else if (parameterTypes[0].equals(char[].class)) {
      arguments[0] = new char[] { '1' };
      if (parameterTypes.length == 2) {
        arguments[1] = decimal32MathContext;
      } else if (parameterTypes.length == 3) {
        arguments[1] = new Integer(0);
        arguments[2] = new Integer(1);
      } else if (parameterTypes.length == 4) {
        arguments[1] = new Integer(0);
        arguments[2] = new Integer(1);
        arguments[3] = decimal32MathContext;
      }
    } else if (parameterTypes[0].equals(Double.TYPE)) {
      arguments[0] = new Double(100.0);
      if (parameterTypes.length == 2) {
        arguments[1] = decimal32MathContext;
      }
    } else if (parameterTypes[0].equals(Integer.TYPE)) {
      arguments[0] = new Integer(100);
      if (parameterTypes.length == 2) {
        arguments[1] = decimal32MathContext;
      }
    } else if (parameterTypes[0].equals(Long.TYPE)) {
      arguments[0] = new Long(100);
      if (parameterTypes.length == 2) {
        arguments[1] = decimal32MathContext;
      }
    } else if (parameterTypes[0].equals(String.class)) {
      arguments[0] = "100.0";
      if (parameterTypes.length == 2) {
        arguments[1] = decimal32MathContext;
      }
    }

    return arguments;
  }

  private Object[] getBigIntegerConstructorArguments(Class[] parameterTypes) {
    Object[] arguments = new Object[parameterTypes.length];
    switch (parameterTypes.length) {
      case 1:
        if (parameterTypes[0] == String.class) {
          arguments[0] = "100";
        } else {
          arguments[0] = new byte[] { (byte) 100 };
        }
        break;
      case 2:
        if (parameterTypes[0] == String.class) {
          arguments[0] = "100";
          arguments[1] = new Integer(10);
        } else if (parameterTypes[1] == Random.class) {
          arguments[0] = new Integer(10);
          arguments[1] = new Random();
        } else {
          arguments[0] = new Integer(refInt.signum());
          arguments[1] = new byte[] { (byte) 100 };
        }
        break;
      case 3:
        arguments[0] = new Integer(refInt.bitLength());
        arguments[1] = new Integer(5);
        arguments[2] = new Random();
        break;
    }

    return arguments;
  }

  private Class getRoundingModeClass() {
    return getClazz("java.math.RoundingMode");
  }

  private Class getMathContextClass() {
    return getClazz("java.math.MathContext");
  }

  /*
   * We need to use reflection to obtain class for RoundingMode and MathContext because these two classes do not exist
   * in jdk1.4.
   */
  private Class getClazz(String className) {
    Class mathContextClazz = null;
    try {
      mathContextClazz = Class.forName(className);
    } catch (ClassNotFoundException e) {
      // ignore ClassNotFoundException
    } catch (SecurityException e) {
      // ignore SecurityException
    }
    return mathContextClazz;
  }

  private Object getUpRoundingMode(Class roundingModeClazz) {
    return getField(roundingModeClazz, "UP");
  }

  private Object getDecimal32MathContext(Class mathContextClazz) {
    return getField(mathContextClazz, "DECIMAL32");
  }

  /*
   * We need to use reflection to obtain the UP and DECIMAL32 fields of class RoundingMode and MathContext because these
   * two classes do not exist in jdk1.4.
   */
  private Object getField(Class mathContextClazz, String fieldName) {
    if (mathContextClazz == null) return null;
    Object decimal32MathContext = null;
    try {
      Field decimal32Field = mathContextClazz.getDeclaredField(fieldName);
      decimal32MathContext = decimal32Field.get(null);
    } catch (SecurityException e) {
      // ignore SecurityException
    } catch (NoSuchFieldException e) {
      // ignore NoSuchFieldException
    } catch (IllegalArgumentException e) {
      // ignore IllegalArgumentException
    } catch (IllegalAccessException e) {
      // ignore IllegalAccessException
    }
    return decimal32MathContext;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = BigIntegerDecimalTestApp.class.getName();
    config.getOrCreateSpec(testClass);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);
    String writeAllowedMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowedMethodExpression);
  }
}
