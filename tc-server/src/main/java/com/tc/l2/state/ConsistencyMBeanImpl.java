/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.l2.state;

import com.tc.management.AbstractTerracottaMBean;
import org.terracotta.server.ServerEnv;

import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConsistencyMBeanImpl extends AbstractTerracottaMBean implements ConsistencyMBean {

  private final ConsistencyManager consistencyManager;

  public ConsistencyMBeanImpl(ConsistencyManager consistencyManager) throws Exception {
    super(ConsistencyMBean.class, false);
    this.consistencyManager = consistencyManager;
  }

  @Override
  public boolean isBlocked() {
    return consistencyManager.lastTransitionSuspended();
  }

  @Override
  public boolean isStuck() {
    return consistencyManager.lastTransitionSuspended();
  }

  @Override
  public Collection<String> requestedActions() {
    return consistencyManager.requestedActions().stream().map(t->t.toString()).collect(Collectors.toList());
  }

  @Override
  public void allowRequestedTransition() {
    ServerEnv.getServer().audit("Allow server state transition invoked", new Properties());
    consistencyManager.allowLastTransition();
  }

  @Override
  public void reset() {
    //
  }
}
