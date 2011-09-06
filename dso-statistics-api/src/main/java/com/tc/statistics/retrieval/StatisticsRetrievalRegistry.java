/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval;

import com.tc.statistics.StatisticRetrievalAction;

import java.util.Collection;

/**
 * This interface describes the SRA registry that will be part of each agent system. The registry is built at startup
 * and contains an instance of each SRA that is supported by the agent. An SRA instance can either be
 * {@link #registerActionInstance(StatisticRetrievalAction) registered directly} by providing an instance, or
 * {@link #registerActionInstance(String) dynamically} by providing a class name. The latter allows optional SRAs to be
 * added by simply adding the appropriate classes to the classpath. Also, if the SRA class is found to require a newer
 * version of the JVM than the one that is currently used by the agent, this will simply not activate that particular
 * SRA. In short, the dynamic method is a permissive way to register SRAs, which will log warnings for those that
 * couldn't be activated and simply run an agent with those SRA capabilities.
 */
public interface StatisticsRetrievalRegistry {
  public void removeAllActionInstances();

  public Collection getSupportedStatistics();

  public Collection getRegisteredActionInstances();

  public StatisticRetrievalAction getActionInstance(String name);

  public void registerActionInstance(StatisticRetrievalAction action);

  public void registerActionInstance(String sraClassName);
}