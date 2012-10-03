/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.util.Assert;
import com.tc.util.concurrent.CopyOnWriteSequentialMap;

import java.util.Collection;

public class StatisticsRetrievalRegistryImpl implements StatisticsRetrievalRegistry {
  private final static TCLogger LOGGER = TCLogging.getLogger(StatisticsRetrievalRegistryImpl.class);

  private final CopyOnWriteSequentialMap<String, StatisticRetrievalAction> instanceMap = new CopyOnWriteSequentialMap<String, StatisticRetrievalAction>();

  @Override
  public void removeAllActionInstances() {
    instanceMap.clear();
  }

  @Override
  public Collection<String> getSupportedStatistics() {
    return instanceMap.keySet();
  }

  @Override
  public Collection<StatisticRetrievalAction> getRegisteredActionInstances() {
    return instanceMap.values();
  }

  @Override
  public StatisticRetrievalAction getActionInstance(final String name) {
    if (null == name) {
      return null;
    }
    return instanceMap.get(name);
  }

  @Override
  public void registerActionInstance(final StatisticRetrievalAction action) {
    if (null == action) {
      return;
    }
    Assert.assertNotNull("Name of SRA " + action, action.getName());
    Assert.assertFalse("The SRA " + action + " is registering a non-unique name '" + action.getName() + "'.", instanceMap.containsKey(action.getName()));
    instanceMap.put(action.getName(), action);
  }

  @Override
  public void registerActionInstance(final String sraClassName) {
    try {
      Class sra_cpu_class = Class.forName(sraClassName);
      StatisticRetrievalAction sra_cpu_action = (StatisticRetrievalAction)sra_cpu_class.newInstance();
      registerActionInstance(sra_cpu_action);
    } catch (NoClassDefFoundError e) {
      LOGGER.warn("Statistic retrieval action " + sraClassName + " wasn't activated since its definition couldn't be found.");
    } catch (ClassNotFoundException e) {
      LOGGER.warn("Statistic retrieval action " + sraClassName + " wasn't activated since it couldn't be found in the classpath.");
    } catch (UnsupportedClassVersionError e) {
      LOGGER.warn("Statistic retrieval action " + sraClassName + " wasn't activated since it is was compiled for a later JVM : " + e.getMessage() + ".");
    } catch (Exception e) {
      LOGGER.warn("Unexpected error while instantiating statistic retrieval action " + sraClassName, e);
    }
  }
}
