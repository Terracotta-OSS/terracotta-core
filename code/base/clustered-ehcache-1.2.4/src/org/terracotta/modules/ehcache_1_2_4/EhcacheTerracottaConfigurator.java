package org.terracotta.modules.ehcache_1_2_4;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.IStandardDSOClientConfigHelper;
import com.tc.object.config.ITransparencyClassSpec;

public class EhcacheTerracottaConfigurator extends TerracottaConfiguratorModule implements IConstants {

	protected void addInstrumentation(final BundleContext context,
			final IStandardDSOClientConfigHelper configHelper) {
		ClassAdapterFactory factory = new EhcacheLruMemoryStoreAdapter();
		ITransparencyClassSpec spec = ((IStandardDSOClientConfigHelper)configHelper)
				.getOrCreateSpec(LRUMEMORYSTORE_CLASS_NAME_DOTS);
		spec.setCallConstructorOnLoad(true);
		spec.setCustomClassAdapter(factory);
	}
}
