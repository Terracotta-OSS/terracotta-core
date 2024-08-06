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
package com.tc.services;

import org.slf4j.Logger;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformServer;

import java.io.Serializable;
import java.util.Collection;

class IStripeMonitoringWrapper implements IStripeMonitoring {

  private final Collection<IStripeMonitoring> underlying;
  private final Logger logger;

  public IStripeMonitoringWrapper(Collection<IStripeMonitoring> underlying, Logger logger) {
    this.underlying = underlying;
    this.logger = logger;
  }

  @Override
  public void serverDidBecomeActive(PlatformServer self) {
    try {
      underlying.forEach(u->u.serverDidBecomeActive(self));
    } catch (Exception e) {
      logger.warn("caught exception while invoking serverDidBecomeActive", e);
    }
  }

  @Override
  public void serverDidJoinStripe(PlatformServer server) {
    try {
      underlying.forEach(u->u.serverDidJoinStripe(server));
    } catch (Exception e) {
      logger.warn("caught exception while invoking serverDidJoinStripe", e);
    }
  }

  @Override
  public void serverDidLeaveStripe(PlatformServer server) {
    try {
      underlying.forEach(u->u.serverDidLeaveStripe(server));
    } catch (Exception e) {
      logger.warn("caught exception while invoking serverDidLeaveStripe", e);
    }
  }

  @Override
  public boolean addNode(PlatformServer sender, String[] parents, String name, Serializable value) {
    try {
      return underlying.stream().map(u->u.addNode(sender, parents, name, value)).reduce(Boolean.TRUE, (m,n)->m&n);
    } catch (Exception e) {
      logger.warn("caught exception while invoking addNode", e);
    }
    return false;
  }

  @Override
  public boolean removeNode(PlatformServer sender, String[] parents, String name) {
    try {
      return underlying.stream().map(u->u.removeNode(sender, parents, name)).reduce(Boolean.TRUE, (m,n)->m&n);
    } catch (Exception e) {
      logger.warn("caught exception while invoking removeNode", e);
    }
    return false;
  }

  @Override
  public void pushBestEffortsData(PlatformServer sender, String name, Serializable data) {
    try {
      underlying.forEach(u->u.pushBestEffortsData(sender, name, data));
    } catch (Exception e) {
      logger.warn("caught exception while invoking pushBestEffortsData", e);
    }
  }
}
