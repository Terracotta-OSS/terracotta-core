/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.applicator.BaseApplicator;
import com.tc.object.applicator.ChangeApplicator;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.platform.PlatformService;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TCClassFactoryImpl implements TCClassFactory {

  private static Class[]                           APPLICATOR_CSTR_SIGNATURE = new Class[] { DNAEncoding.class,
      TCLogger.class                                                        };

  protected final ConcurrentMap<Class<?>, TCClass> classes                   = new ConcurrentHashMap<Class<?>, TCClass>();
  protected final DSOClientConfigHelper            config;
  protected final ClassProvider                    classProvider;
  protected final DNAEncoding                      encoding;
  private final L1ServerMapLocalCacheManager       globalLocalCacheManager;
  private final RemoteServerMapManager             remoteServerMapManager;
  private volatile PlatformService                 platformService;

  public TCClassFactoryImpl(final DSOClientConfigHelper config, final ClassProvider classProvider,
                            final DNAEncoding dnaEncoding, final L1ServerMapLocalCacheManager globalLocalCacheManager,
                            final RemoteServerMapManager remoteServerMapManager) {
    this.config = config;
    this.classProvider = classProvider;
    this.encoding = dnaEncoding;
    this.globalLocalCacheManager = globalLocalCacheManager;
    this.remoteServerMapManager = remoteServerMapManager;
  }

  @Override
  public void setPlatformService(PlatformService platformService) {
    this.platformService = platformService;
  }

  @Override
  public TCClass getOrCreate(final Class clazz, final ClientObjectManager objectManager) {
    TCClass rv = this.classes.get(clazz);
    if (rv != null) { return rv; }

    final String className = clazz.getName();
    rv = createTCClass(clazz, objectManager, className);

    final TCClass existing = this.classes.putIfAbsent(clazz, rv);
    return existing == null ? rv : existing;
  }

  protected TCClass createTCClass(final Class clazz, final ClientObjectManager objectManager, final String className) {
    TCClass rv;
    if (className.equals(TCClassFactory.SERVER_MAP_CLASSNAME)) {
      rv = new ServerMapTCClassImpl(this.platformService, this.globalLocalCacheManager, this.remoteServerMapManager,
                                    this, objectManager, clazz, this.config.isUseNonDefaultConstructor(clazz));
    } else {
      rv = new TCClassImpl(this, objectManager, clazz, this.config.isUseNonDefaultConstructor(clazz));
    }
    return rv;
  }

  @Override
  public ChangeApplicator createApplicatorFor(final TCClass clazz) {
    final Class applicatorClazz = this.config.getChangeApplicator(clazz.getPeerClass());

    if (applicatorClazz == null) { return new BaseApplicator(this.encoding); }

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
