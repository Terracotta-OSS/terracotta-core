/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.exception.TCRuntimeException;
import com.tc.object.applicator.AccessibleObjectApplicator;
import com.tc.object.applicator.ArrayApplicator;
import com.tc.object.applicator.ChangeApplicator;
import com.tc.object.applicator.FileApplicator;
import com.tc.object.applicator.LiteralTypesApplicator;
import com.tc.object.applicator.PhysicalApplicator;
import com.tc.object.applicator.ProxyApplicator;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.dna.api.IDNAEncoding;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * @author steve
 */
public class TCClassFactoryImpl implements TCClassFactory {
  private static final LiteralValues  literalValues             = new LiteralValues();
  private static Class[]              APPLICATOR_CSTR_SIGNATURE = new Class[] { IDNAEncoding.class };

  private final Map                   classes                   = new HashMap();
  private final TCFieldFactory        fieldFactory;
  private final DSOClientConfigHelper config;
  private final ClassProvider         classProvider;
  private final IDNAEncoding           encoding;

  public TCClassFactoryImpl(TCFieldFactory fieldFactory, DSOClientConfigHelper config, ClassProvider classProvider) {
    this.fieldFactory = fieldFactory;
    this.config = config;
    this.classProvider = classProvider;
    this.encoding = new DNAEncoding(classProvider);
  }

  public TCClass getOrCreate(Class clazz, ClientObjectManager objectManager) {
    synchronized (classes) {
      TCClass rv = (TCClass) classes.get(clazz);
      if (rv == null) {
        String loaderDesc = classProvider.getLoaderDescriptionFor(clazz);
        String className = clazz.getName();
        ClassInfo classInfo = JavaClassInfo.getClassInfo(clazz);
        rv = new TCClassImpl(fieldFactory, this,
                             objectManager,
                             config.getTCPeerClass(clazz),
                             getLogicalSuperClassWithDefaultConstructor(clazz), //
                             loaderDesc, config.getLogicalExtendingClassName(className), config.isLogical(className),
                             config.isCallConstructorOnLoad(classInfo), config.getOnLoadScriptIfDefined(classInfo),
                             config.getOnLoadMethodIfDefined(classInfo), config.isUseNonDefaultConstructor(clazz));
        classes.put(clazz, rv);
      }
      return rv;
    }
  }

  public Class getLogicalSuperClassWithDefaultConstructor(Class clazz) {
    TransparencyClassSpec spec = config.getSpec(clazz.getName());
    if (spec == null) { return null; }

    while (clazz != null) {
      if (spec == null) { return null; }
      if (spec.isLogical()) {
        Constructor c = null;
        try {
          c = clazz.getDeclaredConstructor(new Class[0]);
        } catch (SecurityException e) {
          throw new TCRuntimeException(e);
        } catch (NoSuchMethodException e) {
          c = null;
        }
        if (c != null) { return clazz; }
      }
      clazz = clazz.getSuperclass();
      if (clazz != null) {
        spec = config.getSpec(clazz.getName());
      } else {
        spec = null;
      }
    }
    return null;
  }

  public ChangeApplicator createApplicatorFor(TCClass clazz, boolean indexed) {
    if (indexed) { return new ArrayApplicator(encoding); }
    String name = clazz.getName();
    Class applicatorClazz = config.getChangeApplicator(clazz.getPeerClass());

    if (applicatorClazz == null) {
      if (literalValues.isLiteral(name)) {
        return new LiteralTypesApplicator(clazz, encoding);
      } else if (clazz.isProxyClass()) {
        return new ProxyApplicator(encoding);
      } else if (ClassUtils.isPortableReflectionClass(clazz.getPeerClass())) {
        return new AccessibleObjectApplicator(encoding);
      } else if ("java.io.File".equals(name)) {
        return new FileApplicator(clazz, encoding);
      } else if ("java.util.concurrent.atomic.AtomicInteger".equals(name)) {
        try {
          Class klass = Class.forName("com.tc.object.applicator.AtomicIntegerApplicator");
          Constructor constructor = klass.getDeclaredConstructor(APPLICATOR_CSTR_SIGNATURE);
          return (ChangeApplicator)constructor.newInstance(new Object[] {encoding});
        } catch (Exception e) {
          throw new AssertionError(e);
        }
      } else if ("java.util.concurrent.atomic.AtomicLong".equals(name)) {
        try {
          Class klass = Class.forName("com.tc.object.applicator.AtomicLongApplicator");
          Constructor constructor = klass.getDeclaredConstructor(APPLICATOR_CSTR_SIGNATURE);
          return (ChangeApplicator)constructor.newInstance(new Object[] {encoding});
        } catch (Exception e) {
          throw new AssertionError(e);
        }
      } else {
        return new PhysicalApplicator(clazz, encoding);
      }
    }

    try {
      Constructor cstr = applicatorClazz.getConstructor(APPLICATOR_CSTR_SIGNATURE);
      Object[] params = new Object[] { encoding };
      return (ChangeApplicator) cstr.newInstance(params);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
