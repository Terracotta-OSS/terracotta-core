/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tctest.util.ThreadUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueMultiplexer
{
    private static final int MAX_BUFFER = 10;
    
    private transient BlockingQueue<Object> inputQueue;

    private List<BlockingQueue<Object>> outputQueues = new ArrayList<BlockingQueue<Object>>();
    
    public BlockingQueue<Object> getNewOutputQueue()
    {
        synchronized (outputQueues) {
            BlockingQueue<Object> q = new LinkedBlockingQueue<Object>();
            outputQueues.add(q);
            outputQueues.notify();
            return q;
        }
    }
    
    private class OutputQueueWriter implements Runnable
    {
        private BlockingQueue<Object> outputQueue;
        
        public OutputQueueWriter(BlockingQueue<Object> outputQueue)
        {
            this.outputQueue = outputQueue;
        }
        
        public void run()
        {
            ArrayList<Object> l = new ArrayList<Object>(MAX_BUFFER);
            
            while (true) {
                try {
                    l.clear();
                    Object item = inputQueue.take();
                    inputQueue.drainTo(l, MAX_BUFFER-1);
                    l.add(0, item);
                    outputQueue.addAll(l);
                } catch (InterruptedException ie) {
                    //
                }
            }
        }
    }
    
    public void putAll(Object item)
    {
        synchronized (outputQueues) {
            for (Iterator<BlockingQueue<Object>> i = outputQueues.iterator(); i.hasNext();) {
                BlockingQueue<Object> queue = i.next();
                queue.offer(item);
            }
        }
    }

    private class WaitForReaders implements Runnable
    {
        public void run()
        {
           int readers = 0;
           
           System.out.println("Waiting for readers...");
           while (true) {
               BlockingQueue<Object> q = null;
               
               synchronized (outputQueues) {
                   while (outputQueues.size() == readers) {
                       try {
                           outputQueues.wait();
                       } catch (InterruptedException ie) {
                           //
                       }
                   }
                   // an output queue was added, so spin up a thread
                   // to write to it
                   q = outputQueues.get(readers++);
               }
               
               ThreadUtil.startDaemonThread(new OutputQueueWriter(q));
               System.out.println("Started queue reader");
           }
        }
    }

    public void start(BlockingQueue<Object> queue)
    {
        synchronized (this) {
            this.inputQueue = queue;
        }
        ThreadUtil.startDaemonThread(new WaitForReaders());
    }
}
