/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.repository;

import org.apache.commons.lang.ClassUtils;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import com.tc.config.schema.listen.ConfigurationChangeListener;
import com.tc.config.schema.listen.ConfigurationChangeListenerSet;
import com.tc.config.schema.validate.ConfigurationValidator;
import com.tc.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The standard implementation of {@link MutableBeanRepository}.
 */
public class StandardBeanRepository implements MutableBeanRepository {

  private final Class                          requiredClass;
  private final ConfigurationChangeListenerSet listenerSet;
  private final Set                            validators;
  private XmlObject                            bean;

  private XmlObject                            preMutateCopy;

  public StandardBeanRepository(Class requiredClass) {
    Assert.assertNotNull(requiredClass);

    this.requiredClass = requiredClass;
    this.listenerSet = new ConfigurationChangeListenerSet();
    this.validators = new HashSet();
    this.bean = null;
  }

  public void ensureBeanIsOfClass(Class theClass) {
    if (!theClass.isAssignableFrom(this.requiredClass)) {
      // formatting
      throw Assert.failure("You're making sure this repository requires at least " + theClass + ", but it requires "
                           + this.requiredClass + ", which isn't that class or a subclass thereof.");
    }
  }

  public void saveCopyOfBeanInAnticipationOfFutureMutation() {
    Assert.eval(this.preMutateCopy == null);
    this.preMutateCopy = this.bean.copy();
  }

  public void didMutateBean() {
    Assert.eval(this.preMutateCopy != null);
    this.listenerSet.configurationChanged(this.preMutateCopy, this.bean);
    this.preMutateCopy = null;
  }

  public synchronized XmlObject bean() {
    return this.bean;
  }

  static SchemaType getTypeFieldFrom(Class theClass) {
    try {
      Field typeField = theClass.getField("type");

      Assert.eval(typeField.getType().equals(SchemaType.class));

      int modifiers = typeField.getModifiers();
      Assert.eval(Modifier.isPublic(modifiers));
      Assert.eval(Modifier.isStatic(modifiers));
      Assert.eval(Modifier.isFinal(modifiers));

      return (SchemaType) typeField.get(null);
    } catch (NoSuchFieldException nsfe) {
      throw Assert.failure("Class " + theClass.getName()
                           + ", doesn't have a 'public static final SchemaType type' field?", nsfe);
    } catch (IllegalArgumentException iae) {
      throw Assert.failure("Unable to get 'public static final SchemaType type' from class " + theClass.getName(), iae);
    } catch (IllegalAccessException iae) {
      throw Assert.failure("Unable to get 'public static final SchemaType type' from class " + theClass.getName(), iae);
    }
  }

  public SchemaType rootBeanSchemaType() {
    return getTypeFieldFrom(this.requiredClass);
  }

  public synchronized void setBean(XmlObject bean, String sourceDescription) throws XmlException {
    Assert.assertNotBlank(sourceDescription);
    Assert.eval(bean == null || this.requiredClass.isInstance(bean));

    if (this.bean == bean) return;

    if (bean != null) {
      throwExceptionIfSchemaValidationFails(bean, sourceDescription);

      Iterator iter = this.validators.iterator();
      while (iter.hasNext()) {
        ((ConfigurationValidator) iter.next()).validate(bean);
      }
    }

    XmlObject oldBean = this.bean;
    this.bean = bean;
    this.listenerSet.configurationChanged(oldBean, bean);
  }

  private void throwExceptionIfSchemaValidationFails(XmlObject theBean, String sourceDescription) throws XmlException {
    List errors = new ArrayList();
    XmlOptions options = new XmlOptions();
    options = options.setLoadLineNumbers();
    options = options.setErrorListener(errors);
    options = options.setDocumentSourceName(sourceDescription);

    boolean validated = theBean.validate(options);

    if (errors.size() > 0 || (!validated)) {
      StringBuffer descrip = new StringBuffer();

      descrip.append("The configuration from '" + sourceDescription + "' is invalid; it has " + errors.size()
                     + " error" + (errors.size() == 1 ? "" : "s") + ":\n");

      int pos = 1;
      Iterator iter = errors.iterator();
      while (iter.hasNext()) {
        descrip.append("   " + pos + ": " + iter.next().toString() + "\n");
        pos++;
      }

      throw new XmlException(descrip.toString());
    }
  }

  public void addListener(ConfigurationChangeListener listener) {
    Assert.assertNotNull(listener);
    this.listenerSet.addListener(listener);
  }

  public void addValidator(ConfigurationValidator validator) {
    Assert.assertNotNull(validator);
    this.validators.add(validator);
  }

  public String toString() {
    return "<Repository for bean of class " + ClassUtils.getShortClassName(this.requiredClass) + "; have bean? "
           + (this.bean != null) + ">";
  }

}
