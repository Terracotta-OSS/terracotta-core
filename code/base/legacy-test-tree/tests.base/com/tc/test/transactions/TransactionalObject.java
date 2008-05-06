/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.transactions;

import com.tc.util.TCAssertionError;

/**
 * Implementations of this class represent a transactional object: something you can write and read transactionally. If
 * you:
 * </p>
 * <ul>
 * <li>Call {@link startWrite(Object)} just before you begin a transaction that writes to the object (or do the write,
 * if transactions are automatic);</li>
 * <li>Call {@link endWrite(Context)} just after you commit the transaction containing the write (or the write itself,
 * if transactions are automatic), and pass in the {@link Context} that {@link startWrite(Object)} gave you;</li>
 * <li>Call {@link startRead()} just before you begin a transaction that reads from the object (or do the read, if
 * transactions are automatic); and</li>
 * <li>Call {@link endRead(Context, Object)} just after you complete the transaction containing the read (or the read
 * itself, if transactions are automatic), and pass in the {@link Context} that {@link startRead()} gave you;</li>
 * </ul>
 * <p>
 * then, automatically, this class will check the consistency of your application. In other words, it will tell you if
 * the value you read is something that couldn't <em>possibly</em> have been there, were the underlying store behaving
 * truly transactionally.
 * </p>
 * <p>
 * Implementations fail by throwing a {@link TCAssertionError}; that should be sufficient for its current uses in
 * tests. If you need to improve it, by all means, go right ahead &mdash; but be careful, because the logic in the
 * standard implementation is really quite tricky.
 */
public interface TransactionalObject {

  public static interface Context {
    // Nothing here.
  }

  Context startWrite(Object value);

  Context startWrite(Object value, long now);

  void endWrite(Context rawWrite);

  void endWrite(Context rawWrite, long now);

  Context startRead();

  Context startRead(long now);

  void endRead(Context rawRead, Object result);

  void endRead(Context rawRead, Object result, long now);

}