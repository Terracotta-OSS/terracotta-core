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
import com.tc.util.ActiveCoordinatorHelper;
import com.tc.util.Assert;
import com.terracottatech.config.Members;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.MirrorGroups;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.System;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The standard implementation of {@link L2ConfigForL1}.
 */
public class L2ConfigForL1Object implements L2ConfigForL1 {

  private static final String         DEFAULT_HOST     = "localhost";

  private final ConfigContext         l2sContext;
  private final ConfigContext         systemContext;

  private final ObjectArrayConfigItem l2Data;
  private final L2Data                defaultL2Data;
  private final Map                   l2DataByName;
  private final Map                   l2DataByGroupId;
  private ObjectArrayConfigItem[]     l2DataByGroup;
  private int                         coordinatorGrpId = -1;

  public L2ConfigForL1Object(final ConfigContext l2sContext, final ConfigContext systemContext) {
    this(l2sContext, systemContext, null);
  }

  public L2ConfigForL1Object(final ConfigContext l2sContext, final ConfigContext systemContext, final int[] dsoPorts) {
    Assert.assertNotNull(l2sContext);
    Assert.assertNotNull(systemContext);

    this.l2sContext = l2sContext;
    this.systemContext = systemContext;

    this.l2sContext.ensureRepositoryProvides(Servers.class);
    this.systemContext.ensureRepositoryProvides(System.class);

    this.l2DataByName = new HashMap();
    this.l2DataByGroupId = new LinkedHashMap();

    this.defaultL2Data = new L2Data(DEFAULT_HOST, getL2IntDefault("server/dso-port"));

    this.l2Data = new ObjectArrayXPathBasedConfigItem(this.l2sContext, ".", new L2Data[] { this.defaultL2Data }) {
      @Override
      protected Object fetchDataFromXmlObject(final XmlObject xmlObject) {
        Server[] l2Array = ((Servers) xmlObject).getServerArray();
        L2Data[] data;

        if (l2Array == null || l2Array.length == 0) {
          data = new L2Data[] { L2ConfigForL1Object.this.defaultL2Data };
        } else {
          data = new L2Data[l2Array.length];

          for (int i = 0; i < data.length; ++i) {
            Server l2 = l2Array[i];
            String host = l2.getHost();
            String name = l2.getName();

            // if (host == null) host = l2.getName();
            if (host == null) {
              host = L2ConfigForL1Object.this.defaultL2Data.host();
            }

            int dsoPort = l2.getDsoPort() > 0 ? l2.getDsoPort() : L2ConfigForL1Object.this.defaultL2Data.dsoPort();

            if (name == null) {
              name = host + ":" + dsoPort;
            }

            data[i] = new L2Data(host, dsoPort);
            L2ConfigForL1Object.this.l2DataByName.put(name, data[i]);
          }
        }

        organizeByGroup(xmlObject);

        return data;
      }

      private void organizeByGroup(final XmlObject xmlObject) {
        MirrorGroups asgs = ((Servers) xmlObject).getMirrorGroups();
        if (asgs == null) {
          asgs = ((Servers) xmlObject).addNewMirrorGroups();
        }
        MirrorGroup[] asgArray = asgs.getMirrorGroupArray();
        if (asgArray == null || asgArray.length == 0) {
          MirrorGroup group = asgs.addNewMirrorGroup();
          Members members = group.addNewMembers();
          for (Iterator iter = L2ConfigForL1Object.this.l2DataByName.keySet().iterator(); iter.hasNext();) {
            String host = (String) iter.next();
            members.addMember(host);
          }
          asgArray = asgs.getMirrorGroupArray();
        }
        Assert.assertNotNull(asgArray);
        Assert.assertTrue(asgArray.length >= 1);

        for (int i = 0; i < asgArray.length; i++) {
          String[] members = asgArray[i].getMembers().getMemberArray();
          List groupList = (List) L2ConfigForL1Object.this.l2DataByGroupId.get(new Integer(i));
          if (groupList == null) {
            groupList = new ArrayList();
            L2ConfigForL1Object.this.l2DataByGroupId.put(new Integer(i), groupList);
          }
          for (String member : members) {
            L2Data data = (L2Data) L2ConfigForL1Object.this.l2DataByName.get(member);
            if (data == null) { throw new RuntimeException(
                                                           "The member \""
                                                               + member
                                                               + "\" is not persent in the server section. Please verify the configuration."); }
            Assert.assertNotNull(data);
            data.setGroupId(i);
            String groupName = asgArray[i].getGroupName();
            if (groupName == null) {
              groupName = ActiveCoordinatorHelper.getGroupNameFrom(asgArray[i].getMembers().getMemberArray());
            }

            data.setGroupName(groupName);
            groupList.add(data);
          }
        }

        L2ConfigForL1Object.this.coordinatorGrpId = ActiveCoordinatorHelper.getCoordinatorGroup(asgArray);
      }
    };
  }

  private int getL2IntDefault(final String xpath) {
    try {
      return ((XmlInteger) this.l2sContext.defaultFor(xpath)).getBigIntegerValue().intValue();
    } catch (XmlException xmle) {
      throw Assert.failure("Can't fetch default for " + xpath + "?", xmle);
    }
  }

  public ObjectArrayConfigItem l2Data() {
    return this.l2Data;
  }

  public synchronized ObjectArrayConfigItem[] getL2DataByGroup() {
    if (this.l2DataByGroup == null) {
      createL2DataByGroup();
    }

    Assert.assertNoNullElements(this.l2DataByGroup);
    return this.l2DataByGroup;
  }

  private void createL2DataByGroup() {
    Set keys = this.l2DataByGroupId.keySet();
    Assert.assertTrue(keys.size() > 0);

    this.l2DataByGroup = new ObjectArrayConfigItem[keys.size()];

    int l2DataByGroupPosition = 0;
    boolean isCoordinatorSet = false;
    for (Iterator iter = keys.iterator(); iter.hasNext();) {
      Integer key = (Integer) iter.next();
      List l2DataList = (List) this.l2DataByGroupId.get(key);
      final L2Data[] l2DataArray = new L2Data[l2DataList.size()];
      int position = 0;
      for (Iterator iterator = l2DataList.iterator(); iterator.hasNext();) {
        L2Data data = (L2Data) iterator.next();
        l2DataArray[position++] = data;
      }
      if ((l2DataByGroupPosition != this.coordinatorGrpId) || isCoordinatorSet) {
        setL2DataInGrp(l2DataByGroupPosition + 1, l2DataArray);
        l2DataByGroupPosition++;
      } else {
        setL2DataInGrp(0, l2DataArray);
        isCoordinatorSet = true;
      }
    }
  }

  private void setL2DataInGrp(final int l2DataByGroupPosition, final L2Data[] l2DataArray) {
    this.l2DataByGroup[l2DataByGroupPosition] = new ObjectArrayXPathBasedConfigItem(this.l2sContext, ".",
        new L2Data[] { this.defaultL2Data }) {
      @Override
      protected Object fetchDataFromXmlObject(final XmlObject xmlObject) {
        return l2DataArray;
      }
    };
  }

}
