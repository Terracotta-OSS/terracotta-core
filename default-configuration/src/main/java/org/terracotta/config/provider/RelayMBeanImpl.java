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
package org.terracotta.config.provider;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;


/**
 *
 */
public class RelayMBeanImpl extends StandardMBean implements RelayMBean {
  private final DefaultConfigurationProvider provider;

  public RelayMBeanImpl(DefaultConfigurationProvider provider) throws NotCompliantMBeanException {
    super(RelayMBean.class);
    this.provider = provider;
  }

  @Override
  public boolean clearRelay() {
    return provider.clearRelays();
  }
}
