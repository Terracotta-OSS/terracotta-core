package org.terracotta.modules.lucene_2_0_0;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.IStandardDSOClientConfigHelper;

public class LuceneTerracottaConfigurator extends TerracottaConfiguratorModule {

  protected final void addInstrumentation(final BundleContext context, final IStandardDSOClientConfigHelper configHelper) {
    configHelper.getOrCreateSpec("org.apache.lucene.store.RAMFile").setCustomClassAdapter(new RAMFileAdapter());

    configHelper.addCustomAdapter("org.apache.lucene.store.RAMOutputStream", new RAMFileExternalAccessAdatper());
    configHelper.addCustomAdapter("org.apache.lucene.store.RAMInputStream", new RAMFileExternalAccessAdatper());

    configHelper.addCustomAdapter("org.apache.lucene.store.RAMDirectory$1", new RAMDirectoryLockAdapter());

    configHelper.getOrCreateSpec("org.apache.lucene.store.RAMDirectory").setCustomClassAdapter(
        new RAMFileExternalAccessAdatper());
  }

}
