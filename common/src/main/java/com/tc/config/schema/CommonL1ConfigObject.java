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
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.util.Assert;
import com.terracottatech.config.Client;

import java.io.File;

/**
 * The standard implementation of {@link CommonL1Config}.
 */
public class CommonL1ConfigObject extends BaseConfigObject implements CommonL1Config {

  public CommonL1ConfigObject(ConfigContext context) {
    super(context);
    Assert.assertNotNull(context);

    this.context.ensureRepositoryProvides(Client.class);
  }

  @Override
  public File logsPath() {
    final Client client = (Client) this.context.bean();
    return new File(client.getLogs());
  }

}
