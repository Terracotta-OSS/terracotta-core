/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.util.AbstractTransparentAppMultiplexer;
import com.tctest.util.DSOConfigUtil;
import com.tctest.util.TestUtil;
import com.tctest.util.Timer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

public class WorkQueueTestApp extends AbstractTransparentAppMultiplexer
{
    private static final int NUM_ITEMS = 100;
    private static final int SIZE_ITEMS = 10;

    private final BlockingQueue<Object> queue;
    private final CyclicBarrier readBarrier;
    private final Object poison;

    private ItemGenerator itemGenerator = new ItemGenerator();

    public static class Item
    {
        public byte[] data;
    }

    public static class ItemGenerator
    {
        public Object next()
        {
            Item item = new Item();
            item.data = new byte[SIZE_ITEMS];
            return item;
        }

    }

    public WorkQueueTestApp(String appId, ApplicationConfig cfg,
                    ListenerProvider listenerProvider)
    {
        super(appId, cfg, listenerProvider);
        queue = new LinkedBlockingQueue<Object>();
        readBarrier = new CyclicBarrier(Math.min(getParticipantCount(), 2));
        poison = new Object();
    }

    public void run(CyclicBarrier barrier, int index) throws Throwable
    {
        if (index == 0) {
            doPuts();
            return;
        }

        doReads();
    }

    private void doPuts() throws Exception
    {
        Timer t = new Timer();

        System.out.println("Warming up...");

        for (int i = 0; i < NUM_ITEMS; i++) {
            queue.put(itemGenerator.next());
        }

        // put the read barrier in the queue so that we
        // wait for the last warmup item to be read
        queue.put(readBarrier);
        readBarrier.await();

        // dump the items
        System.out.println("Putting items...");
        int total = NUM_ITEMS*getIntensity();
        t.start();
        for (int i = 0; i < total; i++) {
            queue.put(itemGenerator.next());
        }

        // put the read barrier in the queue so that we
        // wait for the last item to be read
        queue.put(readBarrier);
        readBarrier.await();

        // stop the timer
        t.stop();

        // add one more object to the total to account
        // for the read barrier
        total++; 
        
        // send poison to one reader (each reader will requeue it to 
        // kill them all)
        queue.put(poison);
        
        TestUtil.printStats("" + getParticipantCount(), "nodes");
        TestUtil.printStats("" + total, "transactions");
        TestUtil.printStats("" + t.elapsed(), "milliseconds");
        TestUtil.printStats("" + t.tps(total), "tps");
    }

    private void doReads() throws Exception
    {
        System.out.println("Getting items...");

        while (true) {
            Object item = queue.take();
            if (item instanceof CyclicBarrier) {
                ((CyclicBarrier) item).await();
                continue;
            }
            if (item == poison) {
                break;
            }
        }

        queue.put(poison);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config)
    {
        TransparencyClassSpec spec = config.getOrCreateSpec(WorkQueueTestApp.class.getName());

        AbstractTransparentAppMultiplexer.visitL1DSOConfig(visitor, config);

        DSOConfigUtil.autoLockAndInstrumentClass(config, WorkQueueTestApp.class);

        DSOConfigUtil.addRoot(spec, "queue");
        DSOConfigUtil.addRoot(spec, "readBarrier");
        DSOConfigUtil.addRoot(spec, "poison");
    }
}
