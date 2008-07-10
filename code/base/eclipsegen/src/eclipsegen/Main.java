package eclipsegen;

import org.apache.commons.io.FileUtils;
import org.jvyaml.YAML;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Main {

  // TODO: take modules.def.yml location as command line arg

  private final String modulesFile;

  public Main(String modulesFile) {
    Util.ensureFile(modulesFile);

    this.modulesFile = modulesFile;
  }

  public static void main(String[] args) throws Exception {
    System.out.println("Running eclipsegen...");
    new Main(resolveModulesDef()).generate();
    System.out.println("Done!");
  }

  private void generate() throws Exception {
    System.err.println("reading " + modulesFile);

    File base = new File(modulesFile).getParentFile();

    Module[] modules = loadModules();

    writeClasspathFiles(base, modules);

    writeSettings(base, modules);
  }

  @SuppressWarnings("unchecked")
  private Module[] loadModules() throws FileNotFoundException {
    Map ymlData = (Map) YAML.load(new FileReader(modulesFile));
    return ModulesDefReader.readFrom(ymlData);
  }

  private void writeSettings(File base, Module[] modules) throws IOException {
    File source = new File(new File("."), ".settings");
    Util.ensureDir(source);

    Properties jdtCorePrefs = readProps(new File(source, "org.eclipse.jdt.core.prefs"));
    Properties jdtUiPrefs = readProps(new File(source, "org.eclipse.jdt.ui.prefs"));

    for (int i = 0; i < modules.length; i++) {
      Module module = modules[i];
      writeModuleSettings(base, module, jdtCorePrefs, jdtUiPrefs);
    }
  }

  private void writeModuleSettings(File base, Module module, Properties jdtCorePrefs, Properties jdtUiPrefs)
      throws IOException {
    File modDir = new File(base, module.getName());

    File modSettings = new File(modDir, ".settings");
    if (!modSettings.exists()) {
      boolean created = modSettings.mkdirs();
      if (!created) { throw new IOException("cannot create dir " + modSettings); }
    }

    File modSettingsTc = new File(modSettings, ".tc");

    File coreOverride = new File(modSettingsTc, "org.eclipse.jdt.core.prefs");
    File uiOverride = new File(modSettingsTc, "org.eclipse.jdt.ui.prefs");

    if (coreOverride.exists()) {
      FileUtils.copyFileToDirectory(coreOverride, modSettings);
    } else {
      writeJdkSpecific(module, modSettings, jdtCorePrefs);
    }

    if (uiOverride.exists()) {
      FileUtils.copyFileToDirectory(uiOverride, modSettings);
    } else {
      jdtUiPrefs.store(new FileOutputStream(new File(modSettings, "org.eclipse.jdt.ui.prefs")), "");
    }

  }

  private void writeJdkSpecific(Module module, File modSettings, Properties jdtCorePrefs) throws IOException {
    Properties props = (Properties) jdtCorePrefs.clone();
    for (Iterator<String> i = jdkConvertKeys.iterator(); i.hasNext();) {
      props.setProperty(i.next(), module.getJdk().getRaw());
    }
    props.store(new FileOutputStream(new File(modSettings, "org.eclipse.jdt.core.prefs")), "");
  }

  private Properties readProps(File file) throws IOException {
    Util.ensureFile(file);

    SortedProperties props = new SortedProperties();
    props.load(new FileInputStream(file));

    return props;
  }

  private void writeClasspathFiles(File base, Module[] modules) throws Exception {
    for (int i = 0; i < modules.length; i++) {
      Module module = modules[i];

      File modDir = new File(base, module.getName());
      Util.ensureDir(modDir);

      String[] src = findSourceFolders(modDir);
      String[] jars = getIvyDependencies(modDir);

      String[] moduleJars = findModuleJars(modDir);

      writeDotClassPath(modDir, module, src, jars, moduleJars);
    }

  }

  private String[] findModuleJars(File modDir) {
    TreeSet<String> rv = new TreeSet<String>();
    File[] listFiles = modDir.listFiles();
    for (int i = 0; i < listFiles.length; i++) {
      File file = listFiles[i];
      if (file.isDirectory() && libFolders.contains(file.getName())) {
        rv.addAll(getJarsFromDir(file));
      }
    }

    return rv.toArray(new String[rv.size()]);
  }

  private Collection<String> getJarsFromDir(File libDir) {
    List<String> rv = new ArrayList<String>();
    File[] listFiles = libDir.listFiles();
    for (int i = 0; i < listFiles.length; i++) {
      File file = listFiles[i];
      if (file.getName().endsWith(".jar")) {
        rv.add(libDir.getName() + "/" + file.getName());
      }
    }

    return rv;
  }

  private void writeDotClassPath(File modDir, Module module, String[] src, String[] jars, String[] moduleJars)
      throws IOException {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    ps.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    ps.println("<classpath>");

    // source folders and resources
    for (int i = 0; i < src.length; i++) {
      String path = src[i];
      String output = path;
      if (output.endsWith(".resources")) {
        output = output.substring(0, output.lastIndexOf(".resources"));
      }

      ps.println("\t<classpathentry excluding=\"**/.svn/*\" kind=\"src\" output=\"build.eclipse/" + output
                 + ".classes\" path=\"" + path + "\"/>");
    }

    // JDK
    ps
        .println("\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/"
                 + module.getJdk().getEclipse() + "\"/>");

    // jars
    for (int i = 0; i < jars.length; i++) {
      String jar = jars[i];
      ps.println("\t<classpathentry exported=\"true\" kind=\"lib\" path=\"/dependencies/lib/" + jar + "\"/>");

    }

    // modules dependencies
    for (Iterator<String> i = module.getDependencies().iterator(); i.hasNext();) {
      ps.println("\t<classpathentry combineaccessrules=\"false\" kind=\"src\" path=\"/" + i.next() + "\"/>");
    }

    // extra module jars
    for (int i = 0; i < moduleJars.length; i++) {
      String jar = moduleJars[i];
      ps.println("\t<classpathentry exported=\"true\" kind=\"lib\" path=\"" + jar + "\"/>");

    }

    // ps.println("\t<classpathentry kind=\"output\" path=\"bin\"/>");

    ps.println("</classpath>");
    ps.close();

    FileOutputStream fos = new FileOutputStream(new File(modDir, ".classpath"));
    fos.write(baos.toByteArray());
    fos.close();
  }

  private static final Set<String> sourceFolders  = new HashSet<String>();
  private static final Set<String> libFolders     = new HashSet<String>();
  private static final Set<String> jdkConvertKeys = new HashSet<String>();

  static {
    jdkConvertKeys.add("org.eclipse.jdt.core.compiler.codegen.targetPlatform");
    jdkConvertKeys.add("org.eclipse.jdt.core.compiler.compliance");
    jdkConvertKeys.add("org.eclipse.jdt.core.compiler.source");

    sourceFolders.add("src");
    sourceFolders.add("tests.base");
    sourceFolders.add("tests.system");
    sourceFolders.add("tests.unit");

    libFolders.add("lib");
    for (Iterator<String> iter = sourceFolders.iterator(); iter.hasNext();) {
      String src = iter.next();
      if (!"src".equals(src)) {
        libFolders.add("lib." + src);
      }
    }

    Collection<String> res = new ArrayList<String>();
    for (Iterator<String> iter = sourceFolders.iterator(); iter.hasNext();) {
      res.add(iter.next() + ".resources");
    }
    sourceFolders.addAll(res);

  }

  private String[] findSourceFolders(File modDir) {
    ArrayList<String> rv = new ArrayList<String>();

    File[] listFiles = modDir.listFiles();
    for (int i = 0; i < listFiles.length; i++) {
      File file = listFiles[i];
      if (file.isDirectory() && sourceFolders.contains(file.getName())) {
        rv.add(file.getName());
      }
    }

    return rv.toArray(new String[rv.size()]);
  }

  private String[] getIvyDependencies(File modDir) throws Exception {
    TreeSet<String> rv = new TreeSet<String>();

    File[] listFiles = modDir.listFiles();
    for (int i = 0; i < listFiles.length; i++) {
      File file = listFiles[i];
      String name = file.getName();
      if (file.isFile() && name.startsWith("ivy") && name.endsWith(".xml")) {
        rv.addAll(getIvyDeps(file));
      }
    }

    return rv.toArray(new String[rv.size()]);

  }

  private Collection<String> getIvyDeps(File ivyFile) throws Exception {
    String moduleName = ivyFile.getParentFile().getName();

    String expectedIvyModuleAttr = moduleName + ivyFile.getName().replaceFirst("ivy", "").replace(".xml", "");

    ArrayList<String> rv = new ArrayList<String>();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document ivyDoc = builder.parse(ivyFile);

    NodeList infos = ivyDoc.getElementsByTagName("info");
    if (infos.getLength() != 1) { throw new RuntimeException("invalid number of info elements: " + infos.getLength()); }
    Node info = infos.item(0);
    String actualModuleAttr = info.getAttributes().getNamedItem("module").getNodeValue();
    if (!expectedIvyModuleAttr.equals(actualModuleAttr)) {
      //
      throw new RuntimeException("wrong \"module\" name (" + actualModuleAttr + ") in "
                                 + ivyFile.getParentFile().getName() + "/" + ivyFile.getName() + ", expected "
                                 + expectedIvyModuleAttr);
    }

    NodeList depsList = ivyDoc.getElementsByTagName("dependencies");
    if (depsList.getLength() == 0) { return Collections.emptyList(); }
    if (depsList.getLength() > 1) { throw new RuntimeException("multiple dependencies in " + ivyFile); }

    NodeList deps = depsList.item(0).getChildNodes();
    int num = deps.getLength();
    for (int i = 0; i < num; i++) {
      Node node = deps.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        if ("dependency".equals(node.getNodeName())) {
          NamedNodeMap nodeMap = node.getAttributes();

          String jar = nodeMap.getNamedItem("name").getNodeValue() + "-" + nodeMap.getNamedItem("rev").getNodeValue()
                       + ".jar";
          rv.add(jar);
        }
      }
    }

    return rv;
  }

  private static String resolveModulesDef() {
    return new File("..", "modules.def.yml").getAbsolutePath();
  }

}
