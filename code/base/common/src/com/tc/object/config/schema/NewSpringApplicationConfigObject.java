/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.BaseNewConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.ObjectArrayConfigItem;
import com.tc.config.schema.dynamic.ObjectArrayXPathBasedConfigItem;
import com.terracottatech.configV2.NonDistributedFields;
import com.terracottatech.configV2.SpringAppContext;
import com.terracottatech.configV2.SpringAppContexts;
import com.terracottatech.configV2.SpringApplication;
import com.terracottatech.configV2.SpringApps;
import com.terracottatech.configV2.SpringBean;
import com.terracottatech.configV2.SpringDistributedEvent;

public class NewSpringApplicationConfigObject extends BaseNewConfigObject implements NewSpringApplicationConfig {

  private final ObjectArrayConfigItem springApps;

  public NewSpringApplicationConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(SpringApplication.class);

    this.springApps = new ObjectArrayXPathBasedConfigItem(this.context, ".") {
      protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
        return translateSpringApps(xmlObject);
      }
    };

  }

  public ObjectArrayConfigItem springApps() {
    return this.springApps;
  }

  private static Object translateSpringBeans(XmlObject xmlObject) {
    if (xmlObject == null) return null;

    NonDistributedFields[] springBeans = ((SpringBean) xmlObject).getBeanArray();
    SpringContextBean[] beans = new SpringContextBean[springBeans.length];

    String[] fields;
    String name;

    for (int i = 0; i < springBeans.length; i++) {
      name = springBeans[i].getName();
      fields = springBeans[i].getNonDistributedFieldArray();

      beans[i] = new SpringContextBean(name, fields);
    }

    return beans;
  }

  private static Object translateAppContexts(XmlObject xmlObject) {
    if (xmlObject == null) return null;

    SpringAppContext[] springAppContexts = ((SpringAppContexts) xmlObject).getApplicationContextArray();
    AppContext[] appContexts = new AppContext[springAppContexts.length];

    String[] paths;
    String[] distributedEvents = null;
    SpringDistributedEvent distributedEvent;
    SpringContextBean[] beans;

    for (int i = 0; i < springAppContexts.length; i++) {
      paths = springAppContexts[i].getPaths().getPathArray();
      distributedEvent = springAppContexts[i].getDistributedEvents();
      if (distributedEvent != null) distributedEvents = distributedEvent.getDistributedEventArray();
      beans = (SpringContextBean[]) NewSpringApplicationConfigObject.translateSpringBeans(springAppContexts[i]
          .getBeans());

      appContexts[i] = new AppContext(paths, distributedEvents, beans);
    }

    return appContexts;
  }

  private static Object translateSpringApps(XmlObject xmlObject) {
    if (xmlObject == null) return null;

    SpringApps[] springApps = ((SpringApplication) xmlObject).getJeeApplicationArray();
    SpringApp[] springApp = new SpringApp[springApps.length];

    Lock[] locks;
    InstrumentedClass[] includes;
    AppContext[] appContexts;
    String name;
    String[] transientFields;
    boolean sessionSupport;
    boolean fastProxy;

    for (int i = 0; i < springApps.length; i++) {
      name = springApps[i].getName();
      sessionSupport = springApps[i].getSessionSupport();
      locks = (Lock[]) ConfigTranslationHelper.translateLocks(springApps[i].getLocks());
      includes = (InstrumentedClass[]) ConfigTranslationHelper
          .translateIncludes(springApps[i].getInstrumentedClasses());
      
      transientFields = null;
      if (springApps[i].getTransientFields() != null) {
        transientFields = springApps[i].getTransientFields().getFieldNameArray();
      }
      if (transientFields == null) transientFields = new String[0];
      
      appContexts = (AppContext[]) NewSpringApplicationConfigObject.translateAppContexts(springApps[i]
          .getApplicationContexts());
      fastProxy = springApps[i].getFastProxy();

      springApp[i] = new SpringApp(sessionSupport, locks, includes, appContexts, name, fastProxy, transientFields);
    }

    return springApp;
  }

}
