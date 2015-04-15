/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
  public InputStream getInputStream(long maxTimeoutMillis) throws ConfigurationSetupException {
    InputStream out = this.relativeTo.getResourceAsStream(this.path);
    if (out == null) throw new ConfigurationSetupException("Resource '" + this.path + "', relative to class "
                                                           + this.relativeTo.getName() + ", does not exist");
    return out;
  }
  
  @Override
  public File directoryLoadedFrom() {
    return null;
  }

  @Override
  public boolean isTrusted() {
    return false;
  }

  @Override
  public String toString() {
    return "Java resource at '" + this.path + "', relative to class " + this.relativeTo.getName();
  }

}
