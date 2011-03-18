/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.net.TCSocketAddress;
import com.tc.util.ActiveCoordinatorHelper;
import com.tc.util.Assert;
import com.terracottatech.config.MirrorGroup;
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

  private final ConfigContext l2sContext;
  private final ConfigContext systemContext;

  private final L2Data[]      l2sData;
  private final Map           l2DataByName;
  private final Map           l2DataByGroupId;
  private L2Data[][]          l2DataByGroup;

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

    Servers servers = (Servers) this.l2sContext.bean();
    Server[] l2Array = servers.getServerArray();
    this.l2sData = new L2Data[l2Array.length];
    for (int i = 0; i < l2Array.length; i++) {
      Server l2 = l2Array[i];
      String host = l2.getDsoPort().getBind();
      if (TCSocketAddress.WILDCARD_IP.equals(host)) {
        host = l2.getHost();
      }
      String name = l2.getName();
      this.l2sData[i] = new L2Data(host, l2.getDsoPort().getIntValue());
      this.l2DataByName.put(name, this.l2sData[i]);
    }
    organizeByGroup(servers);
  }

  private void organizeByGroup(Servers servers) {
    MirrorGroup[] asgArray = servers.getMirrorGroups().getMirrorGroupArray();
    Assert.assertNotNull(asgArray);
    Assert.assertTrue(asgArray.length >= 1);

    asgArray = ActiveCoordinatorHelper.generateGroupNames(asgArray);

    for (int i = 0; i < asgArray.length; i++) {
      String[] members = asgArray[i].getMembers().getMemberArray();
      List groupList = (List) this.l2DataByGroupId.get(Integer.valueOf(i));
      if (groupList == null) {
        groupList = new ArrayList();
        this.l2DataByGroupId.put(Integer.valueOf(i), groupList);
      }
      for (String member : members) {
        L2Data data = (L2Data) this.l2DataByName.get(member);
        if (data == null) { throw new RuntimeException(
                                                       "The member \""
                                                           + member
                                                           + "\" is not persent in the server section. Please verify the configuration."); }
        Assert.assertNotNull(data);
        String groupName = asgArray[i].getGroupName();
        data.setGroupName(groupName);
        groupList.add(data);
      }
    }
  }

  public L2Data[] l2Data() {
    return this.l2sData;
  }

  public synchronized L2Data[][] getL2DataByGroup() {
    if (this.l2DataByGroup == null) {
      createL2DataByGroup();
    }

    Assert.assertNoNullElements(this.l2DataByGroup);
    return this.l2DataByGroup;
  }

  private void createL2DataByGroup() {
    Set keys = this.l2DataByGroupId.keySet();
    Assert.assertTrue(keys.size() > 0);

    this.l2DataByGroup = new L2Data[keys.size()][];

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
      this.l2DataByGroup[l2DataByGroupPosition] = l2DataArray;
      l2DataByGroupPosition++;
    }
  }
}
