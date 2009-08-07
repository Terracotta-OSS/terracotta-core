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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Main {

  // TODO: take modules.def.yml location as command line arg

  private final String        ossModulesFile;
  private final String        entModulesFile;
  private final boolean       isEnterprise;

  private final static String MODULES_DEF_YML  = "modules.def.yml";
  private final static String ENTERPRISE_PASTH = ".." + File.separator + ".." + File.separator + ".." + File.separator
                                                 + ".." + File.separator + "code" + File.separator + "base";

  public Main() {
    String modulesFile = new File("..", MODULES_DEF_YML).getAbsolutePath();
    Util.ensureFile(modulesFile);
    this.ossModulesFile = modulesFile;

    File entModFile = new File(ENTERPRISE_PASTH, MODULES_DEF_YML);
    if (entModFile.exists()) {
      Util.ensureFile(entModFile);
      this.entModulesFile = entModFile.getAbsolutePath();
      this.isEnterprise = true;
    } else {
      this.entModulesFile = null;
      this.isEnterprise = false;
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length > 1) { throw new RuntimeException("Can't take more than one argument"); }
    System.out.println("Running eclipsegen...");
    String moduleName = null;
    if (args.length == 1) {
      moduleName = args[0];
    }
    new Main().generate(moduleName);
    System.out.println("Done!");
  }

  private void generate(String moduleName) throws Exception {
    System.err.println("reading " + this.ossModulesFile);

    File ossBase = new File(this.ossModulesFile).getParentFile();

    Module[] ossModules = getModules(moduleName, loadModules(this.ossModulesFile));

    File entBase = null;
    Module[] entModules = null;
    if (this.isEnterprise) {
      System.err.println("reading " + this.entModulesFile);
      entBase = new File(this.entModulesFile).getParentFile();
      entModules = getModules(moduleName, loadModules(this.entModulesFile));
    }

    if (ossModules == null && entModules == null) { throw new RuntimeException("Module " + moduleName
                                                                               + " was not found"); }

    if (ossModules != null) {
      writeClasspathFiles(ossBase, ossModules);
      writeSettings(ossBase, ossModules);
      writeProjectFiles(ossBase, ossModules);
    }

    if (this.isEnterprise && entModules != null) {
      writeClasspathFiles(entBase, entModules);
      writeSettings(entBase, entModules);
      writeProjectFiles(entBase, entModules);
    }
  }

  @SuppressWarnings("unchecked")
  private Module[] loadModules(String modulesFile) throws FileNotFoundException {
    Map ymlData = (Map) YAML.load(new FileReader(modulesFile));
    return ModulesDefReader.readFrom(ymlData);
  }

  private Module[] getModules(String moduleName, Module[] modules) {
    if (moduleName == null) { return modules; }

    for (Module module : modules) {
      if (module.getName().equals(moduleName)) { return new Module[] { module }; }
    }

    return null;
  }

  private void writeSettings(File base, Module[] modules) throws IOException {
    File source = new File(new File("."), ".settings");
    Util.ensureDir(source);

    Properties jdtCorePrefs = readProps(new File(source, "org.eclipse.jdt.core.prefs"));
    Properties jdtUiPrefs = readProps(new File(source, "org.eclipse.jdt.ui.prefs"));

    for (Module module : modules) {
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

    System.err.println("Writing .settings for " + modDir.getName());
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
    for (String string : jdkConvertKeys) {
      props.setProperty(string, module.getJdk().getRaw());
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
    for (Module module : modules) {
      File modDir = new File(base, module.getName());
      Util.ensureDir(modDir);

      String[] src = findSourceFolders(modDir);
      String[] jars = getIvyDependencies(modDir);

      String[] moduleJars = findModuleJars(modDir);

      writeDotClassPath(modDir, module, src, jars, moduleJars);
    }

  }

  private void writeProjectFiles(File base, Module[] modules) throws Exception {
    for (Module module : modules) {
      File modDir = new File(base, module.getName());
      Util.ensureDir(modDir);

      writeDotProjectFile(modDir, module);
    }

  }

  private String[] findModuleJars(File modDir) {
    TreeSet<String> rv = new TreeSet<String>();
    File[] listFiles = modDir.listFiles();
    for (File file : listFiles) {
      if (file.isDirectory() && libFolders.contains(file.getName())) {
        rv.addAll(getJarsFromDir(file));
      }
    }

    return rv.toArray(new String[rv.size()]);
  }

  private Collection<String> getJarsFromDir(File libDir) {
    List<String> rv = new ArrayList<String>();
    File[] listFiles = libDir.listFiles();
    for (File file : listFiles) {
      if (file.getName().endsWith(".jar")) {
        rv.add(libDir.getName() + "/" + file.getName());
      }
    }

    return rv;
  }

  private void writeDotClassPath(File modDir, Module module, String[] src, String[] jars, String[] moduleJars)
      throws IOException {

    boolean isUiEclipse = module.getName().equals("ui-eclipse");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    ps.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    ps.println("<classpath>");

    // source folders and resources
    for (String path : src) {
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
    if (isUiEclipse) {
      ps.println("\t<classpathentry exported=\"true\" kind=\"con\" path=\"org.eclipse.pde.core.requiredPlugins\"/>");
    } else {
      for (String jar : jars) {
        ps.println("\t<classpathentry exported=\"true\" kind=\"lib\" path=\"/dependencies/lib/" + jar + "\"/>");
      }
    }

    // modules dependencies
    for (String string : module.getDependencies()) {
      ps.println("\t<classpathentry combineaccessrules=\"false\" kind=\"src\" path=\"/" + string + "\"/>");
    }

    // extra module jars
    for (String jar : moduleJars) {
      ps.println("\t<classpathentry exported=\"true\" kind=\"lib\" path=\"" + jar + "\"/>");

    }

    // ps.println("\t<classpathentry kind=\"output\" path=\"bin\"/>");

    ps.println("</classpath>");
    ps.close();

    System.err.println("Writing .classpath for " + modDir.getName());
    FileOutputStream fos = new FileOutputStream(new File(modDir, ".classpath"));
    fos.write(baos.toByteArray());
    fos.close();
  }

  private void writeDotProjectFile(File modDir, Module module) throws IOException {
    File file = new File(modDir, ".project");

    // don't overwrite existing .project files (they might be customized)
    if (file.exists()) { return; }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    ps.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    ps.println("<projectDescription>");
    ps.println("<name>" + module.getName() + "</name>");

    ps.println();
    ps.println("<comment>" + module.getName() + " source code" + "</comment>");

    ps.println();
    ps.println("<projects />");

    ps.println("<buildSpec>");
    ps.println("<buildCommand>");
    ps.println("<name>org.eclipse.jdt.core.javabuilder</name>");
    ps.println();
    ps.println("<arguments />");
    ps.println("</buildCommand>");
    ps.println();
    ps.println("</buildSpec>");

    ps.println();
    ps.println("<natures ><nature >org.eclipse.jdt.core.javanature</nature>");
    ps.println();
    ps.println("<nature >org.eclipse.pde.PluginNature</nature>");
    ps.println();
    ps.println("</natures>");
    ps.println();
    ps.println("</projectDescription>");
    ps.close();

    System.err.println("Writing .project for " + modDir.getName());
    FileOutputStream fos = new FileOutputStream(file);
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
    for (String src : sourceFolders) {
      if (!"src".equals(src)) {
        libFolders.add("lib." + src);
      }
    }

    Collection<String> res = new ArrayList<String>();
    for (String string : sourceFolders) {
      res.add(string + ".resources");
    }
    sourceFolders.addAll(res);

  }

  private String[] findSourceFolders(File modDir) {
    List<String> rv = new ArrayList<String>();

    File[] listFiles = modDir.listFiles();
    for (File file : listFiles) {
      if (file.isDirectory() && sourceFolders.contains(file.getName())) {
        rv.add(file.getName());
      }
    }

    Collections.sort(rv);
    return rv.toArray(new String[rv.size()]);
  }

  private String[] getIvyDependencies(File modDir) throws Exception {
    SortedSet<String> rv = new TreeSet<String>();

    File[] listFiles = modDir.listFiles();
    for (File file : listFiles) {
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

}
