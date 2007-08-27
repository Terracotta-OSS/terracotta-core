/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.configuration;

import java.net.MalformedURLException;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.config.StandardDSOClientConfigHelper;

public abstract class TerracottaConfiguratorModule implements BundleActivator {

	protected ServiceReference getConfigHelperReference(BundleContext context)
			throws Exception {
		final String CONFIGHELPER_CLASS_NAME = "com.tc.object.config.StandardDSOClientConfigHelperImpl";
		final ServiceReference configHelperRef = context
				.getServiceReference(CONFIGHELPER_CLASS_NAME);
		if (configHelperRef == null) {
			throw new BundleException("Expected the " + CONFIGHELPER_CLASS_NAME
					+ " service to be registered, was unable to find it");
		}
		return configHelperRef;
	}

	public void start(final BundleContext context) throws Exception {
		final ServiceReference configHelperRef = getConfigHelperReference(context);
		final StandardDSOClientConfigHelper configHelper = (StandardDSOClientConfigHelper) context
				.getService(configHelperRef);
		addInstrumentation(context, configHelper);
		context.ungetService(configHelperRef);
		registerModuleSpec(context);
	}

	public void stop(final BundleContext context) throws Exception {
		// Ignore this, we don't need to stop anything
	}

	protected void addInstrumentation(final BundleContext context,
			final StandardDSOClientConfigHelper configHelper) {
		// default empty body
	}

	protected void registerModuleSpec(final BundleContext context) {
		// default empty body
	}

	protected final String getBundleJarUrl(final Bundle bundle) {
		return "jar:" + bundle.getLocation() + "!/";
	}

	protected final void addClassReplacement(
			final StandardDSOClientConfigHelper configHelper,
			final Bundle bundle, final String originalClassName,
			final String replacementClassName) {
		String url = getBundleJarUrl(bundle)
				+ ByteCodeUtil.classNameToFileName(replacementClassName);
		try {
			configHelper.addClassReplacement(originalClassName,
					replacementClassName, new URL(url));
		} catch (MalformedURLException e) {
			throw new RuntimeException(
					"Unexpected error while constructing the URL '" + url + "'",
					e);
		}
	}

	protected final void addExportedBundleClass(
			final StandardDSOClientConfigHelper configHelper,
			final Bundle bundle, final String classname) {
		String url = getBundleJarUrl(bundle)
				+ ByteCodeUtil.classNameToFileName(classname);
		try {
			configHelper.addClassResource(classname, new URL(url));
		} catch (MalformedURLException e) {
			throw new RuntimeException(
					"Unexpected error while constructing the URL '" + url + "'",
					e);
		}
	}

	protected final void addExportedTcJarClass(
			final StandardDSOClientConfigHelper configHelper,
			final String classname) {
		configHelper.addClassResource(classname,
				TerracottaConfiguratorModule.class.getClassLoader()
						.getResource(
								ByteCodeUtil.classNameToFileName(classname)));
	}

}
