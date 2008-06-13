#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Defines a number of TestSet classes. A TestSet defines a set of tests that we would
# like to run (or prepare to run, or whatever); it knows which modules it's interested
# in, which subtrees it's interested in, and what test patterns it wants to run for
# each subtree.
#
# You can implement different objects here to, for example, run only all system tests;
# run only tests ending in 'FooTest' (or whatever); run tests from test-list files;
# or whatever you can imagine.

# The default set of patterns we run if nothing else is specified.
STANDARD_TEST_PATTERNS = [ '*Test' ]

# Mixed in to all TestSet classes; provides useful methods that are implemented in terms
# of the methods expected to be present on any TestSet.
# 
# We expect classes that mix this in to implement:
#
# * <tt>includes_module?(build_module)</tt> -- should we run any tests on the given build module?
# * <tt>includes_subtree?(subtree)</tt> -- should we run any tests on the given subtree?
# * <tt>test_patterns(subtree)</tt> -- which test patterns should we use for the given subtree?
module TestSetBase
  # Runs the specified block on every subtree in the module set that this TestSet
  # is interested in (based on its responses to #includes_module? and #includes_subtree?).
  # The block is passed the subtree and the set of patterns that should be run on 
  # that subtree (as specified by #test_patterns).
  def run_on_subtrees(module_set, &proc)
    module_set.each do |build_module|
      next unless includes_module?(build_module)
      build_module.each do |subtree|
        next unless includes_subtree?(subtree)

        proc.call(subtree, test_patterns(subtree))
      end
    end
  end

  private
  # Reads a file containing a list of patterns of tests to run, one per line, and returns
  # an array representing those patterns. Pretty stupid simple.
  def tests_file_to_test_pattern_list(test_file)
    tests = []

    if FileTest.exist?(test_file.to_s)
      File.open(test_file.to_s, "r") do |file|
        file.each do |line|
          tests << line.strip.gsub(/\./, "/") unless line.strip =~ /^\s*#.*$/ || line.strip =~ /^\s*$/
        end
      end        
    end

    tests
  end

  # Describes a list of build modules, as specified by an array of module names.
  def describe_module_list(list)
    if list.include?('all')
      "All build modules"
    else
      "Build modules: %s" % list.join(", ")
    end
  end
    
  # Describes a set of test types, as specified by an array of subtree names.
  def describe_test_types(types)
    if types.include?('all')
      "all test types"
    else
      "test types: %s" % types.join(", ")
    end
  end
    
  # Returns the list of modules we should run, by reading from the config_source.
  # If the 'check_modules' or 'check_module' configuration properties are set,
  # returns their values (assuming they're a comma-separated string listing modules);
  # 'check_modules' gets preference. If neither is set, then, if 'check_module_groups'
  # or 'check_module_group' is set, returns the list of modules specified by the
  # list of module groups in one of those properties (assuming they're a comma-separated
  # string listing module groups); 'check_module_groups' gets preference.
  #
  # If neither of these is set, returns 'all', meaning all modules should be run.
  def modules_from_config(config_source, module_groups)
    modules_data = config_source.as_array('check_modules') || config_source.as_array('check_module')

    if modules_data.nil? || modules_data.empty?
      modules_groups_spec = config_source.as_array('check_module_groups') || config_source.as_array('check_module_group')
      if modules_groups_spec.nil?
        out = [ 'all' ]
      else
        out = group_list_to_module_list(modules_groups_spec, module_groups)
      end
    else
      out = modules_data
    end
        
    assert { ! out.nil? }
        
    out
  end
    
  # Turns a list of module groups, as specified by 'groups', into a set of modules; module_groups
  # is the hash that maps module group names to an array of module names.
  #
  # Modules will never be included twice in the returned array.
  def group_list_to_module_list(groups, module_groups)
    modules = [ ]
    groups.each do |group_name|
      group_modules = module_groups[group_name.to_sym]
      raise RuntimeError, "No module group named '%s' found." % group_name if group_modules.nil?
      modules = modules.concat(group_modules.collect{ |build_module| build_module.name })
    end
    modules.uniq!
    modules
  end

  # Returns the set of test types we should run, as an array. Returns the set of types specified
  # (as a comma-separated list) in the configuration properties 'check_types' or 'check_type'
  # (with the first getting preference), or ALL_TEST_TYPES, if neither is specified.
  def types_from_config(config_source)
    config_source.as_array('check_types') || config_source.as_array('check_type') || ALL_TEST_TYPES
  end

  # Returns the set of test patterns we should run, as an array. Returns the set of patterns
  # specified (as a comma-spearated list) in the configuration properties 'check_patterns' or
  # 'check_pattern', or STANDARD_TEST_PATTERNS, if neither is specified.
  def patterns_from_config(config_source)
    config_source.as_array('check_patterns') || config_source.as_array('check_pattern') || STANDARD_TEST_PATTERNS
  end
end

# A TestSet that specifies the sets of modules and test types entirely by configuration,
# according to the rules for TestSetBase.modules_from_config and 
# TestSetBase.types_from_config. This is a base class; it doesn't implement #test_patterns,
# so subclasses need to.
class ModuleAndTypeConfigSpecifiedTestSet
  include TestSetBase

  # Creates a new instance. module_groups is the hash that maps module group names to an array
  # of the names of modules in that group.
  def initialize(config_source, module_groups)
    @modules = modules_from_config(config_source, module_groups)
    @types = types_from_config(config_source)
    @patterns = patterns_from_config(config_source)
  end

  def includes_module?(build_module)
    out = @modules.include?('all') || @modules.include?(build_module.name)
    out
  end

  def includes_subtree?(subtree)
    @types.include?('all') || @types.collect { |x| 'tests.%s' % x }.include?(subtree.name)
  end
    
  def to_s
    "%s, %s" % [ describe_module_list(@modules), describe_test_types(@types) ]
  end
end

# A TestSet that specifies absolutely everything from the config source -- uses the rules of
# ModuleAndTypeConfigSpecifiedTestSet for the modules and types, and uses
# TestSetBase.patterns_from_config to get the list of test patterns. (Therefore the set of
# test patterns will never vary between trees.)
class ConfigSpecifiedTestSet < ModuleAndTypeConfigSpecifiedTestSet
  def initialize(config_source, module_groups)
    super(config_source, module_groups)
    @patterns = patterns_from_config(config_source)
  end

  def test_patterns(subtree)
    @patterns
  end
    
  def to_s
    out = super
        
    if @patterns == STANDARD_TEST_PATTERNS
      out += "; standard test patterns (%s)." % STANDARD_TEST_PATTERNS.join(", ")
    else
      out += "; specified test patterns: %s." % @patterns.join(", ")
    end

    out
  end
end

# A TestSet that specifies the set of modules and test types via config, but which
# specifies the set of patterns of tests to run via a single file. This file lists
# one pattern per line, and applies to all modules. This can be used to easily
# run all tests specified in a particular file.
class FileSpecifiedTestSet < ModuleAndTypeConfigSpecifiedTestSet
  def initialize(config_source, module_groups, filename)
    super(config_source, module_groups)
    @filename = filename
    @patterns = tests_file_to_test_pattern_list(filename)
  end

  def test_patterns(subtree)
    @patterns
  end

  # Makes sure we have matching patterns. Without this, you could easily misspell
  # test names in your patterns file, and you'd never know, except that your test
  # simply wouldn't get run any more.
  def validate(subtree, ant, platform, build_results, script_results)
    patterns = test_patterns(subtree)
        
    patterns.each do |pattern|
      matching = subtree.classes_matching_patterns([ pattern ], ant, platform, build_results)
      if matching.empty?
        script_results.failed("%s/%s: %s: No match for pattern '%s'." % [ subtree.build_module.name, subtree.name, subtree.test_list_file(@list_name), pattern ])
      end
    end
        
    patterns.length
  end
    
  def to_s
    "%s; test patterns specified in file '%s': %s" % [ super, @filename, @patterns.join(", ") ]
  end
end

# A TestSet that specifies the set of modules and test types to run via configuration,
# but which looks for a particular file in each subtree that lists the set of test patterns
# to run on that subtree. This can therefore be used to let you specify, in each module,
# in a file with a particular name, a set of tests; you can then run all the tests at
# once by using this object.
class SubtreeFileListedTestSet < ModuleAndTypeConfigSpecifiedTestSet
  # Creates a new instance. list_name is the base name of the list file (as passed to
  # BuildSubtree#test_list_file).
  def initialize(config_source, module_groups, list_name)
    super(config_source, module_groups)
    @list_name = list_name
  end
    
  def test_patterns(subtree)
    tests_file_to_test_pattern_list(subtree.test_list_file(@list_name))
  end
    
  # Makes sure we have matching patterns. Without this, you could easily misspell
  # test names in your patterns file, and you'd never know, except that your test
  # simply wouldn't get run any more.
  def validate(subtree, ant, platform, build_results, script_results)
    patterns = test_patterns(subtree)
        
    patterns.each do |pattern|
      matching = subtree.classes_matching_patterns([ pattern ], ant, platform, build_results)
      if matching.empty?
        script_results.failed("%s/%s: %s: No match for pattern '%s'." % [ subtree.build_module.name, subtree.name, subtree.test_list_file(@list_name), pattern ])
      end
    end
        
    patterns.length
  end
    
  def to_s
    "%s; test patterns specified in lists file '%s' for each subtree" % [ super, @list_name ]
  end
end

# A TestSet that contains a set of modules to run and set of types to run, as
# specified programmatically to its constructor. Always runs the STANDARD_TEST_PATTERNS
# on each subtree, no matter what.
class FixedModuleTypeTestSet
  include TestSetBase
   
  # Creates a new instance. module_names is an array containing the list of modules
  # to run (if 'all' is in the array, all modules will get run, no matter what else
  # is in the array); types is an array containing the list of types of tests to
  # run (if 'all' is in the array, all test types will get run, no matter what else
  # is in the array).
  def initialize(module_names, types)
    @module_names = module_names
    @types = types
  end
   
  def includes_module?(build_module)
    @module_names.include?(build_module.name) || @module_names.include?('all')
  end
   
  def includes_subtree?(subtree)
    @types.collect { |x| 'tests.%s' % x }.include?(subtree.name) || @types.include?('all')
  end
   
  # Makes sure that we've included at least one actual build subtree.
  def validate(module_set, script_results)
    found = false
       
    module_set.each do |build_module|
      build_module.each do |subtree|
        if includes_module?(build_module) && includes_subtree?(subtree)
          found = found || subtree.source_exists
        end
      end
    end
       
    script_results.failed("There are no subtrees named %s in modules %s." % [ @types.join(" or "), @module_names.join(" or ") ]) unless found
       
    found
  end
   
  def test_patterns(subtree)
    STANDARD_TEST_PATTERNS
  end
   
  def to_s
    "modules: %s; types: %s" % [ @module_names.join(", "), @types.join(", ") ]
  end
end

# Just like the FixedModuleTypeTestSet, but you specify a set of module groups, rather
# than a set of modules themselves.
class FixedGroupTypeTestSet < FixedModuleTypeTestSet
  def initialize(group_names, types, module_groups)
    super(group_list_to_module_list(group_names, module_groups), types)
    @module_groups = group_names
  end
    
  def to_s
    "module groups: %s; types: %s" % [ @module_groups.join(", "), @types.join(", ") ]
  end
end

# Just like a ModuleAndTypeConfigSpecifiedTestSet, except that the set of patterns is
# fixed (passed programmatically in the constructor), rather than specified by config.
class FixedPatternSetTestSet < ModuleAndTypeConfigSpecifiedTestSet
  def initialize(config_source, module_groups, patterns)
    super(config_source, module_groups)
    @patterns = patterns
  end
    
  def test_patterns(subtree)
    @patterns
  end
    
  def to_s
    "%s; test patterns: %s" % [ super, @patterns.join(", ") ]
  end
end

# A TestSet that only runs a test with a single name. It will run this test no matter
# where it's located (any module, any test type).
class SingleTestSet
  include TestSetBase

  def initialize(test_name)
    @pattern = test_name.gsub("/", ".").gsub("\\", ".")
  end

  def includes_module?(build_module)
    true
  end

  def includes_subtree?(subtree)
    ALL_TEST_CONTAINING_SUBTREE_NAMES.include?(subtree.name)
  end

  def test_patterns(subtree)
    [ @pattern ]
  end

  def to_s
    "'%s' in any module, any test type." % @pattern
  end
end
