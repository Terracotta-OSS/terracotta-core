/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.ObjectArrayConfigItem;
import com.tc.config.schema.dynamic.ObjectArrayXPathBasedConfigItem;
import com.tc.util.Assert;
import com.terracottatech.config.ActiveServerGroup;
import com.terracottatech.config.ActiveServerGroups;
import com.terracottatech.config.Members;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.System;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The standard implementation of {@link L2ConfigForL1}.
 */
public class L2ConfigForL1Object implements L2ConfigForL1 {

  private static final String         DEFAULT_HOST = "localhost";

  private final ConfigContext         l2sContext;
  private final ConfigContext         systemContext;

  private final ObjectArrayConfigItem l2Data;
  private final L2Data                defaultL2Data;
  private final Map                   l2DataByName;
  private final Map                   l2DataByGroupId;
  private ObjectArrayConfigItem[]     l2DataByGroup;

  public L2ConfigForL1Object(ConfigContext l2sContext, ConfigContext systemContext) {
    this(l2sContext, systemContext, null);
  }

  public L2ConfigForL1Object(ConfigContext l2sContext, ConfigContext systemContext, int[] dsoPorts) {
    Assert.assertNotNull(l2sContext);
    Assert.assertNotNull(systemContext);

    this.l2sContext = l2sContext;
    this.systemContext = systemContext;

    this.l2sContext.ensureRepositoryProvides(Servers.class);
    this.systemContext.ensureRepositoryProvides(System.class);

    this.l2DataByName = new HashMap();
    this.l2DataByGroupId = new HashMap();

    this.defaultL2Data = new L2Data(DEFAULT_HOST, getL2IntDefault("server/dso-port"));

    this.l2Data = new ObjectArrayXPathBasedConfigItem(this.l2sContext, ".", new L2Data[] { defaultL2Data }) {
      protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
        Server[] l2Array = ((Servers) xmlObject).getServerArray();
        L2Data[] data;

        if (l2Array == null || l2Array.length == 0) {
          data = new L2Data[] { defaultL2Data };
        } else {
          data = new L2Data[l2Array.length];

          for (int i = 0; i < data.length; ++i) {
            Server l2 = l2Array[i];
            String host = l2.getHost();
            String name = l2.getName();

            // if (host == null) host = l2.getName();
            if (host == null) host = defaultL2Data.host();

            int dsoPort = l2.getDsoPort() > 0 ? l2.getDsoPort() : defaultL2Data.dsoPort();

            if (name == null) name = host + ":" + dsoPort;

            data[i] = new L2Data(host, dsoPort);
            l2DataByName.put(name, data[i]);
          }
        }

        organizeByGroup(xmlObject);

        return data;
      }

      private void organizeByGroup(XmlObject xmlObject) {
        ActiveServerGroups asgs = ((Servers) xmlObject).getActiveServerGroups();
        if (asgs == null) {
          ActiveServerGroups groups = ((Servers) xmlObject).addNewActiveServerGroups();
          asgs = groups;
          ActiveServerGroup group = groups.addNewActiveServerGroup();
          Members members = group.addNewMembers();
          for (Iterator iter = l2DataByName.keySet().iterator(); iter.hasNext();) {
            String host = (String) iter.next();
            members.addMember(host);
          }
        }
        ActiveServerGroup[] asgArray = asgs.getActiveServerGroupArray();
        Assert.assertNotNull(asgArray);

        for (int i = 0; i < asgArray.length; i++) {
          String[] members = asgArray[i].getMembers().getMemberArray();
          List groupList = (List) l2DataByGroupId.get(new Integer(i));
          if (groupList == null) {
            groupList = new ArrayList();
            l2DataByGroupId.put(new Integer(i), groupList);
          }
          for (int j = 0; j < members.length; j++) {
            L2Data data = (L2Data) l2DataByName.get(members[j]);
            Assert.assertNotNull(data);
            data.setGroupId(i);
            groupList.add(data);
          }
        }
      }
    };    
  }

  private int getL2IntDefault(String xpath) {
    try {
      return ((XmlInteger) l2sContext.defaultFor(xpath)).getBigIntegerValue().intValue();
    } catch (XmlException xmle) {
      throw Assert.failure("Can't fetch default for " + xpath + "?", xmle);
    }
  }

  public ObjectArrayConfigItem l2Data() {
    return this.l2Data;
  }
  
  public ObjectArrayConfigItem[] getL2DataByGroup() {
    if(l2DataByGroup == null)
      createL2DataByGroup();

    return l2DataByGroup;
  }

  private void createL2DataByGroup() {
    Set keys = this.l2DataByGroupId.keySet();
    Assert.assertTrue(keys.size() > 0);

    this.l2DataByGroup = new ObjectArrayConfigItem[keys.size()];

    int l2DataByGroupPosition = 0;
    for (Iterator iter = keys.iterator(); iter.hasNext();) {
      Integer key = (Integer) iter.next();
      List l2DataList = (List) this.l2DataByGroupId.get(key);
      final L2Data[] l2DataArray = new L2Data[l2DataList.size()];
      int position = 0;
      for (Iterator iterator = l2DataList.iterator(); iterator.hasNext();) {
        L2Data data = (L2Data) iterator.next();
        l2DataArray[position++] = data;
      }
      this.l2DataByGroup[l2DataByGroupPosition] = new ObjectArrayXPathBasedConfigItem(this.l2sContext, ".",
          new L2Data[] { defaultL2Data }) {
        protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
          return l2DataArray;
        }
      };
      l2DataByGroupPosition++;
    }
  }

}
