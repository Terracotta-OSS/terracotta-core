package com.tc.objectserver.persistence;

import com.tc.util.AbstractIdentifier;

import org.terracotta.corestorage.Transformer;

/**
 * @author tim
 */
public abstract class AbstractIdentifierTransformer<K extends AbstractIdentifier> implements Transformer<K, Long> {
  private final Class<K> c;

  protected AbstractIdentifierTransformer(final Class<K> c) {
    this.c = c;
  }

  protected abstract K createIdentifier(long id);

  @Override
  public K recover(final Long value) {
    return createIdentifier(value);
  }

  @Override
  public Long transform(final K k) {
    return k.toLong();
  }

  @Override
  public boolean equals(final K left, final Long right) {
    if (c.isInstance(left)) {
      return left.toLong() == right.longValue();
    } else {
      return false;
    }
  }

  @Override
  public Class<Long> getTargetClass() {
    return Long.class;
  }
}
