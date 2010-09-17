#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# This file contains the base definitions for BuildModule and BuildSubtree classes, which
# form the heart of our buildsystem. Note that much of both of these classes is defined
# elsewhere -- this file primarily just gives the basic structure, while various methods
# having to do with compilation, tests, computing pathnames, and so on are defined
# in other files (the 'modules_*.rb' files).

# All different types of tests in our system.
ALL_TEST_TYPES = [ 'unit', 'system' ]

# All different types of test trees in our system. This adds 'base', which contains test
# code but no actual tests.
ALL_TEST_SUBTREE_TYPES = [ 'base' ].concat(ALL_TEST_TYPES)

# Names of all different subtrees that contain actual tests.
ALL_TEST_SUBTREE_NAMES = ALL_TEST_SUBTREE_TYPES.collect { |x| 'tests.%s' % x }

# Names of all different subtrees that contain test code.
ALL_TEST_CONTAINING_SUBTREE_NAMES = ALL_TEST_TYPES.collect { |x| 'tests.%s' % x }

# Names of all different subtrees.
ALL_SUBTREE_NAMES = [ 'src' ].concat(ALL_TEST_SUBTREE_NAMES)

# An object that knows how to create a subtree for a build module. We use several constant
# instances of this to make sure we create all the right subtrees for each build module.
class BuildSubtreeFactory
  attr_reader :name

  # Creates a new instance. name is the name of the subtree it will create;
  # internal_dependent_subtree_names is an array of the names of the other subtrees
  # that this subtree is dependent upon (read: 'will compile against'), while
  # external_dependencies_like is a little tricker...
  #
  # When this subtree is compiled
  # or run, it will contain in its CLASSPATH:
  # * all of the classes that are part of the subtrees <em>of the same build module</em> that are named in internal_dependent_subtree_names, and
  # * all of the classes that are part of the subtree <em>in other build modules</em> that is named by external_dependencies_like, and
  # * all of the classes that are part of any subtree <em>in other build modules</em> that the subtree named by external_dependencies_like would compile against.
  #
  # Typically, 'external_dependencies_like' is set to 'src' for 'src' subtrees,
  # meaning 'src' subtrees compile only against 'src' subtrees from other modules.
  # For 'tests.base', 'tests.unit', 'and 'tests.system' subtrees, it is set to
  # 'tests.base', meaning 'tests.*' subtrees compile only against 'tests.base' and
  # 'src' subtrees in other build modules. (This is because you really shouldn't
  # be writing tests in one module that depend on _tests_ in another module --
  # just the test _infrastructure_ in another module.)
  def initialize(name, internal_dependent_subtree_names, external_dependencies_like)
    @name = name.to_s
    @internal_dependent_subtree_names = internal_dependent_subtree_names
    @external_dependencies_like = external_dependencies_like
  end

  # Creates and returns a BuildSubtree object for the given BuildModule.
  def create(build_module)
    root = FilePath.new(build_module.root, @name).to_s
    BuildSubtree.new(@name, build_module, @internal_dependent_subtree_names, @external_dependencies_like)
  end

  # The list of all BuildSubtreeFactory objects we currently use. Adding a new instance
  # here is how you'd create a new type of subtree for all BuildModules. (Note that
  # this doesn't mean you'd need an actual directory for each subtree; a BuildSubtree
  # object exists and works properly whether or not there's actually any source,
  # libraries, etc. for it on disk -- it just doesn't do anything interesting if
  # there's nothing for it on disk.)
  ALL_BUILD_SUBTREE_FACTORIES = [
    BuildSubtreeFactory.new('src', [ ], 'src'),
    BuildSubtreeFactory.new('tests.base', [ 'src' ], 'tests.base'),
    BuildSubtreeFactory.new('tests.unit', [ 'src', 'tests.base' ], 'tests.base'),
    BuildSubtreeFactory.new('tests.system', [ 'src', 'tests.base' ], 'tests.base')
  ]
end

# A build subtree. This represents, for example, 'common/src', or 'dso-system-tests/tests.base', or
# 'ui-configuration/tests.system' -- that is, a single source root, along with all the libraries
# and other stuff that go with it.
class BuildSubtree
  attr_reader :name, :build_module, :source_exists

  # Creates a new instance. The name is something like 'src', 'tests.unit', or whatever; for
  # information about internal_dependencies and external_dependencies_like, see the doc
  # for BuildSubtreeFactory, above.
  def initialize(name, build_module, internal_dependencies, external_dependencies_like)
    @name = name.to_s
    @build_module = build_module
    @internal_dependencies = internal_dependencies
    @external_dependencies_like = external_dependencies_like

    @source_exists = FileTest.directory?(source_root.to_s)
    @resources_exists = FileTest.directory?(resource_root.to_s)

    @static_resources = Registry[:static_resources]
  end

  def to_s
    "<Module subtree: #{module_subtree_name}>"
  end

  def module_subtree_name
    "#{build_module.name}/#{name}"
  end

  # Returns a FilePath pointing to the root of this subtree's source (whether it actually
  # exists or not).
  def source_root
    FilePath.new(build_module.root, name).canonicalize
  end

  # Returns the FilePath to the file that designates which tests in this module are part
  # of the test list named basename. This FilePath is returned whether it actually
  # exists or not.
  def test_list_file(basename)
    FilePath.new(build_module.root, "#{name}.lists.#{basename}")
  end

  # This returns a PathSet representing the paths to all classes that match any of the
  # patterns (expressed as Ant-style patterns, with '*' and '**') in patterns. This can
  # be used to figure out what tests we're going to run before we use them; it uses the
  # Ant 'path' task so that it's reasonably efficient.
  def classes_matching_patterns(patterns, ant, platform, build_results)
    out = PathSet.new

    patterns.each do |pattern|
      # Yup, gotta use a fresh, new Ant property name each time. Yuck.
      path_property_name = platform.next_ant_property_name('patterns_search_path')
      classes_directory = build_results.classes_directory(self).to_s
      pattern_spec = "**/%s.class" % pattern

      ant.path(:id => path_property_name) {
        ant.fileset(:dir => classes_directory, :includes => pattern_spec)
      }

      property_name = platform.next_ant_property_name('patterns_search_path_property')
      # This turns the path into an Ant property. Double yuck, but necessary.
      ant.property(:name => property_name, :refid => path_property_name)
      out << ant.get_ant_property(property_name)
    end

    out
  end

  # Where would resources for this subtree live?
  def resource_root
    FilePath.new(build_module.root, name + ".resources").canonicalize
  end
end

# A build module. This represents the largest-scale division of our codebase, like 'common',
# 'dso-spring', or 'dso-l2'.
class BuildModule
  attr_reader :root, :name, :jdk, :javadoc, :aspectj, :module, :module_set, :dependencies, :groups
  attr_accessor :source_updated
  alias javadoc? javadoc
  alias module? module
    alias aspectj? aspectj

    # Creates a new instance. root_dir is the root directory of the module; module_set is
    # the BuildModuleSet object (see below); data is a hash containing the following
    # options:
    #
    # * :name -- the name of this module
    # * :jdk -- The name of a JDK as configured in jdk.def.yml
    # * :javadoc -- boolean flag indicating whether or not javadoc API documentation
    #               should be generated for this module
    # * :dependencies -- an array of the names of the modules this module is declared to be dependent on
    #
    # Note that, unlike a subtree, the root directory of a build module must exist. There's
    # no point in creating a module that has absolutely nothing in it.
    def initialize(root_dir, module_set, data)
      @module_set = module_set
      @name = data[:name]
      @root = FilePath.new(root_dir, @name).canonicalize
      jdk = data[:jdk]
      assert("modules.def.yml: module #{@name} does not have required jdk: attribute") { ! jdk.nil? }
      @jdk = Registry[:jvm_set][jdk]
      @javadoc = data[:javadoc] || false
      @aspectj = data[:aspectj] || false
      @module = data[:module] || false
      @source_updated = false
      @dependencies = data[:dependencies] || [ ]
      @groups = Array.new

      assert("Root ('#{@root.to_s}') must be an absolute path") { @root.absolute? }
      assert("Root ('#{@root.to_s}') must exist, and be a directory") { FileTest.directory?(@root.to_s) }

      @subtrees = [ ]
      # Run through all the factories and add a new subtree for each one.
      BuildSubtreeFactory::ALL_BUILD_SUBTREE_FACTORIES.each { |factory| @subtrees << factory.create(self) }
    end

    # Runs the passed block for each subtree, passing in the subtree.
    def each(&proc)
      @subtrees.each(&proc)
    end

    def to_s
      "<Module '#@name', at '#{root}', JDK '#{jdk}', dependent on: %s>" % @dependencies.join(", ")
    end

    # The list of build modules (actual BuildModule objects) that this module is dependent upon.
    def dependent_modules
      out = [ ]
      @dependencies.each { |dependency| out << @module_set[dependency] }
      out
    end

    # Returns the subtree with the specified name. Raises an exception if no subtree
    # with the given name exists.
    def subtree(name)
      out = nil
      @subtrees.each { |subtree| out = subtree if subtree.name == name.to_s }
      raise "Unknown subtree '#{name}'" if out.nil?
      out
    end

    # Returns true if this module has a subtree with the specified name.
    def has_subtree?(name)
      File.directory?(FilePath.new(@root, name).to_s)
    end

    # Returns the groupId for this module.  Currently this method simply returns the hard-coded
    # value "org.terracotta.modules".  At some point in time, it may be possible for each module to
    # specify its own groupId.
    def group_id
      "org.terracotta.modules"
    end

    def source_updated?
      @source_updated
    end
  end

  # Maintains a set of BuildModule objects. This is little more than an array, but lets
  # us wrap it with an object so that we can add useful methods to it. It does also let
  # us create new BuildModules and add them to the set simply by passing in a hash with
  # the right module data -- the stuff that reads modules.def.yml and creates modules
  # out of it uses this functionality.
  class BuildModuleSet
    include Enumerable

    # Creates an instance with the given root directory.
    def initialize(root_dir)
      @root_dir = root_dir
      @modules = [ ]
    end

    # Given a hash, creates a new BuildModule from it and adds it to the set. The hash
    # must contain exactly the data required by BuildModule#initialize (see above).
    def add(data)
      @modules << BuildModule.new(data[:root_dir] || @root_dir, self, data)
    end

    # Returns the module with the given name, or raises an exception if no such module
    # exists.
    def [](module_name)
      out = nil
      @modules.each { |the_module| out = the_module if the_module.name == module_name }
      raise "No such module '#{module_name}'" if out.nil?
      out
    end

    # Runs the given block for each module in the system, yielding each module in turn.
    def each(&proc)
      @modules.each(&proc)
    end

    def to_s
      out = "<BuildModuleSet:\n"
      @modules.each { |buildmodule| out += "   %s\n" % buildmodule.to_s }
      out += ">\n"
      out
    end
  end
