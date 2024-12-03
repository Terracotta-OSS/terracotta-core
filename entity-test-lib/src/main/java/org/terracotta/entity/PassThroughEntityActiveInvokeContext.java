/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.entity;

import java.util.Properties;

public class PassThroughEntityActiveInvokeContext<R extends EntityResponse> extends PassThroughEntityInvokeContext implements
  ActiveInvokeContext<R> {
  private final ClientDescriptor descriptor;

  public PassThroughEntityActiveInvokeContext(ClientDescriptor descriptor, int concurrencyKey, long current, long
    oldest) {
    super(descriptor.getSourceId(), concurrencyKey, current, oldest);
    this.descriptor = descriptor;
  }

  @Override
  public ClientDescriptor getClientDescriptor() {
    return descriptor;
  }

  @Override
  public ActiveInvokeChannel<R> openInvokeChannel() {
    return new PassThroughEntityActiveInvokeChannel<>();
  }

  @Override
  public Properties getClientSourceProperties() {
    Properties props = new Properties();
    props.setProperty("clientID", String.valueOf(descriptor.getSourceId()));
    return props;
  }
}
