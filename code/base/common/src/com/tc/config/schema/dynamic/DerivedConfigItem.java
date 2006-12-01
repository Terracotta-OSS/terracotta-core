/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import com.tc.util.Assert;
import com.tc.util.stringification.OurStringBuilder;

/**
 * A {@link ConfigItem} whose value is derived from one or more other {@link ConfigItem}s.
 */
public abstract class DerivedConfigItem implements ConfigItem, ConfigItemListener {

  private final ConfigItem[]               derivedFrom;
  private final CompoundConfigItemListener listener;

  private Object                           currentValue;

  public DerivedConfigItem(ConfigItem[] derivedFrom) {
    Assert.assertNoNullElements(derivedFrom);
    this.derivedFrom = derivedFrom;

    for (int i = 0; i < derivedFrom.length; ++i)
      derivedFrom[i].addListener(this);

    this.listener = new CompoundConfigItemListener();
    this.currentValue = createValueFrom(this.derivedFrom);
  }

  public Object getObject() {
    return currentValue;
  }

  protected abstract Object createValueFrom(ConfigItem[] fromWhich);

  public void addListener(ConfigItemListener changeListener) {
    this.listener.addListener(changeListener);
  }

  public void removeListener(ConfigItemListener changeListener) {
    this.listener.removeListener(changeListener);
  }

  public void valueChanged(Object oldValue, Object newValue) {
    Object ourOldValue = currentValue;
    this.currentValue = createValueFrom(this.derivedFrom);

    if (((ourOldValue == null) != (this.currentValue == null))
        || ((ourOldValue != null) && (!ourOldValue.equals(this.currentValue)))) {
      this.listener.valueChanged(ourOldValue, this.currentValue);
    }
  }

  public String toString() {
    return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).append("derived from", this.derivedFrom)
        .toString();
  }

}
