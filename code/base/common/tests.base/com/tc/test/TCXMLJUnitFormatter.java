/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

import org.w3c.dom.Text;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.taskdefs.optional.junit.XMLConstants;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.BuildException;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.AssertionFailedError;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.util.Properties;
import java.util.Enumeration;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.BufferedWriter;

/**
 * This formatter is used to generate a 'pre-condition' log --- if the TestCase shuts the VM down (eg: deliberately
 * calling System.exit()) then we'll at least have a log file that indicate that the TestSuite did not complete
 * gracefully, with a record of which TestCase was last run...
 *
 * @author Juris Galang
 */
public class TCXMLJUnitFormatter
   implements JUnitResultFormatter, XMLConstants {

  /**
   */
  private JUnitTest           suite;

  /**
   */
  private File                logfile;

  /**
   * The XML document.
   */
  private Document            doc;

  /**
   * The wrapper for the whole testsuite.
   */
  private Element             rootElement;

  /** constant for unnnamed testsuites/cases */
  private static final String UNKNOWN = "unknown";

  /**
   */
  public void startTest(Test test) {
    //logfile = new File("TEST-" + this.suite.getName() + "-" + ((TestCase) test).getName() + ".xml");
    logfile = new File("TEST-" + this.suite.getName() + ".xml");

    loadDocument();
    Throwable t = new TestSuiteAbortedException("TestSuite '" + suite.getName()
                                                + "' abnormally terminated in test case: '"
                                                + ((TestCase) test).getName() + "'");
    Element nested = doc.createElement(FAILURE);
    Element currentTest = rootElement;

    currentTest.appendChild(nested);

    String message = t.getMessage();
    if (message != null && message.length() > 0) {
      nested.setAttribute(ATTR_MESSAGE, t.getMessage());
    }
    nested.setAttribute(ATTR_TYPE, t.getClass().getName());

    String strace = JUnitTestRunner.getFilteredTrace(t);
    Text trace = doc.createTextNode(strace);
    nested.appendChild(trace);
    saveDocument();
  }

  /**
   */
  public void endTest(Test test) {
    logfile.delete();
  }

  private void loadDocument() {
    try {
      doc = logfile.exists() ? getDocumentBuilder().parse(logfile) : getDocumentBuilder().newDocument();
      if (logfile.exists()) {
        rootElement = (Element) doc.getElementsByTagName(TESTSUITE).item(0);
        return;
      }

      rootElement = doc.createElement(TESTSUITE);
      String n = suite.getName();
      rootElement.setAttribute(ATTR_NAME, n == null ? UNKNOWN : n);

      Element propsElement = doc.createElement(PROPERTIES);
      rootElement.appendChild(propsElement);
      Properties props = suite.getProperties();
      if (props != null) {
        Enumeration e = props.propertyNames();
        while (e.hasMoreElements()) {
          String name = (String) e.nextElement();
          Element propElement = doc.createElement(PROPERTY);
          propElement.setAttribute(ATTR_NAME, name);
          propElement.setAttribute(ATTR_VALUE, props.getProperty(name));
          propsElement.appendChild(propElement);
        }
      }
    } catch (java.io.IOException ioe) {
      throw new BuildException("Unable to append to log file", ioe);
    } catch (org.xml.sax.SAXException saxe) {
      throw new BuildException("Unable to append to log file", saxe);
    }
  }

  private void saveDocument() {
    rootElement.setAttribute(ATTR_TESTS, "" + suite.runCount());
    rootElement.setAttribute(ATTR_FAILURES, "" + (suite.failureCount() + 1));
    rootElement.setAttribute(ATTR_ERRORS, "" + suite.errorCount());
    rootElement.setAttribute(ATTR_TIME, "" + (suite.getRunTime() / 1000.0));
    try {
      FileOutputStream out = new FileOutputStream(logfile, true);
      Writer wri = null;
      try {
        wri = new BufferedWriter(new OutputStreamWriter(out, "UTF8"));
        wri.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        (new DOMElementWriter()).write(rootElement, wri, 0, "  ");
        wri.flush();
        rootElement = null;
      } catch (IOException exc) {
        throw new BuildException("Unable to write log file", exc);
      } finally {
        if (wri != null) {
          try {
            wri.close();
          } catch (IOException e) {
            // ignore
          }
        }
      }
    } catch (FileNotFoundException fnfe) {
      throw new BuildException("Unable to create log file", fnfe);
    }
  }

  /**
   */
  public void startTestSuite(JUnitTest test) {
    this.suite = test;
  }

  /**
   */
  public void endTestSuite(JUnitTest test) {
    //
  }

  private static DocumentBuilder getDocumentBuilder() {
    try {
      return DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch (Exception exc) {
      throw new ExceptionInInitializerError(exc);
    }
  }

  private class TestSuiteAbortedException extends Throwable {
    public TestSuiteAbortedException(String message) {
      super(message);
    }

    public String getMessage() {
      return super.getMessage();
    }

  }

  public void setOutput(OutputStream out) {
    //
  }

  public void setSystemOutput(String out) {
    //
  }

  public void setSystemError(String out) {
    //
  }

  public void addError(Test test, java.lang.Throwable t) {
    //
  }

  public void addFailure(Test test, AssertionFailedError t) {
    //
  }
} // TCXMLJUnitFormatter

