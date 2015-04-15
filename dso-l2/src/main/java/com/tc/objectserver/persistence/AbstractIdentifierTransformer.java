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
