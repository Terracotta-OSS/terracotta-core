/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
