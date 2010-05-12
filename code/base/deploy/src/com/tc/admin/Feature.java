/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.lang.StringUtils;
import org.terracotta.modules.configuration.PresentationFactory;

import com.tc.management.beans.TIMByteProviderMBean;

import java.util.Iterator;

public class Feature {
  private final String             symbolicName;
  private final String             displayName;
  private final FeatureClassLoader loader;
  private PresentationFactory      presentationFactory;

  public static final String       PROP_FEATURE_READY = "featureReady";

  public Feature(String symbolicName, String displayName) {
    this.symbolicName = symbolicName;
    this.displayName = displayName;
    this.loader = new FeatureClassLoader(this);
  }

  public String getSymbolicName() {
    return symbolicName;
  }

  public String getDisplayName() {
    return displayName != null ? displayName : symbolicName;
  }

  public FeatureClassLoader getFeatureClassLoader() {
    return loader;
  }

  public boolean isReady() {
    return loader.isReady();
  }

  public boolean hasError() {
    return loader.hasError();
  }

  public Throwable getError() {
    return loader.getError();
  }

  public String getManifestEntry(String key) {
    Iterator<TIMByteProviderMBean> iter = loader.byteProviders();
    String value;
    while (iter.hasNext()) {
      TIMByteProviderMBean byteProvider = iter.next();
      if ((value = byteProvider.getManifestEntry(key)) != null) { return value; }
    }
    return null;
  }

  public synchronized PresentationFactory getPresentationFactory() throws ClassNotFoundException,
      IllegalAccessException, InstantiationException {
    if (presentationFactory == null) {
      String factoryName = getManifestEntry("Presentation-Factory");
      if (factoryName != null) {
        Class c = loader.loadClass(factoryName);
        if (c != null) {
          presentationFactory = (PresentationFactory) c.newInstance();
        }
      }
    }
    return presentationFactory;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Feature)) return false;

    Feature otherFeature = (Feature) other;
    return StringUtils.equals(getSymbolicName(), otherFeature.getSymbolicName());
  }

  @Override
  public int hashCode() {
    return symbolicName.hashCode();
  }

  @Override
  public String toString() {
    return symbolicName;
  }
}
