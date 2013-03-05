package com.terracotta.management;

import com.terracotta.management.resource.services.*;
import com.terracotta.management.web.resource.services.IdentityAssertionResourceService;
import net.sf.ehcache.management.resource.services.ElementsResourceServiceImpl;

import java.util.HashSet;
import java.util.Set;

public class ApplicationTsa extends javax.ws.rs.core.Application {


  public Set<Class<?>> getClasses() {
    Set<Class<?>> s = new HashSet<Class<?>>();
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

    s.add(net.sf.ehcache.management.resource.services.CacheStatisticSamplesResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CachesResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagersResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagerConfigsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheConfigsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.AgentsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.exceptions.WebApplicationExceptionMapper.class);
    s.add(net.sf.ehcache.management.resource.exceptions.DefaultExceptionMapper.class);
    s.add(net.sf.ehcache.management.resource.exceptions.ResourceRuntimeExceptionMapper.class);
    return s;
  }


}