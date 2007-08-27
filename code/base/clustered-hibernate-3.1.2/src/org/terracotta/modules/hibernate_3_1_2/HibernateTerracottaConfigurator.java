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

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.ModuleSpec;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

import java.util.Dictionary;
import java.util.Hashtable;

public final class HibernateTerracottaConfigurator extends TerracottaConfiguratorModule {
  protected final void addInstrumentation(final BundleContext context, final StandardDSOClientConfigHelper configHelper) {
    /* AutoSynchronized lock for AbstractPersistentCollection, PersistentSet, PersistentBag, PersistentList, and PersistentMap are defined in the terracotta.xml */
    TransparencyClassSpec spec = configHelper.getOrCreateSpec("org.hibernate.collection.AbstractPersistentCollection");
    spec.addTransient("session");
    
    configHelper.addIncludePattern("org.hibernate.collection.PersistentSet", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.collection.PersistentBag", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.collection.PersistentList", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.collection.PersistentMap", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.type.ComponentType", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.tuple..*", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.engine..*", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.type..*", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.FetchMode", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.property..*", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.cache.QueryKey", false, false, false);
    
    /**
     * Second level cache begin
     */
    configHelper.addIncludePattern("org.hibernate.cache.EhCache", false, false, false);
    configHelper.addIncludePattern("org.hibernate.cache.CacheKey", false, false, false);
    configHelper.addIncludePattern("org.hibernate.cache.EhCacheProvider", false, false, false);
    configHelper.addIncludePattern("org.hibernate.EntityMode", false, false, false);
    configHelper.addIncludePattern("org.hibernate.type.IntegerType", false, false, false);
    configHelper.addIncludePattern("org.hibernate.type.PrimitiveType", false, false, false);
    configHelper.addIncludePattern("org.hibernate.type.ImmutableType", false, false, false);
    configHelper.addIncludePattern("org.hibernate.type.NullableType", false, false, false);
    configHelper.addIncludePattern("org.hibernate.type.AbstractType", false, false, false);
    
    configHelper.addIncludePattern("org.hibernate.cache.ReadWriteCache$Item", false, false, false);
    configHelper.addIncludePattern("org.hibernate.cache.ReadWriteCache$Lock", false, false, false);
    configHelper.addIncludePattern("org.hibernate.cache.entry.CacheEntry", false, false, false);
    configHelper.addIncludePattern("org.hibernate.cache.entry.CollectionCacheEntry", false, false, false);
    /**
     * Second level cache ends
     */
    
    configHelper.addIncludePattern("org.hibernate.proxy.pojo.cglib.CGLIBLazyInitializer", false, false, false);
    configHelper.addIncludePattern("org.hibernate.proxy.pojo.BasicLazyInitializer", false, false, false);
    configHelper.addIncludePattern("org.hibernate.proxy.AbstractLazyInitializer", false, false, false);
    
    ClassAdapterFactory factory = new EhcacheClassAdapter();
    spec = configHelper.getOrCreateSpec("org.hibernate.cache.EhCache");
    spec.setCustomClassAdapter(factory);
    
    factory = new EhcacheProviderClassAdapter();
    spec = configHelper.getOrCreateSpec("org.hibernate.cache.EhCacheProvider");
    spec.setCustomClassAdapter(factory);
  }
  
  protected final void registerModuleSpec(final BundleContext context) {
    final Dictionary serviceProps = new Hashtable();
    serviceProps.put(Constants.SERVICE_VENDOR, "Terracotta, Inc.");
    serviceProps.put(Constants.SERVICE_DESCRIPTION, "Hibernate Plugin Spec");
    serviceProps.put(Constants.SERVICE_RANKING, ModuleSpec.HIGN_RANK);
    context.registerService(ModuleSpec.class.getName(), new HibernateModuleSpec(new HibernateChangeApplicatorSpec(getClass().getClassLoader())), serviceProps);
  }

}
