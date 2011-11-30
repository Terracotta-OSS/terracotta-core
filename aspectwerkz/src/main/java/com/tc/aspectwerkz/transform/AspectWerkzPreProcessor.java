/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform;

import com.tc.aspectwerkz.expression.SubtypePatternType;
import com.tc.aspectwerkz.expression.regexp.Pattern;
import com.tc.aspectwerkz.expression.regexp.TypePattern;
import com.tc.aspectwerkz.hook.ClassPreProcessor;
import com.tc.aspectwerkz.transform.inlining.EmittedJoinPoint;
import com.tc.aspectwerkz.util.Util;
// import com.tc.object.bytecode.hook.ClassProcessor;

/**
 * AspectWerkzPreProcessor is the entry point of the AspectWerkz layer 2. <p/>It implements the ClassPreProcessor
 * interface defined in layer 1. <p/>Available options are:
 * <ul>
 * <li><code>-Daspectwerkz.transform.verbose=yes</code> turns on verbose mode: print on stdout all non filtered class
 * names and which transformation are applied</li>
 * <li><code>-Daspectwerkz.transform.dump=org.myapp.*</code> dumps transformed class matching pattern <i>org.myapp.*
 * </i>(even unmodified ones) in <i>./_dump </i> directory (relative to where applications starts). The syntax
 * <code>-Daspectwerkz.transform.dump=*</code> matchs all classes. The pattern language is the same as pointcut
 * pattern language.</li>
 * <li>else <code>-Daspectwerkz.transform.dump=org.myapp.*,before</code> dumps class before and after the
 * transformation whose name starts with <i>org.myapp. </i>(even unmodified ones) in <i>./_dump/before </i> and
 * <i>./_dump/after </i> directories (relative to where application starts)</li>
 * <li><code>-Daspectwerkz.transform.filter=no</code> (or false) disables filtering of
 * <code>com.tc.aspectwerkz</code> and related classes (trove, dom4j etc.). This should only be used in offline
 * mode where weaving of those classes is needed. Setting this option in online mode will lead to
 * <code>ClassCircularityError</code>.</li>
 * </ul>
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class AspectWerkzPreProcessor implements ClassPreProcessor {

  private final static String AW_TRANSFORM_FILTER = "aspectwerkz.transform.filter";

  private final static String AW_TRANSFORM_VERBOSE = "aspectwerkz.transform.verbose";

  private final static String AW_TRANSFORM_DETAILS = "aspectwerkz.transform.details";

  private final static String AW_TRANSFORM_GENJP = "aspectwerkz.transform.genjp";

  private final static String AW_TRANSFORM_DUMP = "aspectwerkz.transform.dump";

  public final static TypePattern DUMP_PATTERN;

  private final static boolean NOFILTER;                                              // TODO: not used, remove?

  public final static boolean DUMP_BEFORE;

  public final static boolean DUMP_AFTER;

  public static final String DUMP_DIR_BEFORE;

  public static final String DUMP_DIR_AFTER;

  public final static boolean VERBOSE;

  public final static boolean DETAILS;

  public final static boolean GENJP;

  static {
    // define the tracing and dump options
    String verbose = System.getProperty(AW_TRANSFORM_VERBOSE, null);
    VERBOSE = "yes".equalsIgnoreCase(verbose) || "true".equalsIgnoreCase(verbose);
    String details = System.getProperty(AW_TRANSFORM_DETAILS, null);
    DETAILS = "yes".equalsIgnoreCase(details) || "true".equalsIgnoreCase(details);
    String genjp = System.getProperty(AW_TRANSFORM_GENJP, null);
    GENJP = "yes".equalsIgnoreCase(genjp) || "true".equalsIgnoreCase(genjp);
    String filter = System.getProperty(AW_TRANSFORM_FILTER, null);
    NOFILTER = "no".equalsIgnoreCase(filter) || "false".equalsIgnoreCase(filter);
    String dumpPattern = System.getProperty(AW_TRANSFORM_DUMP, null);
    if (dumpPattern == null) {
      DUMP_BEFORE = false;
      DUMP_AFTER = false;
      DUMP_PATTERN = null;
    } else {
      dumpPattern = dumpPattern.trim();
      DUMP_AFTER = true;
      DUMP_BEFORE = dumpPattern.indexOf(",before") > 0;
      if (DUMP_BEFORE) {
        DUMP_PATTERN = Pattern.compileTypePattern(dumpPattern.substring(0, dumpPattern.indexOf(',')),
                SubtypePatternType.NOT_HIERARCHICAL);
      } else {
        DUMP_PATTERN = Pattern.compileTypePattern(dumpPattern, SubtypePatternType.NOT_HIERARCHICAL);
      }
    }
    DUMP_DIR_BEFORE = "_dump/before";
    DUMP_DIR_AFTER = AspectWerkzPreProcessor.DUMP_BEFORE ? "_dump/after" : "_dump";
  }

  /**
   * Marks the pre-processor as initialized.
   */
  private boolean m_initialized = false;

  /**
   * Pre processor weaving strategy.
   */
  // private ClassProcessor m_classProcessor;

  /**
   * Initializes the transformer stack.
   */
  public void initialize() {
//    try {
//      m_classProcessor = DSOContextImpl.createGlobalContext();
//      m_initialized = true;
//    } catch (ConfigurationSetupException e) {
//      throw new WrappedRuntimeException(e);
//    }
    throw new RuntimeException("NOT merged with the changed code yet");
  }

  /**
   * Transform bytecode according to the transformer stack Adapted for embedded modes, that will filter out framework
   * classes See preProcessWithOutput for a tool entry point.
   *
   * @param name     class name
   * @param bytecode bytecode to transform
   * @param loader   classloader loading the class
   * @return modified (or not) bytecode
   */
  public byte[] preProcess(final String name, final byte[] bytecode, final ClassLoader loader) {
    // filter out ExtClassLoader and BootClassLoader
    if (!NOFILTER) {
      if ((loader == null) || (loader.getParent() == null)) {
        return bytecode;
      }
    }

    // needed for JRockit (as well as all in all TFs)
    final String className = (name != null) ? name.replace('/', '.') : null;

    // will filter null named classes
    if (filter(className) || !m_initialized) {
      return bytecode;
    }
    if (VERBOSE) {
      log(Util.classLoaderToString(loader) + ':' + className + '[' + Thread.currentThread().getName() + ']');
    }

    try {
      InstrumentationContext context = _preProcess(className, bytecode, loader);
      return context.getCurrentBytecode();
    } catch (Exception e) {
      log("failed " + className);
      e.printStackTrace();
      return bytecode;
    }
  }

  /**
   * Weaving of the class
   *
   * @param className
   * @param bytecode
   * @param loader
   * @return the weaving context, where getCurrentBytecode is the resulting bytecode
   */
  public InstrumentationContext _preProcess(final String className, final byte[] bytecode, final ClassLoader loader) {

    // FIXME now I am creating 2 contexts: one here and one directly in the preProcess method - FIX
    final InstrumentationContext context = new InstrumentationContext(className, bytecode, loader);

    // dump before (not compliant with multiple CL weaving same class differently, since based
    // on class FQN className)
    dumpBefore(className, context);

    // do the transformation
    final byte[] transformedBytes = bytecode; // m_classProcessor.preProcess(className, bytecode, 0, bytecode.length, loader);

    context.setCurrentBytecode(transformedBytes);

    // FIXME when and how to call the postProcess????

    // dump after as required
    dumpAfter(className, context);

    // return the transformed bytecode
    return context;
  }

  /**
   * Weaving without filtering any class and returning a rich object with emitted joinpoints
   *
   * @param name
   * @param bytecode
   * @param loader
   * @return
   */
  public Output preProcessWithOutput(final String name, final byte[] bytecode, final ClassLoader loader) {
    // needed for JRockit (as well as all in all TFs)
    final String className = name.replace('/', '.');

    // we do not filter anything except JP in this mode
    if (name.endsWith((TransformationConstants.JOIN_POINT_CLASS_SUFFIX))) {
      Output output = new Output();
      output.bytecode = bytecode;
      output.emittedJoinPoints = null;
      return output;
    }

    InstrumentationContext context = _preProcess(className, bytecode, loader);
    Output output = new Output();
    output.bytecode = context.getCurrentBytecode();
    output.emittedJoinPoints = (EmittedJoinPoint[]) context.getEmittedJoinPoints()
            .toArray(new EmittedJoinPoint[0]);

    // resolve line numbers
    for (int i = 0; i < output.emittedJoinPoints.length; i++) {
      EmittedJoinPoint emittedJoinPoint = output.emittedJoinPoints[i];
      emittedJoinPoint.resolveLineNumber(context);
    }
    return output;
  }

  /**
   * Logs a message.
   *
   * @param msg the message to log
   */
  public static void log(final String msg) {
    if (VERBOSE) {
      System.out.println(msg);
    }
  }

  /**
   * Excludes instrumentation for the class used during the instrumentation
   *
   * @param klass the AspectWerkz class
   */
  private static boolean filter(final String klass) {
    return (klass == null) || klass.endsWith("_AWFactory")// TODO AVF refactor
            || klass.endsWith(TransformationConstants.JOIN_POINT_CLASS_SUFFIX)
            || klass.startsWith("com.tc.aspectwerkz.")
            || klass.startsWith("com.tc.asm.")
            || klass.startsWith("com.tc.jrexx.")
            || klass.startsWith("com.bluecast.")
            || klass.startsWith("gnu.trove.")
            || klass.startsWith("org.dom4j.")
            || klass.startsWith("org.xml.sax.")
            || klass.startsWith("javax.xml.parsers.")
            || klass.startsWith("sun.reflect.Generated")// issue on J2SE 5 reflection - AW-245
            || klass.startsWith("EDU.oswego.cs.dl.util.concurrent");
  }

  /**
   * Dumps class before weaving.
   *
   * @param className
   * @param context
   */
  public static void dumpBefore(final String className, final InstrumentationContext context) {
    if (DUMP_BEFORE) {
      if (DUMP_PATTERN.matches(className)) {
        context.dump("_dump/before/");
      }
    }
  }

  /**
   * Dumps class after weaving.
   *
   * @param className
   * @param context
   */
  public static void dumpAfter(final String className, final InstrumentationContext context) {
    if (DUMP_AFTER) {
      if (DUMP_PATTERN.matches(className)) {
        context.dump("_dump/" + (DUMP_BEFORE ? "after/" : ""));
      }
    }
  }

  /**
   * Structure build when invoking tool weaving
   */
  public static class Output {
    public byte[] bytecode;
    public EmittedJoinPoint[] emittedJoinPoints;
  }
}