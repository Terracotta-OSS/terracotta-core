/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author mscott
 */
public class GroupFuture<T> implements Future<T> {
    
    private final Collection<Future<T>>  list;

    public GroupFuture(Collection<Future<T>> list) {
        this.list = list;
    }

    @Override
    public boolean cancel(boolean bln) {
        for ( Future<T> f : list ) {
            f.cancel(bln);
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        for ( Future<T> f : list ) {
            if ( !f.isCancelled() ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isDone() {
        for ( Future<T> f : list ) {
            if ( !f.isDone() ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        T val = null;
        for ( Future<T> f : list ) {
            T check = f.get();
            if ( val == null ) {
                val = check;
            } else  if ( check != null && !val.equals(check) ) {
                throw new ExecutionException(new AssertionError(val + " != " + check));
            }
        }
        return val;
    }

    @Override
  public T get(long l, TimeUnit tu) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
