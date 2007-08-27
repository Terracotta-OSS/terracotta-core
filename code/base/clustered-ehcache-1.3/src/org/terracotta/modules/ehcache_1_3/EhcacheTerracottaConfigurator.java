package org.terracotta.modules.ehcache_1_3;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.ehcache.commons_1_0.EhcacheTerracottaCommonsConfigurator;

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

public class EhcacheTerracottaConfigurator extends EhcacheTerracottaCommonsConfigurator {
  protected void addInstrumentation(BundleContext context, StandardDSOClientConfigHelper configHelper) {
    super.addInstrumentation(context, configHelper);

    ClassAdapterFactory factory = new EhcacheCacheManagerClassAdapter();
    TransparencyClassSpec spec = configHelper.getOrCreateSpec(CACHE_MANAGER_CLASS_NAME_DOTS);
    spec.setCustomClassAdapter(factory);
  }

  protected String getExportedBundleName() {
    return EHCACHE_13_BUNDLE_NAME;
  }
}
