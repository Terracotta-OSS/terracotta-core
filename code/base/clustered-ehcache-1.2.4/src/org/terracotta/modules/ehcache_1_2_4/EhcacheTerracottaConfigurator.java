package org.terracotta.modules.ehcache_1_2_4;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

public class EhcacheTerracottaConfigurator extends TerracottaConfiguratorModule {

	public void start(final BundleContext context) throws Exception {
		final ServiceReference configHelperRef = context
				.getServiceReference(StandardDSOClientConfigHelper.class
						.getName());
		if (configHelperRef != null) {
			final StandardDSOClientConfigHelper configHelper = (StandardDSOClientConfigHelper) context
					.getService(configHelperRef);
			addEhcacheInstrumentation(configHelper);
			context.ungetService(configHelperRef);
		} else {
			throw new BundleException("Expected the "
					+ StandardDSOClientConfigHelper.class.getName()
					+ " service to be registered, was unable to find it");
		}
	}

	public void stop(final BundleContext context) throws Exception {
		// Ignore this, we don't need to stop anything
	}

	private void addEhcacheInstrumentation(
			final StandardDSOClientConfigHelper configHelper) {
	    ClassAdapterFactory factory = new EhcacheLruMemoryStoreAdapter();
	    TransparencyClassSpec spec = configHelper.getOrCreateSpec("net.sf.ehcache.store.LruMemoryStore$SpoolingLinkedHashMap");
	    spec.setCustomClassAdapter(factory);
	    
	    factory = new EhcacheMemoryStoreAdapter();
	    spec = configHelper.getOrCreateSpec("net.sf.ehcache.store.MemoryStore");
	    spec.setCustomClassAdapter(factory);
	}
}
