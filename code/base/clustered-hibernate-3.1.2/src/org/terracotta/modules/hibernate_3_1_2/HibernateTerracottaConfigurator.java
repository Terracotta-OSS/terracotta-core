/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.hibernate_3_1_2;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;
import org.terracotta.modules.hibernate_3_1_2.object.config.HibernateChangeApplicatorSpec;
import org.terracotta.modules.hibernate_3_1_2.object.config.HibernateModuleSpec;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.ModuleSpec;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

import java.util.Dictionary;
import java.util.Hashtable;

public final class HibernateTerracottaConfigurator extends TerracottaConfiguratorModule {
  protected final void addInstrumentation(final BundleContext context, final StandardDSOClientConfigHelper configHelper) {
    TransparencyClassSpec spec = configHelper.getOrCreateSpec("org.hibernate.collection.AbstractPersistentCollection");
    spec.addTransient("session");
    
    LockDefinition lockDefinition = new LockDefinition("abstractPersistentCollectionLock", ConfigLockLevel.WRITE);
    lockDefinition.commit();
    configHelper.addLock("* org.hibernate.collection.AbstractPersistentCollection.*(..)", lockDefinition);

    configHelper.addIncludePattern("org.hibernate.collection.PersistentSet", false, false, false);
    lockDefinition = new LockDefinition("persistentSetLock", ConfigLockLevel.WRITE);
    lockDefinition.commit();
    configHelper.addLock("* org.hibernate.collection.PersistentSet.*(..)", lockDefinition);
    
    configHelper.addIncludePattern("org.hibernate.collection.PersistentBag", false, false, false);
    lockDefinition = new LockDefinition("persistentBagLock", ConfigLockLevel.WRITE);
    lockDefinition.commit();
    configHelper.addLock("* org.hibernate.collection.PersistentBag.*(..)", lockDefinition);
    
    configHelper.addIncludePattern("org.hibernate.collection.PersistentList", false, false, false);
    lockDefinition = new LockDefinition("persistentListLock", ConfigLockLevel.WRITE);
    lockDefinition.commit();
    configHelper.addLock("* org.hibernate.collection.PersistentList.*(..)", lockDefinition);
    
    configHelper.addIncludePattern("org.hibernate.collection.PersistentMap", false, false, false);
    lockDefinition = new LockDefinition("persistentMapLock", ConfigLockLevel.WRITE);
    lockDefinition.commit();
    configHelper.addLock("* org.hibernate.collection.PersistentMap.*(..)", lockDefinition);
    
    /**
     * Session
     
    spec = configHelper.getOrCreateSpec("org.hibernate.jdbc.JDBCContext");
    spec.addTransient("borrowedConnection");
    spec.addTransient("hibernateTransaction");
    
    spec = configHelper.getOrCreateSpec("org.hibernate.jdbc.ConnectionManager");
    spec.addTransient("connection");
    spec.addTransient("borrowedConnection");
    spec.addTransient("factory");
    
    configHelper.addIncludePattern("org.hibernate.ConnectionReleaseMode", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.EmptyInterceptor", false, false, false);
    
    */
    
    /**
     * Second level cache begin
     */
    configHelper.addIncludePattern("org.hibernate.cache.EhCache", false, false, false);
    configHelper.addIncludePattern("org.hibernate.cache.CacheKey", false, false, false);
    configHelper.addIncludePattern("org.hibernate.EntityMode", false, false, false);
    configHelper.addIncludePattern("org.hibernate.type.IntegerType", false, false, false);
    configHelper.addIncludePattern("org.hibernate.type.PrimitiveType", false, false, false);
    configHelper.addIncludePattern("org.hibernate.type.ImmutableType", false, false, false);
    configHelper.addIncludePattern("org.hibernate.type.NullableType", false, false, false);
    configHelper.addIncludePattern("org.hibernate.type.AbstractType", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.cache.ReadWriteCache$Item", false, false, false);
    configHelper.addIncludePattern("org.hibernate.cache.ReadWriteCache$Lock", false, false, false);
    configHelper.addIncludePattern("org.hibernate.cache.entry.CacheEntry", false, false, false);
    configHelper.addRoot("ReadWriteCache", "org.hibernate.cache.ReadWriteCache.cache");
    /**
     * Second level cache ends
     */
    
    
    //configHelper.addAutolock("* java.util.Collections$SynchronizedList.*(..)", ConfigLockLevel.WRITE);
    configHelper.addIncludePattern("org.hibernate.proxy.pojo.cglib.CGLIBLazyInitializer", false, false, false);
    configHelper.addIncludePattern("org.hibernate.proxy.pojo.BasicLazyInitializer", false, false, false);
    configHelper.addIncludePattern("org.hibernate.proxy.AbstractLazyInitializer", false, false, false);
//    configHelper.addIncludePattern("org.hibernate.impl.SessionImpl", true, false, false);
//    configHelper.addIncludePattern("org.hibernate.impl.AbstractSessionImpl", true, false, false);
    
    //org.hibernate.proxy.pojo.cglib.CGLIBLazyInitializer, org.hibernate.proxy.pojo.BasicLazyInitializer, org.hibernate.proxy.AbstractLazyInitializer
  }
  
  protected final void registerModuleSpec(final BundleContext context) {
    final Dictionary serviceProps = new Hashtable();
    serviceProps.put(Constants.SERVICE_VENDOR, "Terracotta, Inc.");
    serviceProps.put(Constants.SERVICE_DESCRIPTION, "Hibernate Plugin Spec");
    context.registerService(ModuleSpec.class.getName(), new HibernateModuleSpec(new HibernateChangeApplicatorSpec(getClass().getClassLoader())), serviceProps);
  }

}
