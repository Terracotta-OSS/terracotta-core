package com.tc.services;

import org.slf4j.Logger;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformServer;

import java.io.Serializable;

class IStripeMonitoringWrapper implements IStripeMonitoring {

  private final IStripeMonitoring underlying;
  private final Logger logger;

  public IStripeMonitoringWrapper(IStripeMonitoring underlying, Logger logger) {
    this.underlying = underlying;
    this.logger = logger;
  }

  @Override
  public void serverDidBecomeActive(PlatformServer self) {
    try {
      underlying.serverDidBecomeActive(self);
    } catch (Exception e) {
      logger.warn("caught exception while invoking serverDidBecomeActive", e);
    }
  }

  @Override
  public void serverDidJoinStripe(PlatformServer server) {
    try {
      underlying.serverDidJoinStripe(server);
    } catch (Exception e) {
      logger.warn("caught exception while invoking serverDidJoinStripe", e);
    }
  }

  @Override
  public void serverDidLeaveStripe(PlatformServer server) {
    try {
      underlying.serverDidLeaveStripe(server);
    } catch (Exception e) {
      logger.warn("caught exception while invoking serverDidLeaveStripe", e);
    }
  }

  @Override
  public boolean addNode(PlatformServer sender, String[] parents, String name, Serializable value) {
    try {
      return underlying.addNode(sender, parents, name, value);
    } catch (Exception e) {
      logger.warn("caught exception while invoking addNode", e);
    }
    return false;
  }

  @Override
  public boolean removeNode(PlatformServer sender, String[] parents, String name) {
    try {
      return underlying.removeNode(sender, parents, name);
    } catch (Exception e) {
      logger.warn("caught exception while invoking removeNode", e);
    }
    return false;
  }

  @Override
  public void pushBestEffortsData(PlatformServer sender, String name, Serializable data) {
    try {
      underlying.pushBestEffortsData(sender, name, data);
    } catch (Exception e) {
      logger.warn("caught exception while invoking pushBestEffortsData", e);
    }
  }
}
