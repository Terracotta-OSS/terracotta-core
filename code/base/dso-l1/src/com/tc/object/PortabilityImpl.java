/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;
import com.tc.util.NonPortableReason;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PortabilityImpl implements Portability {

  private static final Class          OBJECT_CLASS         = Object.class;
  private static final List           nonInstrumentedClass = new ArrayList();

  static {
    nonInstrumentedClass.add("java.lang.Object");
    nonInstrumentedClass.add("java.lang.Number");
    nonInstrumentedClass.add("java.util.AbstractList");
    nonInstrumentedClass.add("java.util.AbstractCollection");
    nonInstrumentedClass.add("java.util.AbstractQueue");
    nonInstrumentedClass.add("java.lang.Enum");
    nonInstrumentedClass.add("java.lang.reflect.AccessibleObject");
    nonInstrumentedClass.add("java.util.concurrent.atomic.AtomicInteger");
    nonInstrumentedClass.add("java.util.concurrent.atomic.AtomicLong");
    nonInstrumentedClass.add("java.util.concurrent.locks.AbstractQueuedSynchronizer");
    nonInstrumentedClass.add("java.util.concurrent.locks.AbstractQueuedSynchronizer$Node");
  }

  private final LiteralValues         literalValues        = new LiteralValues();
  private final Map                   portableCache        = new ConcurrentHashMap();
  private final Map                   physicalCache        = new ConcurrentHashMap();
  private final DSOClientConfigHelper config;

  public PortabilityImpl(DSOClientConfigHelper config) {
    this.config = config;
  }

  private List getHierarchy(Class start) {
    List classes = new ArrayList();
    while (start != null && start != OBJECT_CLASS) {
      classes.add(start);
      start = start.getSuperclass();
    }
    return classes;
  }

  public NonPortableReason getNonPortableReason(final Class topLevelClass) {
    final List classes = getHierarchy(topLevelClass);

    // check if any class in the class hierarchy is not-adaptable (like java.lang.Thread etc.)
    for (Iterator i = classes.iterator(); i.hasNext();) {
      Class class2Inspect = (Class) i.next();
      if (config.isNeverAdaptable(JavaClassInfo.getClassInfo(class2Inspect))) {
        if (class2Inspect == topLevelClass) {
          return new NonPortableReason(topLevelClass, NonPortableReason.CLASS_NOT_ADAPTABLE);
        } else {
          NonPortableReason reason = new NonPortableReason(topLevelClass, NonPortableReason.SUPER_CLASS_NOT_ADAPTABLE);
          reason.addErroneousSuperClass(class2Inspect);
          return reason;
        }
      }
    }

    // check for the set of types that weren't instrumented
    byte reasonCode = NonPortableReason.UNDEFINED;
    List uninstrumentedSupers = new ArrayList();
    for (Iterator i = classes.iterator(); i.hasNext();) {
      Class class2Inspect = (Class) i.next();
      if (class2Inspect == topLevelClass) {
        if (!isPortableClass(class2Inspect)) {
          Assert.assertTrue(reasonCode == NonPortableReason.UNDEFINED);
          if (class2Inspect.getClassLoader() == null) {
            reasonCode = NonPortableReason.CLASS_NOT_IN_BOOT_JAR;
          } else {
            reasonCode = NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG;
          }
        }
      } else {
        if (!isPortableClass(class2Inspect)) {
          if (reasonCode == NonPortableReason.UNDEFINED) {
            reasonCode = NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED;
          }
          uninstrumentedSupers.add(class2Inspect);
        }
      }
    }

    if(uninstrumentedSupers.size() > 0 || reasonCode == NonPortableReason.CLASS_NOT_IN_BOOT_JAR) {
      NonPortableReason reason = new NonPortableReason(topLevelClass, reasonCode);
      for (Iterator i = uninstrumentedSupers.iterator(); i.hasNext();) {
        reason.addErroneousSuperClass((Class) i.next());
      }
      return reason;
    }
    
    // Now check if it is a subclass of logically managed class
    for (Iterator i = classes.iterator(); i.hasNext();) {
      Class class2Inspect = (Class) i.next();

      // if a parent class simply wasn't included, don't report this a logical subclass issue until it really is
      if (!config.shouldBeAdapted(JavaClassInfo.getClassInfo(class2Inspect))) {
        break;
      }

      if (config.isLogical(class2Inspect.getName())) {
        NonPortableReason reason = new NonPortableReason(topLevelClass,
                                                         NonPortableReason.SUBCLASS_OF_LOGICALLY_MANAGED_CLASS);
        reason.addErroneousSuperClass(class2Inspect);
        return reason;
      }
    }

    return new NonPortableReason(topLevelClass, reasonCode);
  }

  /*
   * This method does not rely on the config but rather on the fact that the class has to be instrumented at this time
   * for the object to be portable. For Logical Objects it still queries the config.
   */
  public boolean isPortableClass(Class clazz) {
    String clazzName = clazz.getName();
    Boolean isPortable = (Boolean) portableCache.get(clazzName);
    if (isPortable != null) { return isPortable.booleanValue(); }

    boolean bool = literalValues.isLiteral(clazzName) || config.isLogical(clazzName) || clazz.isArray()
                   || Proxy.isProxyClass(clazz) || ClassUtils.isEnum(clazz) || isClassPhysicallyInstrumented(clazz)
                   || isInstrumentationNotNeeded(clazzName) || ClassUtils.isPortableReflectionClass(clazz)
                   || config.isPortableModuleClass(clazz);
    portableCache.put(clazzName, Boolean.valueOf(bool));
    return bool;
  }

  public boolean isInstrumentationNotNeeded(String clazzName) {
    return nonInstrumentedClass.contains(clazzName);
  }

  public boolean isClassPhysicallyInstrumented(Class clazz) {
    // this method should only return true if this class "directly" implements
    // the interface in question. It specifically does *NOT* walk the class hierarchy looking
    // for the interface. This always means you can't just say instanceof here

    String clazzName = clazz.getName();
    Boolean isPhysicalAdapted = (Boolean) physicalCache.get(clazzName);
    if (isPhysicalAdapted != null) { return isPhysicalAdapted.booleanValue(); }

    boolean rv = false;
    Class interfaces[] = clazz.getInterfaces();
    if (interfaces == null || interfaces.length == 0) return false;
    for (int i = 0; i < interfaces.length; i++) {
      if (interfaces[i] == TransparentAccess.class) {
        rv = true;
        break;
      }
    }

    physicalCache.put(clazzName, Boolean.valueOf(rv));
    return rv;
  }

  public boolean isPortableInstance(Object obj) {
    if (obj == null) return true;
    return isPortableClass(obj.getClass());
  }
}
