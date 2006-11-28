package com.tctest.util;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.CyclicBarrier;

/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */

public abstract class AbstractTransparentAppMultiplexer extends AbstractTransparentApp
{
    private final CyclicBarrier myBarrier;
    
    public AbstractTransparentAppMultiplexer(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) 
    {
      super(appId, cfg, listenerProvider);
      myBarrier = new CyclicBarrier(getParticipantCount());
    }
    
    public void run()
    {
      try {
        int index = myBarrier.await();
        run(myBarrier, index);
      } catch (Throwable t) {
        notifyError(t);
      }
    }
    
    public abstract void run(CyclicBarrier barrier, int index) throws Throwable;
    
    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config)
    {
      TransparencyClassSpec spec = config.getOrCreateSpec(AbstractTransparentAppMultiplexer.class.getName());
      
      DSOConfigUtil.addWriteAutolock(config, CyclicBarrier.class);
      DSOConfigUtil.addRoot(spec, "myBarrier");
    }
}
