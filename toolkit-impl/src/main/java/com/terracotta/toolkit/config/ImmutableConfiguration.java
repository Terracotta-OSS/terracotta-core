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
package com.terracotta.toolkit.config;

import org.terracotta.toolkit.config.Configuration;

import java.io.Serializable;

public class ImmutableConfiguration extends UnclusteredConfiguration {

  public ImmutableConfiguration(Configuration configuration) {
    super(configuration);
  }

  @Override
  public final void internalSetConfigMapping(String name, Serializable value) {
    throw new UnsupportedOperationException("This configuration is immutable, cannot change '" + name + "' to " + value);
  }

}
