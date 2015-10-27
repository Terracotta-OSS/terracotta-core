/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.util.Assert;

/**
 * A base class for all new config objects.
 */
public class BaseConfigObject implements Config {

  protected final ConfigContext context;

  public BaseConfigObject(ConfigContext context) {
    Assert.assertNotNull(context);
    this.context = context;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " around bean:\n" + context.bean();
  }

  @Override
  public Object getBean() {
    return this.context.bean();
  }
  
}
