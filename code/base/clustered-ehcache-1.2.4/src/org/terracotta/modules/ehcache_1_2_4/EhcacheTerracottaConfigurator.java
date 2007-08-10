package org.terracotta.modules.ehcache_1_2_4;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.StandardDSOClientConfigHelper;

public class EhcacheTerracottaConfigurator extends TerracottaConfiguratorModule implements Constants {

	protected void addInstrumentation(final BundleContext context,
			final StandardDSOClientConfigHelper configHelper) {
		ClassAdapterFactory factory = new EhcacheLruMemoryStoreAdapter();
		TransparencyClassSpec spec = ((StandardDSOClientConfigHelper)configHelper)
				.getOrCreateSpec(LRUMEMORYSTORE_CLASS_NAME_DOTS);
		spec.setCallConstructorOnLoad(true);
		spec.setCustomClassAdapter(factory);
	}
}
