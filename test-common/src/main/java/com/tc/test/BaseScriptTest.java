/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.test;

import com.tc.process.StreamCollector;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collector;

import static com.tc.test.ScriptTestUtil.extractArguments;
import static com.tc.test.ScriptTestUtil.extractEnvironment;
import static com.tc.test.ScriptTestUtil.extractProperties;
import static java.lang.System.arraycopy;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;

/**
 * Test harness for testing Java launch scripts.
 * <p>
 * While different operating systems and file systems have different restrictions on
 * the properties of the names which may be used to identify directories and files,
 * the general recommendation is to keep it simple:
 * <ul>
 *   <li>Limit the characters used to upper- and lower-case ASCII letters, underscore, period, and hyphen-minus
 *   (<i>IEEE Std 1003.1-2017 Posix Base Specifications, Volume 1: Base Definitions, Portable Filename Character Set 3.282</i>).</li>
 *   <li>Keep the fully-qualified pathname <i>short</i> (Windows limits fully-qualified names to 260 characters, Posix
 *   recommends 256).</li>
 *   <li>Keep the path name segments short -- the Posix specification suggests 14 for portability though
 *   systems generally support 255 characters or more.</li>
 * </ul>
 * Unfortunately, file path properties exceeding the <i>simple</i> are in common use in various operating
 * systems.  Windows, for example, makes frequent use of parentheses -- examples include
 * {@code C:\Program Files (x86)} and in default names used for downloads.  So we test for the use of
 * <i>special</i> characters.
 * <p>
 * Extending classes must implement {@link #testScript(File)}.  {@code testScript} is called for each test
 * and is responsible for establishing the script execution environment -- "installing" files and setting
 * environment variables -- then invoking {@link #execScript(File, Duration, Map, String, String...)} to
 * execute the script.
 *
 * @see #createInstallDir(File, Path)
 * @see #installScript(String, Path)
 * @see #createJar(String, Path, boolean, String...)
 * @see #execScript(File, Duration, Map, String, String...)
 * @see ScriptResult
 */
public abstract class BaseScriptTest {

  /**
   * Identifies the property through which the test seed may be fixed.
   * If not specified, a random long is chosen.
   */
  public static final String SCRIPT_TEST_SEED_PROPERTY = "script.test.seed";
  /**
   * Identifies the property specifying if a copy of each surrogate copy used should be
   * written to {@code System.out}.  If set to {@code true}, the renamed copy of the
   * surrogate test class is displayed.
   */
  public static final String SCRIPT_TEST_DEBUG_PROPERTY = "script.test.debug";

  @ClassRule
  public static final TemporaryFolder INSTALL_ROOT_PARENT = new TemporaryFolder();

  /**
   * Identifies the current operating system.
   */
  protected static final OperatingSystem CURRENT_OPERATING_SYSTEM = OperatingSystem.currentOperatingSystem();

  private static final Random SEEDS = new Random();

  /**
   * Identifies the level of support for special characters used in file path names.
   * @see <a href="https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file">Naming Files, Paths, and Namespaces</a>
   */
  private static List<CharDef> SPECIAL_CHARACTERS = Arrays.asList(
      /*
       * Characters 0x00 through 0x1F are the C0 control characters.  These
       * characters are NOT LEGAL for use in Windows file path names.  While
       * control characters other than 0x00 are LEGAL in Linux, we don't support
       * them for use in file paths used in Terracotta.
       */
      def((char)0x00, "NUL", OperatingSystem.NONE),
      def((char)0x01, "SOH", OperatingSystem.NONE),
      def((char)0x02, "STX", OperatingSystem.NONE),
      def((char)0x03, "ETX", OperatingSystem.NONE),
      def((char)0x04, "EOT", OperatingSystem.NONE),
      def((char)0x05, "ENQ", OperatingSystem.NONE),
      def((char)0x06, "ACK", OperatingSystem.NONE),
      def((char)0x07, "BEL", OperatingSystem.NONE),
      def((char)0x08, "BS", OperatingSystem.NONE),
      def((char)0x09, "TAB", OperatingSystem.NONE),
      def((char)0x0A, "LF", OperatingSystem.NONE),
      def((char)0x0B, "VT", OperatingSystem.NONE),
      def((char)0x0C, "FF", OperatingSystem.NONE),
      def((char)0x0D, "CR", OperatingSystem.NONE),
      def((char)0x0E, "SO", OperatingSystem.NONE),
      def((char)0x0F, "SI", OperatingSystem.NONE),
      def((char)0x10, "DLE", OperatingSystem.NONE),
      def((char)0x11, "DC1", OperatingSystem.NONE),
      def((char)0x12, "DC2", OperatingSystem.NONE),
      def((char)0x13, "DC3", OperatingSystem.NONE),
      def((char)0x14, "DC4", OperatingSystem.NONE),
      def((char)0x15, "NAK", OperatingSystem.NONE),
      def((char)0x16, "SYN", OperatingSystem.NONE),
      def((char)0x17, "ETB", OperatingSystem.NONE),
      def((char)0x18, "CAN", OperatingSystem.NONE),
      def((char)0x19, "EM", OperatingSystem.NONE),
      def((char)0x1A, "SUB", OperatingSystem.NONE),
      def((char)0x1B, "ESC", OperatingSystem.NONE),
      def((char)0x1C, "FS", OperatingSystem.NONE),
      def((char)0x1D, "GS", OperatingSystem.NONE),
      def((char)0x1E, "RS", OperatingSystem.NONE),
      def((char)0x1F, "US", OperatingSystem.NONE),

      /*
       * The following characters are NOT LEGAL in both Windows and Linux.
       */
      def('/', "slash", OperatingSystem.UNIX, OperatingSystem.WINDOWS),

      /*
       * The following characters are not NOT LEGAL for use in Windows
       * file path names.
       */
      def('<', "lessThan", OperatingSystem.WINDOWS),
      def('>', "greaterThan", OperatingSystem.WINDOWS),
      def('"', "quote", OperatingSystem.WINDOWS),
      def('\\', "backslash", OperatingSystem.WINDOWS),
      def('|', "bar", OperatingSystem.WINDOWS),
      def('?', "question", OperatingSystem.WINDOWS),
      def('*', "asterisk", OperatingSystem.WINDOWS),

      /*
       * The following character is NOT LEGAL in Windows but, while being
       * LEGAL in Linux, must be avoided because of its use in Java.
       */
      def(':', "colon", OperatingSystem.UNIX, OperatingSystem.WINDOWS),

      /*
       * The following characters are LEGAL in both Windows and Linux but
       * must be avoided in Windows because its use in Java.
       */
      def(';', "semicolon", OperatingSystem.WINDOWS),

      /*
       * The following characters are LEGAL in both Windows and Linux but,
       * because of scripting issues in Windows, are not supported in Windows.
       */
      def('!', "exclamation", OperatingSystem.WINDOWS),

      /*
       * The following characters are LEGAL in both Windows and Linux but,
       * due to issues with Spring Boot and Logback, must not be used.
       */
      def('%', "percent", OperatingSystem.NONE),    // `org.springframework.boot.logging.logback.DefaultLogbackConfiguration#setRollingPolicy` issue
      def('{', "leftBrace", OperatingSystem.NONE),  // `ch.qos.logback.core.rolling.RollingFileAppender#checkForFileAndPatternCollisions` issue

      /*
       * The following characters are LEGAL in both Windows and Linux and
       * are otherwise not restricted.  Use in scripts may require strict
       * attention to quoting or other techniques.
       */
      def((char)0x20, "space"),
      def('#', "number"),
      def('$', "dollar"),
      def('&', "ampersand"),
      def('\'', "apostrophe"),
      def('(', "leftParen"),
      def(')', "rightParen"),
      def('+', "plus"),
      def(',', "comma"),
      def('-', "minus"),
      def('.', "period"),
      def('=', "equals"),
      def('@', "at"),
      def('[', "leftBracket"),
      def(']', "rightBracket"),
      def('^', "caret"),
      def('_', "underscore"),
      def('`', "backtick"),
      def('}', "rightBrace"),
      def('~', "tilde")
  );

  /**
   * Special characters permitted for the current operating system.
   */
  private static final char[] CURRENT_OPERATING_SYSTEM_SPECIAL_CHARACTERS = SPECIAL_CHARACTERS.stream()
      .filter(d -> d.supportedOs.contains(CURRENT_OPERATING_SYSTEM))
      .collect(Collector.of(
          () -> CharBuffer.allocate(SPECIAL_CHARACTERS.size()),
          (b, d) -> b.append(d.character),
          CharBuffer::append,
          b -> {
            char[] chars = new char[b.position()];
            ((CharBuffer)b.flip()).get(chars);
            return chars;
          }));

  /** These characters have unrestricted use in file name segments. */
  private static final char[] UNRESTRICTED_STANDARD_CHARACTERS =
      "abcdefghijlkmnopqrstuvwxyzABCDEFGHIJLKMNOPQRSTUVWXYZ0123456789_".toCharArray();

  /** Some characters, on some operating systems, may not be used to start or end a name segment. */
  private static final char[] ALL_STANDARD_CHARACTERS;
  static {
    char[] all = Arrays.copyOf(UNRESTRICTED_STANDARD_CHARACTERS, UNRESTRICTED_STANDARD_CHARACTERS.length + 2);
    all[all.length - 2] = '.';
    all[all.length - 1] = '-';
    ALL_STANDARD_CHARACTERS = all;
  }

  private final int pathNameSegmentLength;

  /**
   * Create a script test instance.
   * @param pathNameSegmentLength the length used for the generated installation path name segments
   */
  protected BaseScriptTest(int pathNameSegmentLength) {
    this.pathNameSegmentLength = pathNameSegmentLength;
  }

  /**
   * Method to execute a script in the provided installation root.  The implementing
   * method must:
   * <ol>
   *   <li>Create the directory to contain the script file(s) under {@code installRoot}</li>
   *   <li>Install the script(s) to be tested in the script (bin) directory</li>
   *   <li>If necessary:
   *   <ol type="a">
   *     <li>Create a directory to hold a test jar</li>
   *     <li>Create a test jar containing the class(es) needed for the script</li>
   *   </ol>
   *   </li>
   *   <li>Execute the script</li>
   *   <li>Assert results</li>
   * </ol>
   * @param installRoot the directory serving as the root of the installation environment
   * @throws Exception if the test fails
   * @see #createInstallDir(File, Path)
   * @see #installScript(String, Path)
   * @see #execScript(File, Duration, Map, String, String...)
   */
  protected abstract void testScript(File installRoot) throws Exception;


  /**
   * Test using "standard" installation directory name.
   */
  @Test
  public void testStandardChars() throws Exception {
    long seed = getSeed();
    testScriptInternal(seed, generateStandardSegment(new Random(seed), pathNameSegmentLength));
  }

  /**
   * Test using installation directory name containing <i>legal</i> special characters for the
   * current operating system.
   */
  @Test
  public void testUnusualInstallDir() throws Exception {
    long seed = getSeed();
    testScriptInternal(seed, generateSpecialSegment(new Random(seed), pathNameSegmentLength));
  }


  private void testScriptInternal(long seed, String folder) throws Exception {
    File installRoot;
    try {
      installRoot = INSTALL_ROOT_PARENT.newFolder(folder);
    } catch (IOException e) {
      throw new AssertionError("Failed with seed " + seed + " using folder=\""
          + folder + "\" in \"" + INSTALL_ROOT_PARENT.getRoot() + "\"", e);
    }

    try {
      testScript(installRoot);
    } catch (AssertionError e) {
      throw new AssertionError("Failed with seed " + seed + " using installRoot=\"" + installRoot + "\"", e);
    }
  }

  /**
   * Creates a subdirectory under {@code installRoot}.
   * @param installRoot the {@code File} designating the root of the install directory;
   *                    this corresponds to the kit directory
   * @param installSubdir the <i>relative</i> path of the subdirectory to be created
   *                      under {@code installRoot}; for example,
   *                      {@code Paths.get("tools", "cluster-tool", "bin")} would extend
   *                      {@code installRoot} with {@code tools/cluster-tool/bin}
   * @return the {@code Path} designating the subdirectory
   * @throws IOException if an error is encountered creating the subdirectory
   */
  protected static Path createInstallDir(File installRoot, Path installSubdir) throws IOException {
    return Files.createDirectories(installRoot.toPath().resolve(installSubdir));
  }

  /**
   * Copies the indicated script file, specified as a resource, to the indicated path.
   * @param scriptResource the resource path to the script file
   * @param binPath the directory into which the resource file is copied
   * @return the path of the copied file
   * @throws IOException if an error occurs while copying the resource to {@code binPath}
   */
  protected final Path installScript(String scriptResource, Path binPath) throws IOException {
    URL scriptFile = this.getClass().getResource(scriptResource);
    if (scriptFile == null) {
      throw new AssertionError("Unable to locate " + scriptResource + " as a resource");
    }

    URI uri;
    try {
      uri = scriptFile.toURI();
    } catch (Exception e) {
      throw new AssertionError("Unexpected error converting \"" + scriptFile + "\" to a Path", e);
    }

    Path scriptPath;
    try {
      scriptPath = Paths.get(uri);
    } catch (Exception e) {
      throw new AssertionError("Unexpected error converting \"" + scriptFile + "\" to a Path; uri=" + uri, e);
    }
    Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
            );
    Path f = Files.copy(scriptPath, binPath.resolve(scriptPath.getFileName()), StandardCopyOption.COPY_ATTRIBUTES);
    try {
      Files.setPosixFilePermissions(f, perms);
    } catch (UnsupportedOperationException ignored) {
    }
    return f;
  }

  /**
   * Creates a JAR file containing the indicated classes obtained from {@code CLASSPATH}.
   * The first item of {@code classNames} is set as {@code Main-Class}.
   * <p>
   * This method does not construct a dependency graph of the classes specified in {@code classNames} --
   * {@code classNames} must be complete with respect to non-JDK dependencies.
   * @param jarName the name to assign to the JAR file
   * @param libDir the directory into which the JAR file is written
   * @param useSurrogate if {@code true}, a copy of {@link SurrogateMain} is used for
   *                     the first class in {@code classNames}
   * @param classNames the class names, in dot-separated form, to include in the JAR
   * @throws IOException if an error is encountered while writing the JAR file or creating the surrogate
   */
  protected final void createJar(String jarName, Path libDir, boolean useSurrogate, String... classNames)
      throws IOException {
    Path jarPath = libDir.resolve(jarName);
    Files.deleteIfExists(jarPath);

    Manifest manifest = new Manifest();
    Attributes mainAttributes = manifest.getMainAttributes();
    mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    mainAttributes.put(Attributes.Name.MAIN_CLASS, classNames[0]);

    Set<String> entrySet = new LinkedHashSet<>();
    try (JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(jarPath.toFile()), manifest)) {
      for (String className : classNames) {
        if (useSurrogate) {
          addEntry(entrySet, jarOut, true, className);
          addEntry(entrySet, jarOut, false, "com.tc.test.ScriptTestUtil");
          useSurrogate = false;
        } else {
          addEntry(entrySet, jarOut, false, className);
        }
      }
    }
  }

  /**
   * Adds a class to a jar.
   * @param entrySet the set of entries already in the jar
   * @param jarOut the output stream used to write to the jar
   * @param useSurrogate if {@code true}, use a copy of {@link SurrogateMain} for the class;
   *                     if {@code false}, use the named class
   * @param className the name of the class in dot-separated form
   * @throws IOException if an error occurs while writing to {@code jarOut}
   */
  private void addEntry(Set<String> entrySet, JarOutputStream jarOut, boolean useSurrogate, String className)
      throws IOException {

    String targetResourceName = className.replace('.', '/') + ".class";
    try (ClassStream classIn = (useSurrogate ? createSurrogate(className) : getClassFile(targetResourceName))) {

      long lastModifiedTime = classIn.lastModifiedTime();

      /*
       * Add a jar entry for each package qualifier.
       */
      String[] segments = targetResourceName.split("/");
      StringBuilder entryPath = new StringBuilder();
      for (int i = 0; i < segments.length - 1; i++) {
        entryPath.append(segments[i]).append('/');
        String entryName = entryPath.toString();
        if (entrySet.add(entryName)) {
          JarEntry entry1 = new JarEntry(entryName);
          entry1.setTime(lastModifiedTime);
          jarOut.putNextEntry(entry1);
          jarOut.closeEntry();
        }
      }

      /*
       * Copy the class into the jar.
       */
      if (entrySet.add(targetResourceName)) {
        JarEntry entry = new JarEntry(targetResourceName);
        entry.setTime(lastModifiedTime);
        jarOut.putNextEntry(entry);

        byte[] chunk = new byte[8192];
        int count;
        while ((count = classIn.read(chunk)) != -1) {
          jarOut.write(chunk, 0, count);
        }

        jarOut.closeEntry();
      }
    }
  }

  /**
   * Create a script test class surrogate by renaming a copy of {@code com.tc.text.SurrogateMain}.
   * @param targetClassName the name, in dot-separated format, of the desired class
   * @return a {@code byte[]} containing the renamed surrogate
   * @throws IOException if an error occurs while reading the surrogate class
   */
  private ClassStream createSurrogate(String targetClassName) throws IOException {

    String targetTypeName = targetClassName.replace('.', '/');
    String sourceTypeName = SurrogateMain.class.getName().replace('.', '/');

    String resourceName = sourceTypeName + ".class";

    try (ClassStream classStream = getClassFile(resourceName)) {
      ClassWriter writer = new ClassWriter(0);
      Remapper classRenamer = new Remapper() {
        @Override
        public String map(String internalName) {
          if (internalName.equals(sourceTypeName)) {
            return targetTypeName;
          }
          return super.map(internalName);
        }
      };
      ClassVisitor processVisitor = new ClassRemapper(writer, classRenamer);
      processVisitor = appendASMifier(processVisitor);

      ClassReader reader = new ClassReader(classStream);
      reader.accept(processVisitor, 0);
      writer.visitEnd();

      return new ClassStream(new ByteArrayInputStream(writer.toByteArray()), classStream.lastModifiedTime());
    }
  }

  /**
   * Gets a stream over the indicated class bytes.
   * @param classResourceName the class name suitable for use in {@code ClassLoader.getResource}, e.g.
   *                          in clash-separated form
   * @return a new {@code ClassStream} instance for reading the class bytes; the returned stream is
   *      open and should be disposed of properly
   * @throws IOException if an error is raised while processing {@code classResourceName}
   */
  private ClassStream getClassFile(String classResourceName) throws IOException {
    URL classUrl = this.getClass().getClassLoader().getResource(classResourceName);
    if (classUrl == null) {
      throw new FileNotFoundException("Class file \"" + classResourceName.replace('/', '.') + "\" not found in classpath");
    }

    try {
      URLConnection urlConnection = classUrl.openConnection();

      long lastModifiedTime;
      if (urlConnection instanceof JarURLConnection) {
        JarURLConnection jarConnection = (JarURLConnection)urlConnection;
        lastModifiedTime =  jarConnection.getJarEntry().getLastModifiedTime().toMillis();
      } else {
        lastModifiedTime = urlConnection.getLastModified();
      }

      return new ClassStream(urlConnection.getInputStream(), lastModifiedTime);

    } catch (IOException e) {
      throw new IOException("Failed while processing "+ classResourceName.replace('/', '.'), e);
    }
  }

  /**
   * If {@code script.test.debug} property is {@code true}, adds an {@code ASMifier} printer
   * to the {@code ClassVisitor} provided.
   * <p>
   * This is isolated in this method to permit running tests <i>without</i> ASM Util library
   * present in class path.
   * @param visitor the {@code ClassVisitor} to which the {@code ASMifier} printer is chained
   * @return {@code visitor} if the {@code script.test.debug} property is omitted or false or
   *    the ASM Util library is not available; a {@code TraceClassVisitor} instance otherwise
   */
  private static ClassVisitor appendASMifier(ClassVisitor visitor) {
    if (Boolean.parseBoolean(System.getProperty(SCRIPT_TEST_DEBUG_PROPERTY, "false"))) {
      try {
        return new TraceClassVisitor(visitor, new ASMifier(), new PrintWriter(System.out));
      } catch (NoClassDefFoundError e) {
        System.err.format("script.test.debug=true requires org.ow2.asm:asm-util in CLASSPATH");
        e.printStackTrace(System.err);
      }
    }
    return visitor;
  }

  /**
   * Determines the seed value to use for a test's {@code Random} instance.
   * <p>
   * The value of the {@code script.test.seed} system property is used if set.
   * @return the seed to use
   */
  private static long getSeed() {
    String seedValue = System.getProperty(SCRIPT_TEST_SEED_PROPERTY);
    if (seedValue == null) {
      return SEEDS.nextLong();
    } else {
      try {
        return Long.parseLong(seedValue);
      } catch (NumberFormatException e) {
        throw new AssertionError("Error parsing value of " + SCRIPT_TEST_SEED_PROPERTY + "=" + seedValue, e);
      }
    }
  }

  /**
   * Executes a script capturing {@code stdout} and {@code stderr} output.
   * <p>
   * This method sets {@code JAVA_HOME} to the current value of the {@code java.home} property.
   * @param workingDirectory the directory to establish as the working directory ({@code user.dir})
   *                         for the script execution
   * @param timeout the amount of time permitted for the script execution
   * @param environment the values of any environment variables to set for the script execution;
   *                    values specified here replace current values <i>including</i>
   *                    {@code JAVA_HOME}, if included
   * @param command the script command to execute; the value must be properly quoted for the operating
   *                system environment as required by {@link ProcessBuilder#start()}
   * @param arguments the arguments to pass to the script; the elements must be properly quoted for
   *                  the operating system environment as required by {@link ProcessBuilder#start()}
   * @return a new {@code ScriptResult} instance capturing the execution output
   * @throws IOException if {@link ProcessBuilder#start()} fails for the script
   * @throws InterruptedException if awaiting script completion is interrupted
   */
  protected final ScriptResult execScript(File workingDirectory, Duration timeout, Map<String, String> environment, String command, String... arguments)
      throws IOException, InterruptedException {

    String[] commandLine = new String[1 + arguments.length];
    commandLine[0] = command;
    arraycopy(arguments, 0, commandLine, 1, arguments.length);

    ProcessBuilder builder = new ProcessBuilder(commandLine);
    Map<String, String> processEnvironment = builder.environment();
    processEnvironment.put("JAVA_HOME", System.getProperty("java.home"));
    processEnvironment.putAll(environment);
    builder.directory(workingDirectory);
    Process process = builder.start();

    StreamCollector stdout = new StreamCollector(process.getInputStream());
    stdout.start();
    StreamCollector stderr = new StreamCollector(process.getErrorStream());
    stderr.start();

    long nanoTimeout = timeout.toNanos();
    long waitStart = System.nanoTime();
    if (!process.waitFor(nanoTimeout, TimeUnit.NANOSECONDS)) {
      throw new AssertionError("Completion time exceeded for " + Arrays.toString(commandLine));
    }

    long deadline = nanoTimeout + waitStart;

    TimeUnit.NANOSECONDS.timedJoin(stdout, deadline - System.nanoTime());
    if (stdout.isAlive()) {
      throw new AssertionError("Completion time exceeded for " + Arrays.toString(commandLine));
    }

    TimeUnit.NANOSECONDS.timedJoin(stderr, deadline - System.nanoTime());
    if (stderr.isAlive()) {
      throw new AssertionError("Completion time exceeded for " + Arrays.toString(commandLine));
    }

    return new ScriptResult(process.exitValue(), stdout.toString(), stderr.toString());
  }

  /**
   * Writes the output captured in a {@link ScriptResult} instance to the {@code PrintStream} provided.
   * @param out the {@code PrintStream} to which the {@code ScriptResult} capture is written
   * @param t the {@code Throwable} leading to calling this method
   * @param scriptResult the {@code ScriptResult} holding the output to show
   */
  protected void showFailureOutput(PrintStream out, Throwable t, ScriptResult scriptResult) {
    out.format("Script execution failed: %s%n", t.getMessage());
    if (scriptResult != null) {
      try {
        showScriptOutput(out, scriptResult);
      } catch (Throwable x) {
        t.addSuppressed(x);
      }
    } else {
      out.println("No captured stdout/stderr available; ScriptResult not available");
    }
  }

  /**
   * Writes the output captured  in a {@link ScriptResult} instance to the {@code PrintStream} provided.
   * @param out the {@code PrintStream} to which the {@code ScriptResult} capture is written
   * @param scriptResult the {@code ScriptResult} holding the output to show
   */
  protected final void showScriptOutput(PrintStream out, ScriptResult scriptResult) {
    if (scriptResult != null) {
      out.println("---- Captured STDOUT ----");
      out.println(scriptResult.getStdoutAsString());
      out.println("---- Captured STDERR ----");
      out.println(scriptResult.getStderrAsString());
    } else {
      out.println("No captured stdout/stderr available; ScriptResult not available");
    }
  }


  /**
   * Generates a string containing non-special characters supported by the current operating system.
   * The first and last characters are of the unrestricted set.
   * @param rnd a {@code Random} instance used for character selection
   * @param length the length of the segment to generate
   * @return a string of the specified length containing non-special characters
   */
  private static String generateStandardSegment(Random rnd, int length) {
    assert length >= 1 : "length must be greater than or equal to 1";
    char[] segment = new char[length];
    segment[0] = UNRESTRICTED_STANDARD_CHARACTERS[rnd.nextInt(UNRESTRICTED_STANDARD_CHARACTERS.length)];

    for (int i = 1; i < length - 1; i++) {
      segment[i] = ALL_STANDARD_CHARACTERS[rnd.nextInt(ALL_STANDARD_CHARACTERS.length)];
    }

    segment[length - 1] = UNRESTRICTED_STANDARD_CHARACTERS[rnd.nextInt(UNRESTRICTED_STANDARD_CHARACTERS.length)];
    return String.valueOf(segment);
  }

  /**
   * Generates a string containing special characters supported by the current operating system.
   * The first and last characters are of the unrestricted set.
   * @param rnd a {@code Random} instance used for character selection
   * @param length the length of the segment to generate
   * @return a string of the specified length containing special characters
   */
  private static String generateSpecialSegment(Random rnd, int length) {
    assert length >= 1 : "length must be greater than or equal to 1";
    char[] segment = new char[length];
    segment[0] = UNRESTRICTED_STANDARD_CHARACTERS[rnd.nextInt(UNRESTRICTED_STANDARD_CHARACTERS.length)];

    for (int i = 1; i < length - 1; i++) {
      segment[i] = CURRENT_OPERATING_SYSTEM_SPECIAL_CHARACTERS[rnd.nextInt(CURRENT_OPERATING_SYSTEM_SPECIAL_CHARACTERS.length)];
    }

    segment[length - 1] = UNRESTRICTED_STANDARD_CHARACTERS[rnd.nextInt(UNRESTRICTED_STANDARD_CHARACTERS.length)];
    return String.valueOf(segment);
  }


  private static final class ClassStream extends InputStream {
    private final InputStream classByteStream;
    private final long lastModifiedTime;

    private ClassStream(InputStream classByteStream, long lastModifiedTime) {
      this.classByteStream = classByteStream;
      this.lastModifiedTime = lastModifiedTime;
    }

    public long lastModifiedTime() {
      return lastModifiedTime;
    }

    @Override
    public int read() throws IOException {
      return classByteStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
      return classByteStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return classByteStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
      return classByteStream.skip(n);
    }

    @Override
    public int available() throws IOException {
      return classByteStream.available();
    }

    @Override
    public void mark(int readlimit) {
      classByteStream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
      classByteStream.reset();
    }

    @Override
    public boolean markSupported() {
      return classByteStream.markSupported();
    }

    @Override
    public void close() throws IOException {
      classByteStream.close();
    }
  }

  /**
   * Identifies the operating system as relevant to file name character testing.
   */
  protected enum OperatingSystem {
    /** Microsoft Windows. */
    WINDOWS {
      @Override
      public String quoteCommand(String command) {
        return "\"" + command + "\"";
      }

      @Override
      public String appendScriptExtension(String command) {
        return command + ".bat";
      }
    },
    /** Unix variants including Mac OS X. */
    UNIX {
      @Override
      public String quoteCommand(String command) {
        return command;
      }

      @Override
      public String appendScriptExtension(String command) {
        return command + ".sh";
      }
    },
    /** Systems neither Windows not *NIX. */
    OTHER {
      @Override
      public String quoteCommand(String command) {
        throw new UnsupportedOperationException("OperatingSystem.OTHER.quoteCommand not implemented");
      }

      @Override
      public String appendScriptExtension(String command) {
        throw new UnsupportedOperationException("OperatingSystem.OTHER.appendScriptExtension not implemented");
      }
    }
    ;

    public abstract String quoteCommand(String command);
    public abstract String appendScriptExtension(String command);

    static EnumSet<OperatingSystem> NONE = EnumSet.noneOf(OperatingSystem.class);

    /**
     * Identify the current operating system.
     * @return the current {@code OperatingSystem}
     */
    static OperatingSystem currentOperatingSystem() {
      String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
      if (osName.contains("windows")) {
        return OperatingSystem.WINDOWS;
      } else if (osName.contains("linux")) {
        return OperatingSystem.UNIX;
      } else if (osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")) {
        return OperatingSystem.UNIX;
      } else if (osName.contains("sunos") || osName.contains("solaris")) {
        return OperatingSystem.UNIX;
      } else if (osName.contains("freebsd")) {
        return OperatingSystem.UNIX;
      }
      return OperatingSystem.OTHER;
    }
  }

  private static CharDef def(char character, String identifier, OperatingSystem... unsupportedOs) {
    return new CharDef(character, identifier, unsupportedOs);
  }

  private static CharDef def(char character, String identifier, EnumSet<OperatingSystem> unsupportedOs) {
    return new CharDef(character, identifier, unsupportedOs);
  }

  private static final class CharDef {
    private final char character;
    private final String identifier;
    private final EnumSet<OperatingSystem> supportedOs;

    private CharDef(char character, String identifier, EnumSet<OperatingSystem> supportedOs) {
      this.character = character;
      this.identifier = identifier;
      this.supportedOs = supportedOs;
    }

    private CharDef(char character, String identifier, OperatingSystem... unsupportedOs) {
      this(character, identifier, composeSupportedOs(unsupportedOs));
    }

    private static EnumSet<OperatingSystem> composeSupportedOs(OperatingSystem[] unsupportedOs) {
      if (unsupportedOs.length == 0) {
        return EnumSet.allOf(OperatingSystem.class);
      } else {
        EnumSet<OperatingSystem> supported = EnumSet.allOf(OperatingSystem.class);
        supported.removeAll(Arrays.asList(unsupportedOs));
        return supported;
      }
    }

    @Override
    public String toString() {
      return "CharDef{" + character + ", '" + identifier + '\'' + ", " + supportedOs + '}';
    }
  }

  /**
   * Holds the results of a script execution via {@link #execScript(File, Duration, Map, String, String...)}.
   * @see ScriptTestUtil#showProperties(PrintStream)
   * @see ScriptTestUtil#showEnvironment(PrintStream)
   * @see ScriptTestUtil#showArguments(PrintStream, String[])
   */
  protected static final class ScriptResult {
    private final int code;
    private final String stdout;
    private final String stderr;

    public ScriptResult(int code, String stdout, String stderr) {
      this.code = code;
      this.stdout = stdout;
      this.stderr = stderr;
    }

    /**
     * Gets the completion code.
     * @return the script execution's completion code
     */
    public int getCode() {
      return code;
    }

    /**
     * Gets a {@code String} containing the lines captured from {@code stdout}.
     * @return the script execution {@code stdout} content
     */
    public String getStdoutAsString() {
      return stdout;
    }

    /**
     * Return a map of the properties emitted to {@code stdout} by {@link ScriptTestUtil#showProperties}.
     * @return a {@code Map}, possibly empty, of the properties in {@code stdout}
     */
    public Map<String, String> properties() {
      return extractProperties(stdout);
    }

    /**
     * Return a map of the environment variables emitted to {@code stdout} by {@link ScriptTestUtil#showEnvironment}.
     * @return a {@code Map}, possibly empty, of the environment variables in {@code stdout}
     */
    public Map<String, String> environment() {
      return extractEnvironment(stdout);
    }

    /**
     * Return argument array emitted to {@code stdout} by {@link ScriptTestUtil#showArguments}.
     * @return a {@code String[]}, possibly empty, of the arguments in {@code stdout}
     */
    public String[] arguments() {
      return extractArguments(stdout);
    }

    /**
     * Gets a {@code String} containing the lines captured from {@code stderr}.
     * @return the script execution {@code stderr} content
     */
    public String getStderrAsString() {
      return stderr;
    }
  }
}
