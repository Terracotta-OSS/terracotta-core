package com.terracotta.management;

import net.sf.ehcache.management.resource.services.ElementsResourceServiceImpl;

import org.terracotta.management.application.DefaultApplication;

import com.terracotta.management.resource.services.BackupResourceServiceImpl;
import com.terracotta.management.resource.services.ConfigurationResourceServiceImpl;
import com.terracotta.management.resource.services.DiagnosticsResourceServiceImpl;
import com.terracotta.management.resource.services.JmxResourceServiceImpl;
import com.terracotta.management.resource.services.LogsResourceServiceImpl;
import com.terracotta.management.resource.services.MonitoringResourceServiceImpl;
import com.terracotta.management.resource.services.OperatorEventsResourceServiceImpl;
import com.terracotta.management.resource.services.ShutdownResourceServiceImpl;
import com.terracotta.management.resource.services.TopologyResourceServiceImpl;
import com.terracotta.management.web.resource.services.IdentityAssertionResourceService;

import java.util.HashSet;
import java.util.Set;

public class ApplicationTsa extends DefaultApplication {


  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> s = new HashSet<Class<?>>(super.getClasses());
    s.add(ElementsResourceServiceImpl.class);
    s.add(BackupResourceServiceImpl.class);
    s.add(ConfigurationResourceServiceImpl.class);
    s.add(DiagnosticsResourceServiceImpl.class);
    s.add(LogsResourceServiceImpl.class);
    s.add(MonitoringResourceServiceImpl.class);
    s.add(OperatorEventsResourceServiceImpl.class);
    s.add(ShutdownResourceServiceImpl.class);
    s.add(TopologyResourceServiceImpl.class);
    s.add(IdentityAssertionResourceService.class);
    s.add(JmxResourceServiceImpl.class);

    s.add(net.sf.ehcache.management.resource.services.CacheStatisticSamplesResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CachesResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagersResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagerConfigsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheConfigsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.AgentsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.QueryResourceServiceImpl.class);
    
    s.add(org.terracotta.session.management.SessionsResourceServiceImpl.class);

    return s;
  }


}