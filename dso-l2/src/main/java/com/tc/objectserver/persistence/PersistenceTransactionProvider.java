package com.tc.objectserver.persistence;

import org.terracotta.corestorage.StorageManager;

import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.api.TransactionListener;
import com.tc.objectserver.api.TransactionProvider;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @author tim
 */
public class PersistenceTransactionProvider implements TransactionProvider {

    private final StorageManager manager;

    public PersistenceTransactionProvider(StorageManager manager) {
        this.manager = manager;
    }

    @Override
    public Transaction newTransaction() {
        return new StorageTransaction();
    }

    private class StorageTransaction implements Transaction {

        private final Thread t;
        private boolean valid = true;
        private final Collection<TransactionListener> finalizer = new LinkedList<TransactionListener>();

        private StorageTransaction() {
            this.t = Thread.currentThread();
            manager.begin();
        }

        @Override
        public void commit() {
            if ( !valid ) {
                return;
            }
            if (Thread.currentThread() != t) {
                throw new IllegalStateException("Begin and commit threads don't match.");
            }
            manager.commit();
            fireFinalizer(true);
            valid = false;
        }

        @Override
        public void abort() {
            throw new UnsupportedOperationException();
        }
        
        private void fireFinalizer(boolean committed) {
            for ( TransactionListener tLocal : finalizer ) {
                if ( committed ) {
                    tLocal.committed(this);
                } else {
                    tLocal.aborted(this);
                }
            }
        }

        @Override
        public void addTransactionListener(TransactionListener l) {
            finalizer.add(l);
        }
        
        
    }
}
