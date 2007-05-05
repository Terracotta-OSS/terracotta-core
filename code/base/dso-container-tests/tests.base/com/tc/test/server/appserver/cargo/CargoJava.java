/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.cargo;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.Environment.Variable;

import com.tc.test.TestConfigObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This class is used in the process of patching CARGO to allow it's child processes to know whether the parent process
 * is still alive and kill themselves off if need be. It is a decorator for the ANT Java task which calls
 * {@link CargoLinkedChildProcess} which in turn spawns the desired appserver instance.
 */
public final class CargoJava extends Java {

  private static final boolean DEBUG  = false;

  // this static thing is TERRIBLE, but trying to get tigher integration with Cargo is worse
  public static final Link     LINK   = new Link();

  private Java                 java;
  private Path                 classpath;
  private String               className;
  private List                 args;
  private boolean              dirSet = false;

  public CargoJava(Java java) {
    this.java = java;
    this.args = new ArrayList();
  }

  private void wrapProcess() {
    Args linkArgs;
    try {
      linkArgs = Link.take();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    File dir = linkArgs.instancePath;

    String logFile = new File(dir.getParent(), dir.getName() + ".log").getAbsolutePath();

    if (!dirSet) {
      setDir(dir);
    }

    java.setOutput(new File(logFile));
    java.setFailonerror(true);

    assignWrappedArgs(linkArgs);
    TestConfigObject config = TestConfigObject.getInstance();
    classpath.setPath(classpath.toString() + File.pathSeparatorChar + config.linkedChildProcessClasspath());
    java.setClassname(CargoLinkedChildProcess.class.getName());
    // java.setMaxmemory("128m");
    Environment.Variable envVar = new Environment.Variable();
    envVar.setKey("JAVA_HOME");
    envVar.setValue(System.getProperty("java.home"));
    java.addEnv(envVar);
    // Argument argument = java.createJvmarg();
    // argument.setValue("-verbose:gc");

    if (DEBUG) {
      CommandlineJava cmdLineJava = getCommandLine(this.java);
      System.err.println(cmdLineJava.describeCommand());
    }

    java.createJvmarg().setValue("-DNODE=" + dir.getName());

    java.execute();
  }

  private static CommandlineJava getCommandLine(Java j) {
    // more utter gross-ness
    try {
      Field f = j.getClass().getDeclaredField("cmdl");
      f.setAccessible(true);
      return (CommandlineJava) f.get(j);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void assignWrappedArgs(Args linkArgs) {
    java.clearArgs();
    java.createArg().setValue(this.className);
    java.createArg().setValue(Integer.toString(linkArgs.port));
    java.createArg().setValue(linkArgs.instancePath.getAbsolutePath());

    Iterator iter = this.args.iterator();
    while (iter.hasNext()) {
      String[] parts = ((Argument) iter.next()).getParts();
      for (int i = 0; i < parts.length; ++i)
        java.createArg().setValue(parts[i]);
    }
  }

  public void addEnv(Variable arg0) {
    this.java.addEnv(arg0);
  }

  public void addSysproperty(Variable arg0) {
    this.java.addSysproperty(arg0);
  }

  public void clearArgs() {
    this.java.clearArgs();
  }

  public Argument createArg() {
    Argument out = this.java.createArg();
    this.args.add(out);
    return out;
  }

  public Path createClasspath() {
    Path path = this.java.createClasspath();
    this.classpath = path;
    return path;
  }

  public Argument createJvmarg() {
    return this.java.createJvmarg();
  }

  public boolean equals(Object obj) {
    return this.java.equals(obj);
  }

  public void execute() throws BuildException {
    wrapProcess();
  }

  public int executeJava() throws BuildException {
    return this.java.executeJava();
  }

  public String getDescription() {
    return this.java.getDescription();
  }

  public Location getLocation() {
    return this.java.getLocation();
  }

  public Target getOwningTarget() {
    return this.java.getOwningTarget();
  }

  public Project getProject() {
    return this.java.getProject();
  }

  public RuntimeConfigurable getRuntimeConfigurableWrapper() {
    return this.java.getRuntimeConfigurableWrapper();
  }

  public String getTaskName() {
    return this.java.getTaskName();
  }

  public int hashCode() {
    return this.java.hashCode();
  }

  public void init() throws BuildException {
    this.java.init();
  }

  public void log(String arg0, int arg1) {
    this.java.log(arg0, arg1);
  }

  public void log(String arg0) {
    this.java.log(arg0);
  }

  public void maybeConfigure() throws BuildException {
    this.java.maybeConfigure();
  }

  public void setAppend(boolean arg0) {
    this.java.setAppend(arg0);
  }

  public void setArgs(String arg0) {
    this.java.setArgs(arg0);
  }

  public void setClassname(String arg0) throws BuildException {
    this.className = arg0;
    this.java.setClassname(arg0);
  }

  public void setClasspath(Path arg0) {
    this.java.setClasspath(arg0);
  }

  public void setClasspathRef(Reference arg0) {
    this.java.setClasspathRef(arg0);
  }

  public void setDescription(String arg0) {
    this.java.setDescription(arg0);
  }

  public void setDir(File arg0) {
    this.java.setDir(arg0);
    dirSet = true;
  }

  public void setFailonerror(boolean arg0) {
    this.java.setFailonerror(arg0);
  }

  public void setFork(boolean arg0) {
    this.java.setFork(arg0);
  }

  public void setJar(File arg0) throws BuildException {
    try {
      String absPath = arg0.getCanonicalFile().getParentFile().getParent();
      JarFile jar = new JarFile(arg0);
      Manifest manifest = jar.getManifest();
      Attributes attrib = manifest.getMainAttributes();

      String classPathAttrib = attrib.getValue("Class-Path");
      String absClassPath = classPathAttrib.replaceAll("^\\.\\.", absPath).replaceAll("\\s\\.\\.",
                                                                                      File.pathSeparatorChar + absPath);
      this.classpath.setPath(classpath.toString() + File.pathSeparatorChar + absClassPath);
      this.classpath.setPath(classpath.toString() + File.pathSeparator + arg0);

      // TODO: make sysprops
      // this.classpath.setPath(classpath.toString() + createExtraManifestClassPath("Endorsed-Dirs", attrib, absPath));
      // this.classpath.setPath(classpath.toString() + createExtraManifestClassPath("Extension-Dirs", attrib, absPath));

      setClassname(attrib.getValue("Main-Class"));

    } catch (IOException ioe) {
      throw new BuildException("problem reading manifest");
    }
  }

  // private String createExtraManifestClassPath(String attributeName, Attributes attrib, String absPath) {
  // String extraDirAttrib = attrib.getValue(attributeName);
  // File absExtraDir = new File(absPath + File.separator + extraDirAttrib);
  // String[] extraJars = absExtraDir.list(new FilenameFilter() {
  // public boolean accept(File dir, String name) {
  // return (name.endsWith(".jar")) ? true : false;
  // }
  // });
  // String extraClassPath = "";
  // for (int i = 0; i < extraJars.length; i++) {
  // extraClassPath += File.pathSeparatorChar + absExtraDir.toString() + File.separator + extraJars[i];
  // }
  // return extraClassPath;
  // }

  public void setJvm(String arg0) {
    this.java.setJvm(arg0);
  }

  public void setJvmargs(String arg0) {
    this.java.setJvmargs(arg0);
  }

  public void setJVMVersion(String arg0) {
    this.java.setJVMVersion(arg0);
  }

  public void setLocation(Location arg0) {
    this.java.setLocation(arg0);
  }

  public void setMaxmemory(String arg0) {
    this.java.setMaxmemory(arg0);
  }

  public void setNewenvironment(boolean arg0) {
    this.java.setNewenvironment(arg0);
  }

  public void setOutput(File arg0) {
    this.java.setOutput(arg0);
  }

  public void setOwningTarget(Target arg0) {
    this.java.setOwningTarget(arg0);
  }

  public void setProject(Project arg0) {
    this.java.setProject(arg0);
  }

  public void setRuntimeConfigurableWrapper(RuntimeConfigurable arg0) {
    this.java.setRuntimeConfigurableWrapper(arg0);
  }

  public void setTaskName(String arg0) {
    this.java.setTaskName(arg0);
  }

  public void setTimeout(Long arg0) {
    this.java.setTimeout(arg0);
  }

  public String toString() {
    return this.java.toString();
  }

  public static class Args {
    final File instancePath;
    final int  port;

    public Args(int port, File instancePath) {
      this.port = port;
      this.instancePath = instancePath;
    }
  }

  public static class Link {
    private static Args         args = null;
    private static final Object lock = new Object();

    public static Args take() throws InterruptedException {
      synchronized (lock) {
        while (args == null) {
          lock.wait();
        }
        Args rv = args;
        args = null;
        lock.notifyAll();
        return rv;
      }
    }

    public static void put(Args putArgs) throws InterruptedException {
      synchronized (lock) {
        while (args != null) {
          lock.wait();
        }

        args = putArgs;
        lock.notifyAll();
      }
    }
  }

}
