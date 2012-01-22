/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.commons.lang.ClassUtils;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.ConfigItemListener;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.lang.reflect.Array;

/**
 * A base class for all new config objects.
 */
public class BaseConfigObject implements Config {

  private static final TCLogger logger = TCLogging.getLogger(BaseConfigObject.class);

  protected final ConfigContext context;

  public BaseConfigObject(ConfigContext context) {
    Assert.assertNotNull(context);
    this.context = context;
  }

  private static class IgnoringConfigItemListener implements ConfigItemListener {
    private final ConfigItem item;

    public IgnoringConfigItemListener(ConfigItem item) {
      Assert.assertNotNull(item);
      this.item = item;
    }

    public void valueChanged(Object oldValue, Object newValue) {
      logger.warn("The attempt to change the value of " + item + " from " + oldValue + " to " + newValue
                  + " was ignored; runtime changes in this configuration value are not yet supported.");
    }
  }

  public void changesInItemIgnored(ConfigItem item) {
    Assert.assertNotNull(item);
    item.addListener(new IgnoringConfigItemListener(item));
  }

  private class ForbiddenConfigItemListener implements ConfigItemListener {
    private final ConfigItem item;

    public ForbiddenConfigItemListener(ConfigItem item) {
      Assert.assertNotNull(item);
      this.item = item;
    }

    private boolean isEqual(Object one, Object two) {
      if (one != null && two != null && one.getClass().isArray() && two.getClass().isArray()
          && one.getClass().getComponentType().equals(two.getClass().getComponentType())) {
        if (Array.getLength(one) != Array.getLength(two)) return false;

        for (int i = 0; i < Array.getLength(one); ++i) {
          if (!isEqual(Array.get(one, i), Array.get(two, i))) return false;
        }

        return true;
      } else if (one != null && two != null) {
        return one.equals(two);
      } else return one == two;
    }

    public void valueChanged(Object oldValue, Object newValue) {
      if (oldValue == null) return;
      if (newValue != null && isEqual(oldValue, newValue)) return;

      context.illegalConfigurationChangeHandler().changeFailed(item, oldValue, newValue);
    }
  }

  public void changesInItemForbidden(ConfigItem item) {
    Assert.assertNotNull(item);
    item.addListener(new ForbiddenConfigItemListener(item));
  }

  public String toString() {
    return ClassUtils.getShortClassName(getClass()) + " around bean:\n" + context.bean();
  }

  public XmlObject getBean() {
    return this.context.bean();
  }
  
}
