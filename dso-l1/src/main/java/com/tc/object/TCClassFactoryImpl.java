/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.applicator.ArrayApplicator;
import com.tc.object.applicator.ChangeApplicator;
import com.tc.object.applicator.LiteralTypesApplicator;
import com.tc.object.applicator.PhysicalApplicator;
import com.tc.object.bytecode.Manager;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TCClassFactoryImpl implements TCClassFactory {

  private static Class[]                           APPLICATOR_CSTR_SIGNATURE = new Class[] { DNAEncoding.class,
      TCLogger.class                                                        };

  protected final ConcurrentMap<Class<?>, TCClass> classes                   = new ConcurrentHashMap<Class<?>, TCClass>();
  protected final TCFieldFactory                   fieldFactory;
  protected final DSOClientConfigHelper            config;
  protected final ClassProvider                    classProvider;
  protected final DNAEncoding                      encoding;
  private final L1ServerMapLocalCacheManager       globalLocalCacheManager;
  private final RemoteServerMapManager             remoteServerMapManager;
  private final Manager                            manager;

  public TCClassFactoryImpl(final TCFieldFactory fieldFactory, final DSOClientConfigHelper config,
                            final ClassProvider classProvider, final DNAEncoding dnaEncoding, Manager manager,
                            final L1ServerMapLocalCacheManager globalLocalCacheManager,
                            final RemoteServerMapManager remoteServerMapManager) {
    this.fieldFactory = fieldFactory;
    this.config = config;
    this.classProvider = classProvider;
    this.encoding = dnaEncoding;
    this.manager = manager;
    this.globalLocalCacheManager = globalLocalCacheManager;
    this.remoteServerMapManager = remoteServerMapManager;
  }

  public TCClass getOrCreate(final Class clazz, final ClientObjectManager objectManager) {
    TCClass rv = this.classes.get(clazz);
    if (rv != null) { return rv; }

    final String className = clazz.getName();
    final ClassInfo classInfo = JavaClassInfo.getClassInfo(clazz);
    rv = createTCClass(clazz, objectManager, className, classInfo);

    final TCClass existing = this.classes.putIfAbsent(clazz, rv);
    return existing == null ? rv : existing;
  }

  protected TCClass createTCClass(final Class clazz, final ClientObjectManager objectManager, final String className,
                                  final ClassInfo classInfo) {
    TCClass rv;
    if (className.equals(TCClassFactory.SERVER_MAP_CLASSNAME)) {
      rv = new ServerMapTCClassImpl(this.manager, this.globalLocalCacheManager, this.remoteServerMapManager,
                                    this.fieldFactory, this, objectManager, clazz,
                                    getLogicalSuperClassWithDefaultConstructor(clazz),
                                    this.config.getLogicalExtendingClassName(className),
                                    this.config.isLogical(className), this.config.isCallConstructorOnLoad(classInfo),
                                    this.config.hasOnLoadInjection(classInfo),
                                    this.config.getOnLoadScriptIfDefined(classInfo),
                                    this.config.getOnLoadMethodIfDefined(classInfo),
                                    this.config.isUseNonDefaultConstructor(clazz),
                                    this.config.useResolveLockWhenClearing(clazz),
                                    this.config.getPostCreateMethodIfDefined(className),
                                    this.config.getPreCreateMethodIfDefined(className));
    } else {
      rv = new TCClassImpl(this.fieldFactory, this, objectManager, clazz,
                           getLogicalSuperClassWithDefaultConstructor(clazz),
                           this.config.getLogicalExtendingClassName(className), this.config.isLogical(className),
                           this.config.isCallConstructorOnLoad(classInfo), this.config.hasOnLoadInjection(classInfo),
                           this.config.getOnLoadScriptIfDefined(classInfo),
                           this.config.getOnLoadMethodIfDefined(classInfo),
                           this.config.isUseNonDefaultConstructor(clazz),
                           this.config.useResolveLockWhenClearing(clazz),
                           this.config.getPostCreateMethodIfDefined(className),
                           this.config.getPreCreateMethodIfDefined(className));
    }
    return rv;
  }

  public Class getLogicalSuperClassWithDefaultConstructor(Class clazz) {
    TransparencyClassSpec spec = this.config.getSpec(clazz.getName());
    if (spec == null) { return null; }

    while (clazz != null) {
      if (spec == null) { return null; }
      if (spec.isLogical()) {
        Constructor c = null;
        try {
          c = clazz.getDeclaredConstructor(new Class[0]);
        } catch (final SecurityException e) {
          throw new TCRuntimeException(e);
        } catch (final NoSuchMethodException e) {
          // c is already null
        }
        if (c != null) { return clazz; }
      }
      clazz = clazz.getSuperclass();
      if (clazz != null) {
        spec = this.config.getSpec(clazz.getName());
      } else {
        spec = null;
      }
    }
    return null;
  }

  public ChangeApplicator createApplicatorFor(final TCClass clazz, final boolean indexed) {
    if (indexed) { return new ArrayApplicator(this.encoding); }
    final String name = clazz.getName();
    final Class applicatorClazz = this.config.getChangeApplicator(clazz.getPeerClass());

    if (applicatorClazz == null) {
      if (LiteralValues.isLiteral(name)) {
        return new LiteralTypesApplicator(clazz, this.encoding);
      } else {
        return new PhysicalApplicator(clazz, this.encoding);
      }
    }

    TCLogger logger = TCLogging.getLogger(ChangeApplicator.class.getName() + "." + applicatorClazz.getName());

    try {
      Constructor cstr = applicatorClazz.getConstructor(APPLICATOR_CSTR_SIGNATURE);
      Object[] params = new Object[] { encoding, logger };
      return (ChangeApplicator) cstr.newInstance(params);
    } catch (final Exception e) {
      throw new AssertionError(e);
    }
  }
}
