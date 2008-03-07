/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.util.concurrent.CopyOnWriteArrayMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class StatisticsRetrievalRegistryImpl implements StatisticsRetrievalRegistry {
  private final static TCLogger logger = CustomerLogging.getDSOGenericLogger();

  private final Map instanceMap = new CopyOnWriteArrayMap();

  public void removeAllActionInstances() {
    instanceMap.clear();
  }

  public Collection getSupportedStatistics() {
    return Collections.unmodifiableSet(instanceMap.keySet());
  }

  public Collection getRegisteredActionInstances() {
    return Collections.unmodifiableCollection(instanceMap.values());
  }

  public StatisticRetrievalAction getActionInstance(final String name) {
    if (null == name) {
      return null;
    }
    return (StatisticRetrievalAction)instanceMap.get(name);
  }

  public void registerActionInstance(final StatisticRetrievalAction action) {
    if (null == action) {
      return;
    }
    instanceMap.put(action.getName(), action);
  }

  public void registerActionInstance(final String sraClassName) {
    try {
      Class sra_cpu_class = Class.forName(sraClassName);
      StatisticRetrievalAction sra_cpu_action = (StatisticRetrievalAction)sra_cpu_class.newInstance();
      registerActionInstance(sra_cpu_action);
    } catch (ClassNotFoundException e) {
      logger.warn("Statistic retrieval action " + sraClassName + " wasn't activated since it couldn't be found in the classpath.");
    } catch (UnsupportedClassVersionError e) {
      logger.warn("Statistic retrieval action " + sraClassName + " wasn't activated since it is was compiled for a later JVM : " + e.getMessage() + ".");
    } catch (Exception e) {
      throw new TCRuntimeException("Unexpected error while instantiating statistic retrieval action " + sraClassName, e);
    }
  }
}
