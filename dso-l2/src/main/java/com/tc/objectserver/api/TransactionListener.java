/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

/**
 *
 * @author mscott
 */
public interface TransactionListener {
    void committed(Transaction t);
    void aborted(Transaction t);
}
