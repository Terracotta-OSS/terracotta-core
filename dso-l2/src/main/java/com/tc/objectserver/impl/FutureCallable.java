/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author mscott
 */
public class FutureCallable<T> implements Future<T> {
    
    private final Callable<T> target;
    private final Future<T> future;
    
    public FutureCallable(ExecutorService service, Callable<T> call) {
        target = call;
        future = service.submit(call);
    }
    
    @Override
    public boolean cancel(boolean interrupt) {        
        if ( target instanceof CanCancel ) {
            ((CanCancel)target).cancel();
        }
        return future.cancel(interrupt);
    }
    
    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public T get(long l, TimeUnit tu) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(l, tu);
    }
}
