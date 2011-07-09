package com.tc.test;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;

public class CheckListTestSuiteBase {
	
	@SuppressWarnings("unchecked")
	public static Test suite()  {
		String basedir = System.getProperty("basedir");
		String listName = System.getProperty("listName", "checkshort.txt");
		File listFileName = new File(basedir + File.separator + listName);
		Assert.assertTrue(listFileName.exists());
		System.out.println("Running tests from list " + listName);
		FileReader reader = null;
		try {
			reader = new FileReader(listFileName);
			List<String> tests = IOUtils.readLines(reader);
			TestSuite suite = new TestSuite();
			for (String test : tests) {
				test = test.trim();
				if (test.startsWith("#") || test.length() == 0) {
					continue;
				}
				suite.addTestSuite((Class<? extends TestCase>) Class.forName(test));
			}
			return suite;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}
	
}