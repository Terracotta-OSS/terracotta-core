/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

import org.apache.commons.lang.SerializationUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class OrganicObjectGraphManager {

  private static final String   BASE_DIR       = "tests.base";
  private static final String   CLASS_TYPE     = "OrganicObjectGraphNode";
  private static final String[] TYPE_NAMES     = new String[] { "int", "String", "short", "double" };
  private static Class[]        cachedNodeTypes;
  private static final int      BOUND          = 50;
  private static Random         random         = new Random(0);
  private static StringBuffer   buffer;
  private static int            sequenceNumber = -1;

  public static void main(String[] args) throws Exception {
    if (args[0] == null) {
      System.err.println("Please supply the number of node variations as the first command line argument");
      return;
    }
    int variations = Integer.parseInt(args[0]);
    int id = 0;
    for (int i = 0; i < variations; i++) {
      int size = 0;
      do {
        size = getRandom(BOUND);
      } while (size == 0);
      int[] types = new int[size];
      for (int j = 0; j < size; j++) {
        types[j] = getRandom(TYPE_NAMES.length);
      }
      generateGraphTypes(types, id++);
    }
  }

  public static void serializeGraph(OrganicObjectGraph graph, File file) throws FileNotFoundException {
    SerializationUtils.serialize(graph, new FileOutputStream(file));
  }

  public static OrganicObjectGraph deserializeGraph(File file) throws FileNotFoundException {
    return (OrganicObjectGraph) SerializationUtils.deserialize(new FileInputStream(file));
  }

  public static synchronized OrganicObjectGraph createOrganicGraph(int elements, String envKey) throws Exception {
    Class[] nodeTypes = getNodeTypes();
    int size = nodeTypes.length;
    OrganicObjectGraph[] leafNodes = new OrganicObjectGraph[elements];
    Constructor constructor = nodeTypes[getRandom(size)].getConstructor(new Class[] { Integer.TYPE, String.class });
    Object[] args = new Object[] { new Integer(nextSequenceNumber()), envKey };
    OrganicObjectGraph rootNode = (OrganicObjectGraph) constructor.newInstance(args);
    leafNodes[0] = rootNode;
    int boundMarker = 1;
    OrganicObjectGraph currentNode;
    OrganicObjectGraph leafNode;
    while (boundMarker < leafNodes.length) {
      currentNode = (OrganicObjectGraph) nodeTypes[getRandom(size)].newInstance();
      leafNode = leafNodes[getRandom(boundMarker)];
      currentNode.setParent(leafNode);
      leafNode.addReference(currentNode);
      leafNodes[boundMarker++] = currentNode;
    }
    return rootNode;
  }

  /**
   * @NOTTHREADSAFE this method must be called by a process other than the one which created the graphs in the fist
   *                place
   */
  public static boolean validate(OrganicObjectGraph[] graphs, int size, int mutations) throws Exception {
    random = new Random(0);
    OrganicObjectGraph[] controlGraphs = new OrganicObjectGraph[graphs.length];
    for (int i = 0; i < graphs.length; i++) {
      System.out.println("verify: " + graphs[i].envKey() + " #" + graphs[i].sequenceNumber());
      controlGraphs[i] = OrganicObjectGraphManager.createOrganicGraph(size, null);
      for (int j = 0; j < graphs[i].changeIterationCount(); j++) {
        controlGraphs[i].mutateRandom(mutations);
      }
      if (!controlGraphs[i].equals(graphs[i])) return false;
      System.out.println("  graph[" + i + "] passed");
    }
    return true;
  }

  private static int nextSequenceNumber() {
    return ++sequenceNumber;
  }
  
  private static Class[] getNodeTypes() {
    List types = new LinkedList();
    int count = 0;
    if (cachedNodeTypes != null) {
      return cachedNodeTypes;
    }
    String thisClassName = OrganicObjectGraph.class.getName();
    String[] parts = thisClassName.split("\\.");
    String thisClassSimpleName = parts[parts.length - 1];
    String packages = thisClassName.substring(0, thisClassName.length() - thisClassSimpleName.length());
    try {
      while (true) {
        types.add(Class.forName(packages + CLASS_TYPE + "_" + count++));
      }
    } catch (ClassNotFoundException e) {
      // HACK!
    }
    cachedNodeTypes = (Class[]) types.toArray(new Class[0]);
    return cachedNodeTypes;
  }

  private static void generateGraphTypes(int[] types, int id) {
    String className = CLASS_TYPE + "_" + id;
    out("package com.tctest.performance.sampledata;");
    out("");
    out("public final class " + className + " extends OrganicObjectGraph {");
    out("");
    out("  private int size = " + types.length + ";");
    out("  private int[] types = new int[] { " + printTypesArray(types) + " };");
    out("");
    for (int i = 0; i < types.length; i++) {
      out("  private " + TYPE_NAMES[types[i]] + " f" + i + ";");
    }
    out("");
    out("  public " + className + "(int sequenceNumber, String envKey) {");
    out("    super(sequenceNumber, envKey);");
    out("  }");
    out("");
    out("  public " + className + "() {");
    out("    super();");
    out("  }");
    out("");
    out("  protected int getSize() {");
    out("    return size;");
    out("  }");
    out("");
    out("  protected int getType(int index) {");
    out("    return types[index];");
    out("  }");
    out("");
    printSetValueMethods(types);
    printEqualsMethod(types, className);
    out("}");
    writeFile(className);
  }

  private static void printSetValueMethods(int[] types) {
    boolean[] setValueCreated = new boolean[TYPE_NAMES.length];
    for (int i = 0; i < types.length; i++) {
      int type = types[i];
      if (!setValueCreated[type]) {
        setValueCreated[type] = true;
        out("  protected void setValue(int index, " + TYPE_NAMES[type] + " value) {");
        out("    switch (index) {");
        for (int j = 0; j < types.length; j++) {
          if (type == types[j]) {
            out("      case " + j + ":");
            out("        f" + j + " = value;");
          }
        }
        out("      default:");
        out("        break;");
        out("    }");
        out("  }");
        out("");
      }
    }
  }

  private static void printEqualsMethod(int[] types, String className) {
    String fieldName;
    out("  public boolean equals(Object rawObj) {");
    out("    if (!(rawObj instanceof " + className
        + ")) { System.out.println(\"not instanceof\"); System.out.println(rawObj.getClass().getName() + \"="
        + className + "\"); return false; }");
    out("    " + className + " obj = (" + className + ") rawObj;");
    for (int i = 0; i < types.length; i++) {
      fieldName = "f" + i;
      // out("System.out.println(" + fieldName + " + \"=\" + obj." + fieldName + ");");
      if (TYPE_NAMES[types[i]].equals("String")) {
        out("    if (!(\"\" + " + fieldName + ").equals(\"\" + obj." + fieldName + ")) return false;");
      } else {
        out("    if (" + fieldName + " != obj." + fieldName + ") return false;");
      }
    }
    out("    return super.equals(obj);");
    out("  }");
  }

  private static String printTypesArray(int[] types) {
    String out = String.valueOf(types[0]);
    for (int i = 1; i < types.length; i++) {
      out += ", " + types[i];
    }
    return out;
  }

  private static void out(String str) {
    if (buffer == null) buffer = new StringBuffer();
    buffer.append(str + "\n");
  }

  private static void writeFile(String name) {
    String[] pathParts = OrganicObjectGraphManager.class.getName().split("\\.");
    String path = "";
    for (int i = 0; i < pathParts.length - 1; i++) {
      path += pathParts[i] + File.separator;
    }
    File file = new File(BASE_DIR + File.separator + path + name + ".java");
    try {
      DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
      out.writeBytes(buffer.toString());
      out.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    buffer = null;
  }

  static int sequence;

  private static int getRandom(int bound) {
    return new Long(Math.round(Math.floor(bound * random.nextDouble()))).intValue();
  }
}
