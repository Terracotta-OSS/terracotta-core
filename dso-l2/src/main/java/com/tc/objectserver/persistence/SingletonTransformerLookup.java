package com.tc.objectserver.persistence;

import org.terracotta.corestorage.AnonymousTransformerLookup;
import org.terracotta.corestorage.Transformer;

/**
 * @author tim
 */
public class SingletonTransformerLookup extends AnonymousTransformerLookup {
  private final Class<?> targetClass;
  private final Transformer<?, ?> transformer;

  public SingletonTransformerLookup(final Class<?> targetClass, final Transformer<?, ?> transformer) {
    this.targetClass = targetClass;
    this.transformer = transformer;
  }

  @Override
  public <T> Transformer<? super T, ?> lookup(final Class<T> klazz) {
    if (targetClass == klazz) {
      return (Transformer<? super T, ?>) transformer;
    } else {
      return null;
    }
  }
}
