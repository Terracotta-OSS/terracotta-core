/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import com.tc.object.partitions.PartitionManager;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CoresidentSimpleTestServlet extends HttpServlet {

  private Map              sharedMap0;
  private Map              sharedMap1;
  private static Manager[] managers = ClassProcessorHelper.getPartitionedManagers();

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintWriter out = new PrintWriter(bos);

    try {
      String cmd = req.getParameter("cmd");
      String mapStr = req.getParameter("map");
      if (!"0".equals(mapStr) && !"1".equals(mapStr)) {
        Assert.fail("Request should have the parameter \"map\" with value \"0\" or \"1");
      }
      int mapNum = Integer.parseInt(mapStr);
      String p = req.getParameter("partition");
      if (!"0".equals(p) && !"1".equals(p)) {
        Assert.fail("Request should have the parameter \"partition\" with value \"0\" or \"1");
      }
      int partition = Integer.parseInt(p);

      out.println("DOGET cmd:" + cmd + " partition:" + partition + " map:" + mapStr);
      out.println("ServletClass: " + System.identityHashCode(this.getClass()));
      out.println("Setting partition number to: " + partition);
      PartitionManager.setPartition(managers[partition]);
      Map map = null;
      switch (mapNum) {
        case 0:
          map = sharedMap0;
          break;
        case 1:
          map = sharedMap1;
          break;
      }

      out.println("Using map: " + mapNum + " hashCode: " + System.identityHashCode(map));

      if ("print".equals(cmd)) {
        out.println("partition: "
                    + partition
                    + " sharedMap:"
                    + (map == null ? "NULL" : "map:" + System.identityHashCode(map) + " keys:"
                                              + map.keySet().toString()));
        out.println("OK");
      }
      if ("initialize".equals(cmd)) {
        switch (mapNum) {
          case 0:
            sharedMap0 = new HashMap();
            map = sharedMap0;
            break;
          case 1:
            sharedMap1 = new HashMap();
            map = sharedMap1;
            break;
        }
        out.println("Initialized map hashCode: " + System.identityHashCode(map));
        out.println("OK");
      }
      if ("insert".equals(cmd)) {
        synchronized (map) {
          final long key = System.currentTimeMillis();
          map.put("" + key, "value");
          out.println("Inserted key: " + key);
        }
        out.println("Map Elements: " + map);
        out.println("OK");
      }
      if ("assertSize".equals(cmd)) {
        int size = -1;
        try {
          final String sizePar = req.getParameter("size");
          out.println("SizeParameter: " + (sizePar == null ? "NULL" : sizePar));
          size = Integer.parseInt(sizePar);
        } catch (Exception e) {
          Assert.fail("Request should have a valid \"size\" parameter for \"assertSize\" command");
        }
        out.println("Size: " + map.size() + " Expected: " + size);
        out.println("Map elements: " + map.toString());
        Assert.assertEquals(size, map.size());
        out.println("OK");
      }
    } catch (Exception e) {
      out.println("NOT OK: " + e.toString());
    } finally {
      out.close();
      final String ss = bos.toString();
      System.err.println(ss);
      resp.getWriter().print(ss);
    }
  }
}