/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.process;

import com.tc.util.Assert;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A child Java process that uses a socket-based ping protocol to make sure that if the parent dies, the child dies a
 * short time thereafter. Useful for avoiding 'zombie child processes' when writing tests, etc. &mdash; otherwise, if
 * the parent process crashes or otherwise terminates abnormally, you'll get child processes accumulating until all hell
 * breaks loose on the box.
 * </p>
 * <p>
 * Although it can't actually be related through Java inheritance (because {@link Process}is a class, not an
 * interface), this class behaves essentially identical to {@link Process}with two differences:
 * <ul>
 * <li>You instantiate this class directly, rather than getting an instance from {@link Runtime#exec}.</li>
 * <li>The process doesn't start until you call {@link #start}.</li>
 * </ul>
 */
public class LinkedJavaProcess {

  private File           javaHome;
  private final String   mainClassName;
  private String[]       javaArguments;
  private final String[] arguments;
  private String[]       environment;
  private File           directory;
  private File           javaExecutable;
  private boolean        isDSO   = false;

  private Process        process;
  private boolean        running;
  private final List     copiers = Collections.synchronizedList(new ArrayList());

  public LinkedJavaProcess(String mainClassName, String[] classArguments) {
    Assert.assertNotBlank(mainClassName);

    if (classArguments == null) classArguments = new String[0];

    this.mainClassName = mainClassName;
    this.javaArguments = null;
    this.arguments = classArguments;
    this.environment = null;
    this.directory = null;
    this.javaExecutable = null;
    this.process = null;
    this.running = false;
  }

  public File getJavaHome() {
    return javaHome;
  }

  public void setJavaHome(File javaHome) {
    this.javaHome = javaHome;
  }

  public LinkedJavaProcess(String mainClassName) {
    this(mainClassName, null);
  }

  public void setJavaExecutable(File javaExecutable) {
    Assert.assertNotNull(javaExecutable);

    this.javaExecutable = javaExecutable;
  }

  public void setJavaArguments(String[] javaArguments) {
    this.javaArguments = javaArguments;
  }

  public void setEnvironment(String[] environment) {
    this.environment = environment;
  }

  public void setDirectory(File directory) {
    this.directory = directory;
  }

  public void setDSOTarget(boolean isDSO) {
    this.isDSO = isDSO;
  }

  public synchronized void destroy() {
    if (!this.running) throw new IllegalStateException("This LinkedJavaProcess is not running.");
    this.process.destroy();
    this.running = false;
  }

  private synchronized void setJavaExecutableIfNecessary() throws IOException {
    if (this.javaExecutable == null) {
      if (javaHome == null) {
        javaHome = new File(System.getProperty("java.home"));
      }

      File javaBin = new File(javaHome, "bin");
      File javaPlain = new File(javaBin, "java");
      File javaExe = new File(javaBin, "java.exe");

      if (this.javaExecutable == null) {
        if (javaPlain.exists() && javaPlain.isFile()) this.javaExecutable = javaPlain;
      }

      if (this.javaExecutable == null) {
        if (javaExe.exists() && javaExe.isFile()) this.javaExecutable = javaExe;
      }

      if (this.javaExecutable == null) {
        // formatting
        throw new IOException("Can't find the Java binary; perhaps you need to set it yourself? Tried "
            + javaPlain.getAbsolutePath() + " and " + javaExe.getAbsolutePath());
      }
    }
  }

  public synchronized void start() throws IOException {
    if (this.running) throw new IllegalStateException("This LinkedJavaProcess is already running.");

    HeartBeatService.startHeartBeatService();

    List fullCommandList = new LinkedList();
    List allJavaArguments = new ArrayList();

    allJavaArguments.add("-Djava.class.path=" + System.getProperty("java.class.path"));
    if (this.javaArguments != null) allJavaArguments.addAll(Arrays.asList(this.javaArguments));

    setJavaExecutableIfNecessary();

    int socketPort = HeartBeatService.listenPort();

    Map env = makeEnvMap(Arrays.asList(this.environment == null ? new String[] {} : this.environment));
    fixupEnvironment(env);

    fullCommandList.add(this.javaExecutable.getAbsolutePath());
    fullCommandList.addAll(allJavaArguments);
    fullCommandList.add(isDSO ? DSOLinkedJavaProcessStarter.class.getName() : LinkedJavaProcessStarter.class.getName());
    fullCommandList.add(Integer.toString(socketPort));
    fullCommandList.add(this.mainClassName);
    if (this.arguments != null) fullCommandList.addAll(Arrays.asList(this.arguments));

    String[] fullCommand = (String[]) fullCommandList.toArray(new String[fullCommandList.size()]);
    this.process = Runtime.getRuntime().exec(fullCommand, makeEnv(env), this.directory);
    this.running = true;
  }

  private Map makeEnvMap(List list) {
    Map rv = new HashMap();

    for (Iterator iter = list.iterator(); iter.hasNext();) {
      String[] nameValue = ((String) iter.next()).split("=", 2);
      rv.put(nameValue[0], nameValue[1]);
    }

    return rv;
  }

  private String[] makeEnv(Map env) {
    int i = 0;
    String[] rv = new String[env.size()];
    for (Iterator iter = env.keySet().iterator(); iter.hasNext(); i++) {
      String key = (String) iter.next();
      rv[i] = key + "=" + env.get(key);
    }
    return rv;
  }

  private static void fixupEnvironment(Map env) {
    if (Os.isWindows()) {
      // A bunch of name lookup stuff will fail w/o setting SYSTEMROOT. Also, if
      // you have apple's rendevous/bonjour
      // client installed, it needs to be in the PATH such that dnssd.dll will
      // be found when using DNS

      if (!env.containsKey("SYSTEMROOT")) {
        String root = Os.findWindowsSystemRoot();
        if (root == null) { throw new RuntimeException("cannot find %SYSTEMROOT% in the environment"); }
        env.put("SYSTEMROOT", root);
      }

      String crappleDirs = "C:\\Program Files\\Rendezvous\\" + File.pathSeparator + "C:\\Program Files\\Bonjour\\";

      if (!env.containsKey("PATH")) {
        env.put("PATH", crappleDirs);
      } else {
        String path = (String) env.get("PATH");
        path = path + File.pathSeparator + crappleDirs;
        env.put("PATH", path);
      }
    }
  }

  /**
   * Java names these things a bit funny &mdash; this is the spawned process's <tt>stdout</tt>.
   */
  public synchronized InputStream getInputStream() {
    if (!this.running) throw new IllegalStateException("This LinkedJavaProcess is not yet running.");
    return this.process.getInputStream();
  }

  public InputStream STDOUT() {
    return getInputStream();
  }

  public OutputStream STDIN() {
    return getOutputStream();
  }

  public InputStream STDERR() {
    return getErrorStream();
  }

  public void mergeSTDOUT() {
    mergeStream(STDOUT(), System.out);
  }

  public void mergeSTDERR() {
    mergeStream(STDERR(), System.err);
  }

  private void mergeStream(InputStream in, OutputStream out) {
    StreamCopier copier = new StreamCopier(in, out);
    copiers.add(copier);
    copier.start();
  }

  /**
   * This is the spawned process's <tt>stderr</tt>.
   */
  public synchronized InputStream getErrorStream() {
    if (!this.running) throw new IllegalStateException("This LinkedJavaProcess is not yet running.");
    return this.process.getErrorStream();
  }

  /**
   * Java names these things a bit funny &mdash; this is the spawned process's <tt>stdin</tt>.
   */
  public synchronized OutputStream getOutputStream() {
    if (!this.running) throw new IllegalStateException("This LinkedJavaProcess is not yet running.");
    return this.process.getOutputStream();
  }

  public synchronized int exitValue() {
    if (this.process == null) throw new IllegalStateException("This LinkedJavaProcess has not been started.");
    int out = this.process.exitValue();
    // Process.exitValue() throws an exception if not yet terminated, so we know
    // it's terminated now.
    this.running = false;
    return out;
  }

  public int waitFor() throws InterruptedException {
    Process theProcess = null;

    synchronized (this) {
      if (!this.running) throw new IllegalStateException("This LinkedJavaProcess is not running.");
      theProcess = this.process;
      Assert.assertNotNull(theProcess);
    }

    int exitCode = theProcess.waitFor();

    for (Iterator i = copiers.iterator(); i.hasNext();) {
      Thread t = (Thread) i.next();
      t.join();
      i.remove();
    }

    synchronized (this) {
      this.running = false;
    }

    return exitCode;

  }

}