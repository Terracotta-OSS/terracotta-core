/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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
import java.util.stream.Stream;

class IStripeMonitoringWrapper implements IStripeMonitoring {

  private final Collection<IStripeMonitoring> underlying;
  private final Logger logger;
  private volatile boolean isPassive = true;

  public IStripeMonitoringWrapper(Collection<IStripeMonitoring> underlying, Logger logger) {
    this.underlying = underlying;
    this.logger = logger;
  }

  @Override
  public void serverDidBecomeActive(PlatformServer self) {
    try {
      isPassive = false;
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
      buildSelectiveStream(isPassive).map(u->u.addNode(sender, parents, name, value)).reduce(Boolean.TRUE, (m,n)->m&n);
    } catch (Exception e) {
      logger.warn("caught exception while invoking addNode", e);
    }
    return false;
  }

  @Override
  public boolean removeNode(PlatformServer sender, String[] parents, String name) {
    try {
      return buildSelectiveStream(isPassive).map(u->u.removeNode(sender, parents, name)).reduce(Boolean.TRUE, (m,n)->m&n);
    } catch (Exception e) {
      logger.warn("caught exception while invoking removeNode", e);
    }
    return false;
  }

  @Override
  public void pushBestEffortsData(PlatformServer sender, String name, Serializable data) {
    try {
      buildSelectiveStream(isPassive).forEach(u->u.pushBestEffortsData(sender, name, data));
    } catch (Exception e) {
      logger.warn("caught exception while invoking pushBestEffortsData", e);
    }
  }

  private Stream<IStripeMonitoring> buildSelectiveStream(boolean passive) {
      Stream<IStripeMonitoring> s = underlying.stream();
      if (passive) {
        s = s.filter(IStripeMonitoring::requiresLocalDeliveryOnPassive);
      }
      return s;
  }

  @Override
  public void replayNode(PlatformServer sender, String[] parents, String name, Serializable value) {
    try {
      underlying.forEach(u->u.replayNode(sender, parents, name, value));
    } catch (Exception e) {
      logger.warn("caught exception while invoking addNode", e);
    }
  }

  @Override
  public boolean requiresLocalDeliveryOnPassive() {
    return underlying.stream().anyMatch(IStripeMonitoring::requiresLocalDeliveryOnPassive);
  }
}
