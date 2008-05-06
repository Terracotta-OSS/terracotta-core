/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.setup.sources;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.util.Assert;

import java.io.File;
import java.io.InputStream;

/**
 * A {@link ConfigurationSource} that returns data from a Java resource.
 */
public class ResourceConfigurationSource implements ConfigurationSource {

  private final String path;
  private final Class  relativeTo;

  public ResourceConfigurationSource(String path, Class relativeTo) {
    Assert.assertNotBlank(path);
    Assert.assertNotNull(relativeTo);

    this.path = path;
    this.relativeTo = relativeTo;
  }

  public InputStream getInputStream(long maxTimeoutMillis) throws ConfigurationSetupException {
    InputStream out = this.relativeTo.getResourceAsStream(this.path);
    if (out == null) throw new ConfigurationSetupException("Resource '" + this.path + "', relative to class "
                                                           + this.relativeTo.getName() + ", does not exist");
    return out;
  }
  
  public File directoryLoadedFrom() {
    return null;
  }

  public boolean isTrusted() {
    return false;
  }

  public String toString() {
    return "Java resource at '" + this.path + "', relative to class " + this.relativeTo.getName();
  }

}
