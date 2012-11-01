/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.objectserver.api.Transaction;

/**
 *
 * @author mscott
 */
public interface TransactionProvider {
    Transaction newTransaction();
}
