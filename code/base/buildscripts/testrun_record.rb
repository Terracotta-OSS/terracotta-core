#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Contains a set of classes that allow us to examine the XML files generated
# by JUnit when it runs our tests, and create a set of objects that summarizes
# the key information contained in them. Then, we can print this information
# out to the screen, use it to fail the build (or whatever) as necessary, or
# even serialize it to disk for later fast retrieval.
#
# Note: there is a critical difference between 'testrun_results' and
# 'testrun_record'. 'testrun_results' understands where we should store
# test results, temporary files, Ant property files, and so on; in other words,
# it is used at test-run time to figure out where to store everything.
# 'testrun_record' is used after the fact, to analyze a set of previously-
# performed test runs (in fact, they could've been run days ago; it reads 
# everything from disk, not from memory). 
#
# There is, however, one important exception here: the setUp and tearDown
# methods on TestRunRecord are actually called before and after we run tests,
# respectively. This is because TestRunRecord needs a little extra information,
# above and beyond what the rest of the system would ordinarily use, to do its
# bookkeeping; this lets TestRunRecord write out that information as appropriate.
# This is the only exception, though, and the rest of this file is all about
# analyzing previously-run tests from files stored on disk.

require 'rexml/document'
require 'parsedate'
require 'java'

# Right now, JRuby's implementation of marshalling has too many bugs for it to be
# usable here, alas. Once it's fixed we can re-enable this. 
TESTRUN_RECORD_MARSHALLING_ENABLED = false

# Takes a boolean argument, failed, and a string, s, and returns a string that's either
#   [FAILED] s
# or
#   [PASSED] s
# depending on whether the first argument is true or false, respectively.
def tag_passfail(failed, s)
  "[%s] %s" % [ failed ? 'FAILED' : 'PASSED', s ]
end

# Represents a testsuite run (i.e., a single XML output file -- the results of a single
# JUnit class) that can't be parsed. Typically, this happens when the result file is 0
# bytes long, because the JVM crashed or otherwise puked while trying to write it out,
# but this can also happen if the XML file just plain gets corrupt or for some similar
# reason.
class UnparseableTestSuiteRunRecord
  attr_reader :name
    
  # Creates a new instance. filename is the full path to the file, file_classname is
  # the name of the test class that (should be) represented by that file, and
  # exception is the exception we got when we tried to parse the file.
  def initialize(filename, file_classname, exception)
    @filename = filename
    @name = file_classname
    @exception = exception
  end
    
  def to_s
    tag_passfail(true, "%s: Result XML file ('%s') is unparseable (!); got exception: %s (%s)" % [ @name, @filename.to_s, @exception.message, @exception.backtrace.join("\n") ])
  end
    
  # A human-readable string representing this record, in detail.
  def dump
    to_s
  end
    
  # What clas was this record for?
  def classname
    @name
  end
    
  # Did this record fail? Unparseable records are always considered to have failed.
  def failed?
    true
  end
end

# Represents a normal testsuite run (i.e., a single XML output file -- the results of a single
# JUnit class). 
#
# We parse the XML using the Java DOM API, rather than REXML (which is the 'usual' Ruby API
# for doing XML parsing). This is unfortunate, because REXML is about eight billion times 
# cleaner and easier to use than the fucking DOM API, but REXML, running on top of JRuby --
# which is interpreted right now, not compiled to Java bytecode -- on top of the JVM is just
# way too slow. Maybe once JRuby starts being compiled to Java bytecode, this will be
# better.
class TestSuiteRunRecord
  attr_reader :name

  include_class('javax.xml.parsers.DocumentBuilderFactory') { |p, name| "Java" + name }
  include_class('java.io.File') { |p, name| "Java" + name }
  include_class('org.w3c.dom.Node') { |p, name| "Java" + name }
  include_class('org.w3c.dom.Text') { |p, name| "Java" + name }
  include_class('org.w3c.dom.Element') { |p, name| "Java" + name }
    
  # These are constants copied from org.w3c.dom.Node. Yes, this is lame, but there seems
  # to currently be no way to easily access constants (public static final class members)
  # of Java classes from JRuby.
  NODE_TYPE_ELEMENT = 1

  # These are constants copied from org.w3c.dom.Node. Yes, this is lame, but there seems
  # to currently be no way to easily access constants (public static final class members)
  # of Java classes from JRuby.
  NODE_TYPE_TEXT = 3
    
  # Creates a TestSuiteRunRecord from a file, which should be a standard JUnit result
  # XML file.
  def self.from_file(file)
    file = FilePath.new(file.to_s)
    out = nil
        
    # Make sure this filename looks like the name of a standard JUnir result XML file.
    if file.filename =~ /^TEST-(\S+)\.xml$/i
      file_classname = $1

      # We're using Java here, instead of REXML, because Java is *vastly* faster.
      # REXML is beautiful but very slow when running in Ruby-interpeted-on-top-of-Java-
      # interpreted-on-top-of-CPU. 
      builder_factory = JavaDocumentBuilderFactory.newInstance
      builder = builder_factory.newDocumentBuilder
      document = builder.parse(JavaFile.new(file.to_s))
            
      xml_classname = document.getDocumentElement.getAttribute("name")

      if file_classname != xml_classname
        puts :warn, "File '%s' should be for class '%s', but it's actually for class '%s'. Assuming it's actually for class '%s'." % 
        [ xml_file, file_classname, xml_classname, xml_classname ]
      end
            
      out = TestSuiteRunRecord.new(file.to_s, document)
    end
        
    out
  end
    
  # Creates a new instance, based on the filename of the JUnit XML log file (log_filename)
  # and the Java DOM org.w3c.dom.Document object.
  def initialize(log_filename, document)
    @log_filename = log_filename
        
    root_element = document.getDocumentElement
        
    @failures = root_element.getAttribute("failures").to_i
    @errors = root_element.getAttribute("errors").to_i
    @name = root_element.getAttribute("name")
    @tests = root_element.getAttribute("tests").to_i
        
    failed_tests = find_failures_and_errors(root_element.getElementsByTagName("testcase"))
    failed_suites = find_failures_and_errors(document.getElementsByTagName("testsuite"))
        
    @failed_tests = collect_failures(failed_tests)
    @failed_suites = collect_failures(failed_suites)
  end
    
  # What's the name of this class?
  def classname
    @name
  end
    
  # How many tests got run?
  def total_tests
    @tests
  end
    
  # How many tests passed?
  def total_passed
    @tests - (@failures + @errors)
  end
    
  # Did any tests fail?
  def failed?
    (@failures + @errors) > 0
  end
    
  # Return a full, detailed representation of this record, as a string.
  def dump
    out = to_s
        
    if failed?
      out += "\n\n"
      @failed_tests.merge(@failed_suites).each do |name, text|
        out += "    %s FAILED:\n" % name
        out += "\n        log: %s\n\n" % @log_filename.to_s
        out += text
        out += "\n"
      end
      out = out.strip
    end
        
    out
  end
    
  # A string that summarizes this object.
  def to_s
    tag_passfail(failed?, @name)
  end
    
  private
  # Does the given DOM Element have at least one child element with a name that's
  # any of the names in the array names?
  def has_child_element_with_name(element, names)
    child_list = element.getChildNodes
    has_one = false
        
    0.upto(child_list.getLength - 1) do |index|
      child = child_list.item(index)
      has_one = true if child.getNodeType == NODE_TYPE_ELEMENT && names.include?(child.getNodeName)
    end
        
    has_one
  end
    
  # Returns a list containing all the elements in the element_list array that
  # have child elements of 'failure' or 'error'.
  def find_failures_and_errors(element_list)
    out_list = [ ]
        
    0.upto(element_list.getLength - 1) do |index|
      element = element_list.item(index)
      out_list << element if has_child_element_with_name(element, [ "failure", "error" ])
    end
        
    out_list
  end
    
  # Returns a hash that maps strings of format fully.qualified.TestName.testMethod
  # to values that are the text of the failures of that test method. Methods that
  # did not fail are not included in the output hash.
  def collect_failures(list)
    out = { }
    list.each do |failure|
      unless failure.nil?
        full_name = "%s.%s" % [ failure.getAttribute("classname"), failure.getAttribute("name") ]
                
        text = extract_text_from(failure)
        out[full_name] = text.strip
      end
    end
    out
  end
    
  # Given a DOM Text or Element node, returns the text from that node. (For Element
  # nodes, this returns all the text from all child nodes.)
  def extract_text_from(node)
    out = ""
        
    if node.getNodeType == NODE_TYPE_TEXT
      out = node.getNodeValue.strip
    elsif node.getNodeType == NODE_TYPE_ELEMENT
      children = node.getChildNodes
      out = ""
      0.upto(children.getLength - 1) do |index|
        out += extract_text_from(children.item(index)).strip
      end
    else
      ""
    end
        
    out
  end
end

# Represents the results of all tests that were run on a particular subtree. This
# will therefore incorporate zero or more TestSuiteRunRecord objects.
class SubtreeTestRunRecord
  # Creates a new instance. module_testrun_directory is the directory containing
  # results of all tests for that module (arranged in a hierarchy according to
  # subtree name); subtree_name is the name of the subtree this record is for.
  def initialize(module_testrun_directory, subtree_name)        
    @subtree_name = subtree_name
    @subtree_testrun_directory = FilePath.new(module_testrun_directory, "%s.results" % subtree_name)
    @subtree_tempdir = FilePath.new(module_testrun_directory, "%s.temp" % subtree_name)
    @read = false
  end
    
  # Did any test in this subtree fail?
  def failed?
    read
    @failed
  end
    
  # Do any records for this subtree actually exist?
  def exist?
    @subtree_testrun_directory.exist?
  end
    
  # A detailed string, in human-readable format, representing the results for this subtree.
  def dump
    read
    out = to_s
        
    if failed?
      @testsuites.each do |name, testsuite|
        if testsuite.failed?
          out += "\n          " + testsuite.dump + "\n"
        end
      end
    end
        
    out
  end
    
  # The names of all classes that failed in this subtree, as an array.
  def failed_test_classnames
    read
    @testsuites.values.collect { |testsuite| testsuite.failed? ? testsuite.classname : nil }.delete_if { |elem| elem.nil? }
  end
    
  # A short human-readable string summarizing the results of this subtree.
  def to_s
    read
    tag_passfail(failed?, "Subtree '%s' (failed %d/%d tests, %d/%d test suites)" % [ @subtree_name, total_tests - total_passed,
        total_tests, total_suites - total_suites_passed, total_suites ])
  end
    
  # How many test *cases*, in total, passed?
  def total_passed
    read
    @total_passed
  end
    
  # How many test *cases*, in total, were run?
  def total_tests
    read
    @total_tests
  end
    
  # How many test *suites*, in total, passed?
  def total_suites_passed
    read
    @total_suites_passed
  end
    
  # How many test *suites*, in total, were run?
  def total_suites
    read
    @total_suites
  end
    
  private
  # Reads in all the XML files for tests in this subtree, analyzes them, and stores the
  # data in this object. This only will happen once (lazy-init), so it's always safe to
  # call this function. 
  def read
    unless @read
      @testsuites = { }
      @failed = false
      @total_passed = 0
      @total_tests = 0
      @total_suites_passed = 0
      @total_suites = 0
           
      if exist?
        Dir.open(@subtree_testrun_directory.to_s) do |dir|
          dir.each do |entry|
            if entry =~ /^TEST-(\S+)\.xml$/i
              file_classname = $1
                           
              filename = FilePath.new(@subtree_testrun_directory, entry)                                                 
              begin
                # A little output so users know we're actually doing something
                puts(".")
                               
                testsuite_run = TestSuiteRunRecord.from_file(filename)
                @testsuites[testsuite_run.name] = testsuite_run
                @total_tests += testsuite_run.total_tests
                @total_passed += testsuite_run.total_passed
                @total_suites += 1
                @total_suites_passed += 1 unless testsuite_run.failed?
              rescue => e
                testsuite_run = UnparseableTestSuiteRunRecord.new(filename, file_classname, e)
                @testsuites[file_classname] = testsuite_run
                @total_suites += 1
                @total_tests += 1 # unknown, set to 1 as default     
                create_abnormal_junit_report(filename.to_s, file_classname)
              end
                           
              @failed = @failed || testsuite_run.failed?
            end
          end
        end
      end
           
      @read = true 
    end
  end
end

# The results of all test runs for an entire module. This will therefore incorporate zero
# or more SubtreeTestRunRecord objects.
class ModuleTestRunRecord
    
  # Creates a new instance. testrun_directory is the directory for the entire test run;
  # module_name is the name of the module this object should represent.
  def initialize(testrun_directory, module_name)
    @module_name = module_name
    @module_testrun_directory = FilePath.new(testrun_directory, module_name)
    @read = false
  end

  # A human-readable string, describing this record, in detail.
  def dump
    read
    out = to_s
        
    @subtree_results.each do |name, subtree_result|
      out += "\n     " + subtree_result.dump + "\n" if subtree_result.total_suites > 0
    end
        
    out
  end
    
  # Did any test in this entire module fail?
  def failed?
    @failed
  end
      
  # A human-readable string, summarizing the contents of this record.
  def to_s
    read
    tag_passfail(failed?, "Module '%s' (failed %d/%d tests, %d/%d suites)" % [ @module_name, total_tests - total_passed,
        total_tests, total_suites - total_suites_passed, total_suites ])
  end
    
  # The names of any test classes that failed, as an array.
  def failed_test_classnames
    read
    @subtree_results.values.collect { |subtree_result| subtree_result.failed_test_classnames }.flatten.delete_if { |val| val.nil? }
  end
    
  # How many test *cases*, in total, passed?
  def total_passed
    read
    @total_passed
  end
    
  # How many test *cases*, in total, were run?
  def total_tests
    read
    @total_tests
  end

  # How many test *suites*, in total, passed?
  def total_suites_passed
    read
    @total_suites_passed
  end
    
  # How many test *suites*, in total, were run?
  def total_suites
    read
    @total_suites
  end
    
  private
  # Reads in all the XML files for tests in this module, analyzes them, and stores the
  # data in this object. This only will happen once (lazy-init), so it's always safe to
  # call this function. 
  def read
    unless @read
      @subtree_results = { }
      @failed = false
      @total_passed = 0
      @total_tests = 0
      @total_suites_passed = 0
      @total_suites = 0

      ALL_SUBTREE_NAMES.each do |subtree_name|
        subtree_record = SubtreeTestRunRecord.new(@module_testrun_directory, subtree_name)
                
        if subtree_record.exist?
          @subtree_results[subtree_name] = subtree_record
          @failed = @failed || subtree_record.failed?
          @total_passed += subtree_record.total_passed
          @total_tests += subtree_record.total_tests
          @total_suites_passed += subtree_record.total_suites_passed
          @total_suites = subtree_record.total_suites
        end
      end

      @read = true 
    end
  end
end

# Represents the results of an entire test run. This will therefore incorporate zero or
# more ModuleTestRunRecord objects.
class TestRunRecord
  # The name of the file into which we store a marshalled version of this object, for
  # later quick access.
  SAVED_FILENAME = 'test-run-record.marshalled'
    
  # Given a directory, search_directory, to search for test run directories (this is typically just the
  # 'build' directory), looks for the latest test run (the one with the highest number)
  # in it, creates a TestRunRecord for it, and returns it.
  def self.latest_record(search_directory, module_set)
    latest_num = nil
    latest_path = nil
        
    Dir.open(search_directory.to_s) do |dir|
      dir.each do |entry|
        if entry =~ /^\s*testrun-(\d+)\s*$/i
          latest_num = $1.to_i
          latest_path = FilePath.new(search_directory, entry)
        end
      end
    end
        
    out = nil
    unless latest_path.nil?
      out = for_directory(latest_path, module_set)
    end
        
    out
  end
    
  # Given a directory containing the results of a test run, creates a TestRunRecord
  # for it and returns it.
  def self.for_directory(directory, module_set)
    out = nil

    # We allow disabling of marshalling, because JRuby has too many bugs in its
    # marshalling code for now.
    if TESTRUN_RECORD_MARSHALLING_ENABLED
      saved_file = FilePath.new(directory, SAVED_FILENAME)
            
      # Try to open the marshalled file if we can, and just load data from there.
      # If anything goes wrong, just skip it, and reconstruct the data from the
      # original XML files instead.
      if FileTest.file?(saved_file.to_s)
        begin
          File.open(saved_file.to_s, "r") do |file|
            out = Marshal.load(file)
          end
          puts "Loaded testrun record directly from '%s'." % saved_file.to_s
        rescue => e
          puts :warn, "Attemped to load test run record from '%s', but got an exception ('%s'). Parsing XML files directly." % [ saved_file.to_s, e.to_s ]
        end
      end
    end
        
    # If we don't have a record yet, create one from the original XML files.
    out = TestRunRecord.new(directory, module_set) if out.nil?
        
    out
  end
    
  # Creates a new instance. testrun_directory is the directory containing the test run
  # we should analyze.
  def initialize(testrun_directory, module_set)
    @testrun_directory = testrun_directory
    @module_set = module_set
    @read = false
  end
    
  # Does some preparatory work -- writes the timestamp of when the testrun started, writes
  # out a description of the test set that we ran, and stores away the test-set description.
  # This is special -- it should be called *before* the actual test run itself, not at
  # analysis time.
  def setUp(test_set)
    set_timestamp(start_timestamp_file)
    write_test_set_description(test_set)
    @test_set_description = test_set.to_s
  end

  # Does some teardown work -- writes the timestamp of when the testrun ended, and writes
  # out a list of failed-test classnames (so that 'tcbuild recheck' works). This is special --
  # it should be called *after* the actual test run itself, not at analysis time.
  def tearDown
    set_timestamp(end_timestamp_file)
    write_failed_test_classnames
  end
    
  # Writes out a list of the names of all classes that had at least one failure; this is
  # read back in by 'tcbuild recheck' to re-run only failed tests.
  def write_failed_test_classnames
    names = failed_test_classnames
    File.open(failed_test_classnames_file.to_s, "w") do |file|
      names.each { |name| file << "%s\n" % name }
    end
  end
    
  # How many test *cases*, in total, passed?
  def total_passed
    read
    @total_passed
  end

  # How many test *cases*, in total, were run?
  def total_tests
    read
    @total_tests
  end
    
  # How many test *suites*, in total, passed?
  def total_suites_passed
    read
    @total_suites_passed
  end
    
  # How many test *suites*, in total, were run?
  def total_suites
    read
    @total_suites
  end
    
  # Did any test in the entire run fail?
  def failed?
    read
    @failed
  end
    
  # Returns an array containing the names of any classes that failed at least one of their tests.
  def failed_test_classnames
    if @failed_test_classnames.nil?
      if FileTest.file?(failed_test_classnames_file.to_s)
        File.open(failed_test_classnames_file.to_s, "r") do |file|
          @failed_test_classnames = [ ]
                    
          file.each do |line|
            @failed_test_classnames << line.strip
          end
        end
      else
        read
        @failed_test_classnames = @modules.values.collect { |module_record| module_record.failed_test_classnames }.flatten.delete_if { |elem| elem.nil? }
      end
    end
        
    @failed_test_classnames
  end
    
  # Returns a string summarizing the results of this test run, in human-readable format.
  def to_s
    out = tag_passfail(failed?, "Test Run in %s:\n         %s - %s\n         failed %d/%d tests, %d/%d suites" %
      [ @testrun_directory.to_s, time_string(start_time), time_string(end_time),
        total_tests - total_passed, total_tests, total_suites - total_suites_passed, total_suites ])
    out += "\n         (Run of %s)" % @test_set_description unless @test_set_description.nil?
    out
  end
    
  # Returns a string with human-readable details of this test run.
  def dump
    out = to_s
    out += "\n------------------------------------------------------------------------\n"
    if end_time.nil?
      puts ""
      puts :warn, "This test run did not complete; some tests may be missing. Use caution."
      puts :warn, "(The file '%s' does not exist.)" % end_timestamp_file.to_s
      puts ""
    end
        
    @modules.each do |name, record|
      out += "\n\n" + record.dump + "\n\n" if record.total_suites > 0
    end
        
    out
  end
    
  # Describes the directory that the tests were run in.
  def directory_description
    @testrun_directory.to_s
  end

  # When did this test run start?
  def start_time
    read
    @start_time
  end
    
  # When did this test run end?
  def end_time
    read
    @end_time
  end

  # Where should we put the list of classes that have failed tests?
  def failed_test_classnames_file
    FilePath.new(@testrun_directory, "failed-tests.txt")
  end
      
  private
  # If marshalling is enabled, and there's no marshalled version of this object on disk
  # yet, puts a copy there.
  def save_if_necessary
    saved_filename = FilePath.new(@testrun_directory, SAVED_FILENAME)
        
    if TESTRUN_RECORD_MARSHALLING_ENABLED
      unless @valid_on_disk
        File.open(saved_filename.to_s, "w") do |file|
          begin
            @valid_on_disk = true
            Marshal.dump(self, file)
            puts "Saved test run record to '%s'." % saved_filename.to_s
          rescue => e
            @valid_on_disk = false
            puts :warn, "Attempted to save test run record to '%s', but got an exception: %s" % [ saved_filename.to_s, e.to_s ]
          end
        end
      end
    end
  end

  # Turns a date and time (a Ruby Time object) into a human-readable string. Can handle nil
  # input values, as well.
  def time_string(time)
    time.nil? ? "[unknown]" : time.asctime
  end
    
  # A lazy-initialization routine for making sure we've done all the required analysis
  # for all the tests in this test run.
  #
  # Note that this doesn't need to be modified for marshalling, since marshalled objects
  # will already have @read set to true.
  def read
    unless @read
      puts "Analyzing test results in '%s':" % directory_description
            
      start_analysis_time = Time.now
            
      @start_time = get_timestamp(start_timestamp_file)
      @end_time = get_timestamp(end_timestamp_file)
      @test_set_description = read_test_set_description
      @modules = { }
      @total_passed = 0
      @total_tests = 0
      @total_suites_passed = 0
      @total_suites = 0
      @failed = false

      @module_set.each do |build_module|
        dir = FilePath.new(@testrun_directory, build_module.name)
        if FileTest.directory?(dir.to_s)
          module_record = ModuleTestRunRecord.new(@testrun_directory, build_module.name)
          @modules[build_module.name] = module_record
          @total_passed += module_record.total_passed
          @total_tests += module_record.total_tests
          @total_suites_passed += module_record.total_suites_passed
          @total_suites += module_record.total_suites
          @failed = @failed || module_record.failed?
        end
      end
            
      end_analysis_time = Time.now
      puts "\nAnalyzed the results of %d test suite(s) in %.2f seconds.\n\n" % [ @total_suites, end_analysis_time - start_analysis_time ]

      @read = true
            
      save_if_necessary
    end
  end

  # Writes out a description of the given test set to a specified file, so that we
  # can read it back in later, at analysis time. This method is special -- it's called
  # at test-run time.
  def write_test_set_description(test_set)
    File.open(test_set_description_file.to_s, "w") do |file|
      file << test_set.to_s
    end
  end
    
  # Reads in the test set description, as written by write_test_set_description.
  def read_test_set_description
    if FileTest.file?(test_set_description_file.to_s)
      out = ""
      File.open(test_set_description_file.to_s, "r") do |file|
        file.each { |line| out += line }
      end
    else
      out = nil
    end
        
    out
  end
    
  # Where should we store the test set description?
  def test_set_description_file
    FilePath.new(@testrun_directory, "test-set-description.txt")
  end
    
  # Where should we store the start time for the tests?
  def start_timestamp_file
    FilePath.new(@testrun_directory, "testrun.begin")
  end

  # Where should we store the end time for the tests?
  def end_timestamp_file
    FilePath.new(@testrun_directory, "testrun.end")
  end

  # Writes the current time into the given file.
  def set_timestamp(filename)
    File.open(filename.to_s, "w") do |file|
      file << Time.now.strftime("%Y/%m/%d %H:%M:%S") << "\n"
    end
  end

  # Reads the current time from the given file.
  def get_timestamp(filename)
    out = nil

    if FileTest.file?(filename.to_s)
      begin
        File.open(filename.to_s, "r") do |file|
          line = file.readline
          array = ParseDate::parsedate(line, false)
          out = Time.local(*array[0..5])
        end
      rescue Exception => e
        out = nil
      end
    end

    out
  end
end
