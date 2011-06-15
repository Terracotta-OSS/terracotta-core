#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# This is the main Terracotta build file. It consists primarily of the
# BaseCodeTerracottaBuilder, which is an object that knows how to build various
# things in code/base/.

require 'java'
require 'builder/antbuilder'
require 'fileutils'
require 'yaml'
require 'set'
require 'erb'
require 'find'

require 'buildscripts/cross_platform'
require 'buildscripts/autorequire'

# Universally accessible Hash that can be used as a service registry.  This can
# be useful to avoid passing a bunch of parameters around.  To add a service to
# the registry, just add an entry to the Hash, such as
#   Registry[:my_service] = my_service_object
unless defined?(Registry)
  Registry = Hash.new
end

# Get all our buildscripts code.
AutoRequire.all_in_directory('buildscripts')

# The prefix that may optionally be used for all properties that control the build.
# This can be useful to distinguish properties that control the build from properties
# that affect tests directly, etc., especially when you're using some overarching
# system like CruiseControl -- it makes it clear that the property will control tcbuild,
# rather than controlling CruiseControl itself or configuring how the tests respond.
BUILD_CONTROL_PREFIX = "tc.build-control."

include MavenConstants
include PropertyNames

class BaseCodeTerracottaBuilder < TerracottaBuilder
  # Creates a new instance. 'arguments' should just be the list of arguments passed on the
  # command line.
  def initialize(arguments)
    super(:help, arguments)

    # Some more objects we need.
    os_root_dir = FilePath.new(@basedir.to_s, "..", "..").canonicalize.to_s
    ee_root_dir =  os_root_dir =~ /community/ ? FilePath.new(os_root_dir.to_s, "..").canonicalize.to_s : nil
    @build_environment = BuildEnvironment.new(platform, config_source, os_root_dir, ee_root_dir)
    if @build_environment.is_ee_branch?
      @ee_basedir = File.join(ee_root_dir, "code", "base")
    end
    @static_resources = StaticResources.new(basedir)
    @archive_tag = ArchiveTag.new(@build_environment)

    Registry[:build_environment] = @build_environment
    Registry[:static_resources] = @static_resources

    @filters_def = load_yaml_erb(@static_resources.global_filters_file.to_s, binding)
    Registry[:filters_def] = @filters_def

    handle_appserver_overwite()

    # Figure out which JVMs we're using.
    find_jvms()

    # Load up our modules; allow definition of new modules by setting a configuration
    # property that points to additional module files to load. I believe that right now
    # this isn't used, but it was used in the past to (e.g.) let the Spring folks
    # define additional modules without making everybody else compile them (because
    # they did not, in fact, compile, IIRC).
    module_files = [ FilePath.new(basedir, 'modules.def.yml') ]
    additional_files = config_source.as_array('additional_module_files') || []
    additional_files << @static_resources.enterprise_modules_file
    additional_files.each { |file| module_files << FilePath.new(file.to_s) } unless additional_files.nil?

    # Our BuildResults object.
    @build_results = BuildResults.new(FilePath.new(basedir, 'build'))
    Registry[:build_results] = @build_results

    # Go get our module set and module groups from the modules.def.yml and associated files.
    built_data = BuildModuleSetBuilder.new(basedir, *module_files).build_modules
    @module_set = built_data[:module_set]
    @module_groups = built_data[:module_groups]

    # Load the XMLBeans task, so we can use it to process config files when needed by that target.
    ant.taskdef(:name => 'xmlbean', :classname => 'org.apache.xmlbeans.impl.tool.XMLBean')

    # fail if branh name doesn't match between OSS and EE branch
    if @build_environment.is_ee_branch?
      branch = @build_environment.current_branch
      unless @build_environment.ee_svninfo.url =~ /#{branch}$/
        STDERR.puts("*" * 80)
        STDERR.puts("  Branch name '#{branch}' doesn't match between EE and OS branches.")
        STDERR.puts("  EE URL: #{@build_environment.ee_svninfo.url}")
        STDERR.puts("  OS URL: #{@build_environment.os_svninfo.url} ")
        STDERR.puts("*" * 80)
        fail("branch name doesn't match")
      end
    end
  end

  def enterprise?
    flavor = (@flavor || OPENSOURCE).downcase
    flavor == 'enterprise'
  end

  def handle_appserver_overwite()
    appserver = @config_source['tc.build-control.appserver'] || @config_source['appserver']
    unless appserver.nil?
      if appserver =~ /([^-]+)-([^\.]+)\.(.*)/
        @internal_config_source['tc.tests.configuration.appserver.factory.name'] = $1
        @internal_config_source['tc.tests.configuration.appserver.major-version'] = $2
        @internal_config_source['tc.tests.configuration.appserver.minor-version'] = $3
      end
    end
  end

  def monkey?
    config_source['monkey-name']
  end

  # Prints a help message.
  def help
    loud_message "Please read README.txt for available tcbuild targets"
  end

  # Shows a list of all available variants for each subtree. Useful for debugging the buildsystem.
  def show_variants
    depends :init

    @module_set.each do |build_module|
      build_module.each do |subtree|
        variants = subtree.full_all_variants

        unless variants.empty?
          puts "Available variants for #{build_module.name}/#{subtree.name}:"
          variants.each do |variant_name, variant_values|
            variant_values.each do |variant_value|
              puts "   %s: %s: %s" % [ variant_name, variant_value, subtree.full_variant_libraries(variant_name, variant_value) ]
            end
          end
          puts ""
        end
      end
    end
  end

  # Used by every other target, basically.
  def init
    # set flavor property passed in by users
    @internal_config_source['flavor'] = @flavor

    # this setup is for kit packaging naming pattern
    # it's overwritten by build nightly kits target to add revision to the name
    unless @internal_config_source['version_string']
      @internal_config_source['version_string'] = @build_environment.version
    end
    
    write_build_info_file if monkey?
  end
  
  def findbugs(with_gui=nil)
    depends :compile
    
    @ant.taskdef(:name => "findbugs",  :classname => "edu.umd.cs.findbugs.anttask.FindBugsTask")
    findbugs_home = ENV['FINDBUGS_HOME'] || ''
    raise("FINDBUGS_HOME is not defined or doesn't exist") unless File.exists?(findbugs_home)

    @ant.findbugs( :home => findbugs_home, :output => "xml:withMessages", :outputFile => "build/findbugs.xml",
                   :jvmargs => "-Xmx512m", :excludeFilter => "findbugs/excludeFilter.xml") do
      @module_set.each do |build_module|
        src_subtree = build_module.subtree("src")
        src_path = src_subtree.source_root.to_s
        if File.exists?(src_path)
          build_path = @build_results.classes_directory(src_subtree).to_s
          @ant.sourcePath(:path => src_path)
          @ant.class(:location => build_path)
        end
      end
    end
    
    if with_gui == 'gui'
      findbugs_gui
    end
    
  end
  
  def findbugs_gui
    findbugs_home = ENV['FINDBUGS_HOME'] || ''
    raise("FINDBUGS_HOME is not defined or doesn't exist") unless File.exists?(findbugs_home)
    params = "-gui -look:native -maxHeap 512 build/findbugs.xml"
    if ENV['OS'] =~ /windows/i
      `#{findbugs_home.gsub('\\', '/')}/bin/findbugs.bat #{params}`
    else
      `#{findbugs_home}/bin/findbugs #{params}`
    end
  end

  # Download and install dependencies as specified by the various ivy*.xml
  # files in the individual modules.
  def resolve_dependencies
    if @no_ivy
      loud_message("--no-ivy option found. Skipping Ivy.")
      return
    end
    
    depends :init
    ivy_settings_path = File.join(@basedir.to_s, "buildconfig/ivysettings.xml")
    
    @ant.property(:name => "ivy.resolver.default.check.modified", :value => "false")
    @ant.taskdef(:name => 'ivy_configure',
      :classname => "org.apache.ivy.ant.IvyConfigure")
    @ant.taskdef(:name => 'ivy_resolve',
      :classname => "org.apache.ivy.ant.IvyResolve")
    @ant.taskdef(:name => 'ivy_retrieve',
      :classname => "org.apache.ivy.ant.IvyRetrieve")
    @ant.taskdef(:name => 'makepom',
      :classname => "org.apache.ivy.ant.IvyMakePom")
    
    @ant.ivy_configure( :file => ivy_settings_path )

    bases = [@basedir.to_s]
    if @build_environment.is_ee_branch?
      bases << @ee_basedir
    end
    
    FilePath.new(@static_resources.lib_dependencies_directory).ensure_directory
    dependencies_pattern = "#{@static_resources.lib_dependencies_directory.to_s}/[artifact]-[revision].[ext]"

    bases.each do |base|
      loud_message("resolving base #{base}")

      projects = Dir.entries(base)
      if @config_source['projects']
        projects = @config_source['projects'].split(/,/)
      end

      projects.each do |project|
        project_path = File.join(base, project)

        next unless File.directory?(project_path)
        next if project =~ /buildconfig/
        
        Dir.glob("#{project_path}/ivy*.xml").each do |ivyfile|
          next if ivyfile =~ /ivysettings.xml/
          puts "Resolving Ivy file: #{ivyfile}"
          @ant.ivy_resolve(:file => ivyfile)
          @ant.ivy_retrieve(:pattern => dependencies_pattern)
          #puts "Converting to pom"
          #@ant.makepom(:ivyfile => ivyfile, :pomfile => ivyfile + ".pom") do
          #@ant.mapping(:conf => "default", :scope => "compile")
          #end
        end
      end
    end
    
  end

  # Show which modules have been defined. Useful for debugging the buildsystem.
  def show_modules
    puts "Module set: %s" % @module_set.to_s
  end

  # Show all of the modules that the named module depends on.
  def show_module_dependencies(module_name)
    puts("Module #{module_name} depends on:")
    @module_set[module_name].dependencies.each do |dep| puts("\t#{dep}") end
  end

  # Show all of the modules that are dependent on the named module.
  def show_dependent_modules(module_name)
    puts("The following modules are dependent on #{module_name}:")
    @module_set.each do |mod|
      puts("\t#{mod.name}") if mod.dependencies.include?(module_name)
    end
  end

  # clean 'build' and  'depedencies/lib'
  def clean_all
    begin
      clean
      lib = File.join(@basedir.to_s, "dependencies", "lib")
      Dir.chdir(lib) do 
        FileUtils.rm Dir.glob("*")
      end
    rescue Errno::ENOENT => e       
      # ignore file not found error
    end
  end
  
  # clean 'build' folder
  def clean
    begin
      build_folder = File.join(@basedir.to_s, "build")
      FileUtils.rm_rf(build_folder)
      # in Windows, long filename can't be deleted
      # need to use our ad-hoc delete function
      delete_deep_folder(build_folder) if File.exists?(build_folder)
      
      fail("Can't clean build folder") if File.exists?(build_folder)
    rescue Errno::ENOENT => e       
      # ignore file not found error
    end
  end

  def clean_dist
    FileUtils.rm_rf File.join(@basedir.to_s, "build/dist")
  end

  def clean_cache
    FileUtils.rm_rf '.tc-build-cache'
  end

  # Removes the results of all tests you've ever run, but doesn't remove any of the compiled classes.
  # Can be useful for slimming down your 'build' directory when it's gotten out of control, but you
  # don't want to wait for a full recompile.
  def clean_tests
    depends :init

    TestRunResults.clean_all(@build_results, ant)
  end

  # Yeah. That little thing.
  def compile
    depends :init, :resolve_dependencies

    if @no_compile
      loud_message("--no-compile option found.  Skipping compilation.")
      return
    end
   
    compile_only = config_source['compile_only']
    unless compile_only.nil?
      loud_message("compile_only option found. Only specified modules will be compiled.")
      module_names = compile_only.split(/,/)
      module_names.each do |name|
        build_module = @module_set[name]
        build_module.compile(@jvm_set, @build_results, ant, config_source, @build_environment)
      end
    else
      @module_set.each do |build_module|
        build_module.compile(@jvm_set, @build_results, ant, config_source, @build_environment)
      end
    end
  end

  # only compile the specified module
  # and its dependencies
  def smart_compile(build_module_name)
    depends :init, :resolve_dependencies

    build_module = @module_set[build_module_name]
    if @no_compile
      loud_message("--no-compile option found.  Skipping compilation.")
    else
      build_module.dependent_modules.each do |dependent_module|
        dependent_module.compile(@jvm_set, @build_results, ant, config_source, @build_environment)
      end
      build_module.compile(@jvm_set, @build_results, ant, config_source, @build_environment)
    end
  end

  def build_external
    builder = ExternalProjectBuilder.new(@static_resources.external_projects_directory)
    builder.build_all
  end

  # Produces Javadoc documentation in the build/doc/api directory.
  # The set of modules to include is specified in modules.def.yml
  def javadoc
    depends :init

    javadoc_dir = @build_results.javadoc_directory.delete
    javadoc_dir.delete

    @ant.javadoc(:destdir => javadoc_dir.to_s,
      :author => true, :version => true, :use => true,
      :windowtitle => "Terracotta API Documentation") do
      modules = @module_set.find_all {|mod| mod.javadoc?}
      modules.each do |mod|
        @ant.packageset(:dir => mod.name + '/src', :defaultexcludes => true) do
          @ant.include(:name => 'com/tc/**')
        end
      end
    end
  end

  # Runs all tests; you can use check_modules, check_types, check_patterns, etc. (as documented
  # in ConfigSpecifiedTestSet) to alter exactly what gets run.
  def check
    depends :init, :compile
    run_tests(ConfigSpecifiedTestSet.new(config_source, @module_groups))
  end

  # Runs a single test, as named by its one argument. This is actually a pattern;
  # 'tcbuild check_one *Test' will, in fact, run all tests -- but that's just a weird thing to do...
  def check_one(test)
    depends :init, :compile
    run_tests(SingleTestSet.new(test))
  end

  def pound_this(test)
    depends :init, :compile
    upper = 9999
    (1..upper).each do |count|
      STDERR.puts "Pounding count: #{count}"
      run_tests(SingleTestSet.new(test))
      if @script_results.failed?
        STDERR.puts "Test failed after #{count} runs"
        if config_source['email']
          `echo #{@script_results.to_s} | mail -s "#{test} failed after pounded for #{count} times" #{config_source['email']}`
        end
        break
      end
    end
    puts "Pounded for #{upper} without failure."
  end

  # Runs all test patterns named in the specified file, which typically would be just a list of
  # tests, one to a line.
  def check_file(test_file)
    depends :init, :compile
    run_tests(FileSpecifiedTestSet.new(config_source, @module_groups, test_file))
  end

  # Runs all tests specified in the <module>/<subtree>.lists.tests.<type> file, for each
  # subtree (or those specified by the 'check_modules' and 'check_types' configuration properties).
  def check_list(listname)
    depends :init, :compile
    validate_and_run(SubtreeFileListedTestSet.new(config_source, @module_groups, listname))
  end

  # Runs a distributed test; the rest of the arguments on the command line are passed as arguments
  # to the ControlSetup class.
  def run_disttest
    if jdk = config_source['jdk']
      jvm = @jvm_set[jdk]
    else
      jvm = @module_set['dso-performance-tests'].subtree('tests.base').tests_jvm(@jvm_set)
    end
    args = all_remaining_arguments
    args << 'javahome=%s' % jvm.home.to_s unless args.find { |arg| arg.starts_with?('javahome=') }
    # do_run_class(jvm, classname, arguments, jvmargs, subtree)
    do_run_class(jvm, 'com.tc.simulator.distrunner.ControlSetup', args,
      config_source.as_array('jvmargs') || [ ],
      @module_set['dso-performance-tests'].subtree('tests.base'))
  end

  # Runs the 'short' test list. (For a quick 'sanity check' of our software.)
  def check_short
    check_list('short')
  end

  # Runs all tests in the 'pounder' list. (For the pounder monkey.)
  def check_pounder
    check_list('pounder')
  end

  # monkey police calls this action:
  #  - check for version in poms
  #  - check for compilation 
  #  - check_short
  def check_compile
    begin
      @no_compile = false # override this if it's turned on
      check_maven_version
      check_short
      raise "check_short failed." if @script_results.failed?
         
      mark_this_revision_as_good()
    rescue 
      mark_this_revision_as_bad($!)
      raise
    end
  end

  def check_ee_build
    check_one('AssertTest')
    publish_enterprise_packages
  end
    
  # A helper method to validate and run a test set. (Validation lets us avoid the case where,
  # for example, you misspell a test name in a list and don't realize it.)
  def validate_and_run(test_set)
    start_time = Time.now
    total_patterns = 0

    @module_set.each do |build_module|
      build_module.each do |subtree|
        total_patterns += test_set.validate(subtree, ant, @platform, @build_results, @script_results)
      end
    end

    end_time = Time.now
    puts "\nValidated all %d test(s) in %.2f seconds for %s." % [ total_patterns, end_time - start_time, test_set.to_s ]

    run_tests(test_set) unless @script_results.failed?
  end

  # Runs the crash tests. Uses the internal configuration source to set the required property.
  def check_crashtests
    depends :init, :compile
    @internal_config_source['tc.tests.configuration.transparent-tests.mode'] = 'crash'
    run_tests(FixedModuleTypeTestSet.new([ 'dso-crash-tests'], [ 'system' ]))
  end

  # Runs the active/passive tests. Uses the internal configuration source to set
  # the required property.
  def check_active_passive
    depends :init, :compile
    @internal_config_source['tc.tests.configuration.transparent-tests.mode'] = 'active-passive'
    @internal_config_source['test_timeout'] = (30 * 60).to_s
    run_tests(FixedModuleTypeTestSet.new([ 'dso-crash-tests' ], [ 'system' ]))
  end

  # Runs the active/active tests. Uses the internal configuration source to set
  # the required property.
  def check_active_active
    depends :init, :compile
    @internal_config_source['tc.tests.configuration.transparent-tests.mode'] = 'active-active'
    @internal_config_source['test_timeout'] = (30 * 60).to_s
    run_tests(FixedModuleTypeTestSet.new([ 'ent-active-active-tests' ], [ 'system' ]))
  end
  
  # Prepares to run tests in the given module of the given type, then writes out all the information
  # you'd need to run them to the screen.
  def check_show(module_name, test_type)
    depends :init, :compile
    test_set = FixedModuleTypeTestSet.new([ module_name ], [ test_type ])
    if test_set.validate(@module_set, @script_results)
      prepare_and_run_block_on_tests(test_set,
        TestRunResults.next_results(@build_results), Proc.new { |testrun| puts testrun.dump })
    end
  end

  # Prepares to run tests in an external tool (i.e., Eclipse).
  def check_prep(module_name = 'all', test_type = 'all')    
    depends :init, :compile
    
    loud_message "You might need to pass tests-jdk=1.5 if the test is 1.5 compatible and you want L2 to run in process"
    
    if module_name.downcase == 'all'
      @module_set.each do |mod|
        check_prep(mod.name, test_type)
      end
    else
      if test_type.downcase == 'all'
        %w(unit system).each do |type|
          check_prep(module_name, type)
        end
      else
        mod = @module_set[module_name]
        if mod.has_subtree?("tests.#{test_type}")
          test_set = FixedModuleTypeTestSet.new([module_name], [test_type])
          if test_set.validate(@module_set, @script_results)
            results_dir = FilePath.new(@build_results.build_dir,
              "externally-run-tests")
            results_dir = results_dir.ensure_directory
            test_proc = Proc.new { |testrun| testrun.prepare_for_external_run }
            prepare_and_run_block_on_tests(test_set,
              TestRunResults.new(results_dir), test_proc)
          end
        end
      end
    end
  end

  # Regenerates the JAR containing the class files that represent the XML schema.
  # You must rerun this whenever you change the .xsd files -- that is, if you want
  # your changes to show up. ;)
  def generate_config_classes
    generate_xmlbeans_class("generate_config_classes")
  end
  
  # Generates the JAR containg the class files that represent the XML schema  
  # for the l1 reconnect properties from l2 
  def generate_l1_reconnect_properties_classes
    generate_xmlbeans_class("generate_l1_reconnect_properties_classes")
  end
  
  def generate_stats_config_classes
    generate_xmlbeans_class("generate_stats_config_classes")
  end

  # A target for the monkeys to run when you just want to test that they're
  # working correctly. We run this test because it's one of the few that's
  # relevant to the monkey system itself.
  def check_monkeytest
    check_one('AssertTest')
  end

  # monkey target to test reflector links
  def check_reflector
    check_one('ReflectorTest')
  end
  
  # Target that runs tests for all modules which are not assigned to a module group.
  # This is to ensure that modules that are not assigned to a group are executed by
  # the 'alltests' monkey.
  def check_nogroup
    depends :init, :compile
    groupless = @module_set.find_all { |mod| mod.groups.empty? }.map { |mod| mod.name }
    run_tests(FixedModuleTypeTestSet.new(groupless, %w(unit system)))
  end
  
  def check_maven_version
    maven_version = @config_source['maven.version']
    printf("%-50s: %s\n",  "maven.version defined in build-config.global", maven_version)
    poms = Dir.glob('poms/*.xml')
    poms.each do |pom|
      compare_maven_version(maven_version, pom, '/project/parent/version')
    end
    compare_maven_version(maven_version, 'pom.xml', '/project/properties/tcVersion')
  end
  
  def eclipse_gen
    build_module_name = 'eclipsegen'
    subtree_name = 'src'
    build_module = @module_set[build_module_name]
    subtree = build_module.subtree(subtree_name)

    build_module.compile(@jvm_set, @build_results, ant, config_source, @build_environment)

    if config_jre = config_source['jdk']
      jvm = @jvm_set[config_jre]
    else
      jvm = build_module.jdk
    end
    arguments = all_remaining_arguments
    jvmargs = config_source.as_array('jvmargs') || [ ]
    do_run_class(jvm, "eclipsegen.Main", arguments, jvmargs, subtree, FilePath.new(@basedir, build_module_name))    
  end
  
  def generate_license
    fail("Only EE branch checkout can be used for license generation") unless @build_environment.is_ee_branch?
    depends :init, :compile
    build_module_name = 'ent-license-generator'
    subtree_name = 'src'
    build_module = @module_set[build_module_name]
    subtree = build_module.subtree(subtree_name)

    build_module.compile(@jvm_set, @build_results, ant, config_source, @build_environment)

    if config_jre = config_source['jdk']
      jvm = @jvm_set[config_jre]
    else
      jvm = build_module.jdk
    end
    arguments = all_remaining_arguments
    jvmargs = config_source.as_array('jvmargs') || [ ]
    do_run_class(jvm, "com.tc.license.generator.EnterpriseLicenseGenerator", arguments, jvmargs, subtree)    
  end

  # Runs a class, as specified on the command line. Takes one argument, which is the name of the test;
  # you can also set 'jvm' (to point to the JAVA_HOME you want to use), 'module' and 'subtree' to point
  # to the module and subtree you want to run against (for CLASSPATH, etc.), 'jvmargs' to specify JVM
  # arguments, and 'with_dso' to 'true' if you want to run with the DSO boot JAR in your CLASSPATH.
  def run_class(classname)
    depends :init, :compile

    build_module_name = config_source['module'] || 'legacy-test-tree'
    subtree_name = config_source['subtree'] || 'src'
    build_module = @module_set[build_module_name]
    subtree = build_module.subtree(subtree_name)

    if config_jre = config_source['jdk']
      jvm = @jvm_set[config_jre]
    else
      jvm = build_module.jdk
    end
    arguments = all_remaining_arguments
    jvmargs = config_source.as_array('jvmargs') || [ ]

    if config_source['with_dso'] =~ /^\s*true\s*$/i
      if config_source['home'] != nil
        homedir = FilePath.new(config_source['home'])
      else
        homedir = @build_results.tools_home
      end

      home = TerracottaHome.new(@static_resources, homedir).prepare(ant)

      unless jvmargs.detect { |arg| arg =~ /^\s*\-Xbootclasspath.*$/i }
        begin
          boot_jar = BootJar.new(jvm, home.dir, @module_set, home.config_file).ensure_created
          jvmargs << '-Xbootclasspath/p:%s' % boot_jar.path.to_s
        rescue
          STDERR.puts("Failed to create bootjar for: " + classname.to_s + ". Check log for exception.")
        end
      end

      unless jvmargs.detect { |arg| arg =~ /^\s*\-Dtc.config=.*$/i }
        jvmargs << '-Dtc.config=%s' % home.config_file.to_s
      end

      unless jvmargs.detect { |arg| arg =~ /^\s*-Dtc.classpath=.*$/i }
        classpath = @module_set['deploy'].subtree('src').classpath(@build_results, :full, :runtime)
        jvmargs << '-Dtc.classpath=%s' % classpath
      end
    end

    do_run_class(jvm, classname, arguments, jvmargs, subtree)
  end

  # Runs the Terracotta server. You can specify the 'jvmargs' config property to change the JVM
  # arguments it runs with.
  def run_server
    depends :init, :compile

    build_module = @module_set['deploy']
    subtree = build_module.subtree('src')

    home = TerracottaHome.new(@static_resources, @build_results.server_home).prepare(ant)

    # 2006-07-19 andrew -- HACK HACK HACK. This is really quite lame. However, the server
    # requires this to exist for now. We should fix the server to do something more sane
    # than this.
    lib_directory = FilePath.new(home.dir, "common", "lib").ensure_directory
    FileUtils.touch(FilePath.new(lib_directory, "tc.jar").to_s)

    if config_jre = config_source['jdk']
      jvm = @jvm_set[config_jre]
    else
      jvm = build_module.jdk
    end

    subtree.run_java(ant, 'com.tc.server.TCServerMain', home.dir.to_s,
      jvm, config_source.as_array('jvmargs'),
      all_remaining_arguments,
      { 'tc.base-dir' => home.dir.to_s },
      @build_results, @build_environment)
  end

  # Re-runs only those tests that failed on the last test run (or on the one specified by
  # the 'testrun' config property).
  def recheck
    depends :init, :compile

    if config_source['testrun'].nil?
      latest_record = TestRunRecord.latest_record(@build_results.build_dir, @module_set)
      descrip = "The latest test run"
    else
      latest_record = named_testrun_record(config_source['testrun'])
      descrip = "The specified test run"
    end

    failed_test_classnames = latest_record.failed_test_classnames

    puts ""
    if failed_test_classnames.empty?
      puts "%s, in '%s', passed. There is nothing to recheck." % [ descrip, latest_record.directory_description ]
    else
      puts "%s, in '%s', failed %d tests. Re-running the following tests:" % [ descrip, latest_record.directory_description, failed_test_classnames.length ]

      patterns = [ ]
      failed_test_classnames.each do |classname|
        puts "   %s" % classname
        components = classname.split(/\./)
        patterns << FilePath.new(*components).to_s
      end

      puts ""

      run_tests(FixedPatternSetTestSet.new(config_source, @module_groups, patterns))
    end
  end

  # Shows, in a human-readable format, the results of the specified testrun
  # (e.g., 'tcbuild show_test_results testrun-0001'); you can also specify 'latest' to
  # see results of the latest testrun.
  def show_test_results(testrun_name)
    depends :init

    if testrun_name.strip.downcase == 'latest'
      record = TestRunRecord.latest_record(@build_results.build_dir, @module_set)
    else
      record = named_testrun_record(testrun_name)
    end

    puts record.dump
  end

  # Shows the CLASSPATH that we run the given subtree in the given module with.
  def show_classpath(module_name, subtree_name)
    depends :init

    type = (config_source['type'] || :runtime).to_sym
    scope = (config_source['scope'] || :full).to_sym
    classpath = @module_set[module_name].subtree(subtree_name).classpath(@build_results, scope, type)

    puts "For subtree '#{module_name}/#{subtree_name}', the #{scope} #{type} classpath is:"
    puts ""
    puts classpath
  end

  # Creates a boot JAR. jvm_spec (the argument) can be either '1.5', '1.6', or a path to a
  # JAVA_HOME. Creates the boot JAR for this JVM, and prints out where it put it.
  def create_boot_jar(jvm_spec)
    depends :init, :compile

    jvm = @jvm_set[jvm_spec]
    output_path = @build_results.tools_home
    output_path = FilePath.new(config_source['dest']) unless config_source['dest'].nil?
    boot_jar = BootJar.new(jvm, output_path, @module_set, @static_resources.dso_boot_jar_config_file)
    boot_jar.ensure_created(:delete_existing => true)
    puts ""
    puts "Boot JAR for JVM '%s' successfully created at '%s'." % [ jvm.to_s, boot_jar.path.to_s ]
  end

  # Regenerates the old-style (monolithic) Eclipse project.
  def eclipse
    depends :init

    eclipse_builder = EclipseProjectBuilder.new(true, ant)
    @module_set.each { |the_module| the_module.eclipse(@build_environment, eclipse_builder) }
    eclipse_builder.generate
  end

  # Regenerates Eclipse projects for all the modules. Only overwrites .classpath files; will not
  # change existing project files, if they're present.
  def meclipse
    depends :init
    eclipse_builder = EclipseProjectBuilder.new(false, ant)
    @module_set.each { |the_module| the_module.eclipse(@build_environment, eclipse_builder) }
  end

  # Shows all configuration properties we're using.
  def show_config
    puts "Building with configuration:"
    puts "========================================================================"
    puts
    puts configuration_summary
  end

  # Does nothing; can be a useful target occasionally. (No, really.)
  def do_nothing
    # do a whole lot of nothing
  end

  def show_jvms
    puts(find_jvms)
  end

  def archive_build
    depends :compile
    archive_build_if_necessary
  end

  protected
  # Overrides superclass method to provide for implicit targets.
  def target_missing(target)
    super(target) unless handle_implicit_check_target(target)
  end

  # Provides for implicit targets: you can say 'check_<modulegroup>',
  # 'check_<testtype>', or 'check_<modulegroup>_<testtype>', and it will 'just work'.
  # e.g.: check_spring; check_system; or check_spring_system.
  def handle_implicit_check_target(target)
    out = false

    if target.to_s =~ /^check_(.*)\s*$/
      out = true
      data = $1

      if data =~ /^([A-Za-z0-9]+)_([A-Za-z0-9]+)$/
        group = $1.to_sym
        types = [ $2 ]
        puts "Implicit check of group '%s', test types: %s." % [ group, types.join(", ") ]
        puts ""
      elsif ALL_TEST_TYPES.include?(data.downcase)
        types = [ data.downcase ]
        group = :all
        puts "Implicit check of all trees, test type %s." % types[0]
        puts ""
      elsif @module_groups.has_key?(data.downcase.to_sym)
        types = ALL_TEST_TYPES
        group = data.downcase.to_sym
        puts "Implicit check of group '%s', all test types." % group
        puts ""
      else
        raise RuntimeError, "Unknown syntax for implicit check: '%s'." % target
      end

      raise RuntimeError, "No module group named '%s' found when performing implicit check. (Did you accidentally say, e.g., check_unit_dso instead of check_dso_unit?) I know about groups: %s" % [ group, @module_groups.keys.join(", ") ] unless @module_groups.has_key?(group)

      depends :init, :compile
      run_tests(FixedGroupTypeTestSet.new([ group ], types, @module_groups))
    elsif target.to_s =~ /^(create|publish)_(.*)\s*$/
      out  = true
      verb = $1
      data = $2.sub(/_package$/, '')
      send("#{verb}_package".to_sym, data)
    end

    out
  end

  # Wraps the superclass's ConfigSource in an OptionallyPrefixingConfigSource, so we
  # can optionally prefix all build properties with BUILD_CONTROL_PREFIX if we want.
  def create_config_source(*args)
    OptionallyPrefixingConfigSource.new(BUILD_CONTROL_PREFIX, super(*args))
  end

  # Archives the build. Uses the archive tag and the build archive directory to figure
  # out where to put it.
  def archive_build_if_necessary
    unless build_archive_dir.nil?
      local_build_config = FilePath.new(@basedir, "build-config.local")
      if local_build_config.exist?
        @ant.copy(:todir => @build_results.build_dir, :file => local_build_config.to_s)
      end

      @build_results.archive_to(ant, full_build_archive_path)
    end
  end

  def deploy_snapshots
    deploy(OPENSOURCE, true, TERRACOTTA_SNAPSHOTS_REPO_ID, TERRACOTTA_SNAPSHOTS_REPO)
  end

  def deploy_staging
    deploy(OPENSOURCE, false, TERRACOTTA_STAGING_REPO_ID, TERRACOTTA_STAGING_REPO)
  end

  def deploy_releases
    deploy(OPENSOURCE, false, TERRACOTTA_RELEASES_REPO_ID, TERRACOTTA_RELEASES_REPO)
  end

  def deploy_patches
    deploy(OPENSOURCE, false, TERRACOTTA_PATCHES_REPO_ID, TERRACOTTA_PATCHES_REPO)
  end

  def deploy_ee_snapshots
    deploy(ENTERPRISE, true, TERRACOTTA_EE_SNAPSHOTS_REPO_ID, TERRACOTTA_EE_SNAPSHOTS_REPO)
  end

  def deploy_ee_staging
    # since staging is always internal, we use it for both OSS and EE artifacts
    deploy(ENTERPRISE, false, TERRACOTTA_STAGING_REPO_ID, TERRACOTTA_STAGING_REPO)
  end

  def deploy_ee_releases
    deploy(ENTERPRISE, false, TERRACOTTA_EE_RELEASES_REPO_ID, TERRACOTTA_EE_RELEASES_REPO)
  end

  def deploy_ee_patches
    deploy(ENTERPRISE, false, TERRACOTTA_PATCHES_REPO_ID, TERRACOTTA_PATCHES_REPO)
  end

  def build_nightly_kits
    setup_config_for_nightly_kits()
    publish_packages(['dso', 'web'], OPENSOURCE)
  end

  def build_nightly_ee_kits
    setup_config_for_nightly_kits()
    publish_packages(['dso'], ENTERPRISE)
  end

  private

  def setup_config_for_nightly_kits
    @internal_config_source['kit-type'] = 'nightly'
    if @build_environment.current_branch == 'trunk'
      version = 'trunk-SNAPSHOT'
    else
      version = @build_environment.maven_version
    end
    @internal_config_source['version_string'] = version.gsub(/SNAPSHOT/, "nightly-rev#{@build_environment.os_revision}")
    @internal_config_source['build-archive-dir'] = @config_source['build-archive-dir'] || '/shares/monkeyshare/output/kits'
  end

  def deploy(flavor, snapshot, repo_id, repo_url)
    @internal_config_source[MAVEN_SNAPSHOT_CONFIG_KEY] = snapshot.to_s
    @internal_config_source[MAVEN_REPO_ID_CONFIG_KEY] = repo_id
    @internal_config_source[MAVEN_REPO_CONFIG_KEY] = repo_url
    @internal_config_source['include_sources'] = 'true'
    mvn_install(flavor)
  end

  def generate_xmlbeans_class(target)
    ant_script = @static_resources.ant_script
    result = system("#{ant_script} -f buildconfig/xmlbeans-ant-tasks.xml #{target}")
    fail("Error running ant in #{Dir.getwd}") unless result
  end

  def mark_this_revision_as_good
    STDERR.puts("Revision #{@build_environment.combo_revision} is good to go.")
    return if @build_environment.jenkins? && !@build_environment.monkey_police?
    root = File.join(build_archive_dir.to_s, "monkey-police", @build_environment.current_branch)
    FileUtils.mkdir_p(root) unless File.exist?(root)
    good_rev_file = File.join(root, "good_rev.yml")
    File.open(good_rev_file, "w") do | f |
      YAML.dump({'ee' => @build_environment.ee_revision, 'os' => @build_environment.os_revision}, f)
    end
  end

  def mark_this_revision_as_bad(exception)
    STDERR.puts("Revision #{@build_environment.combo_revision} is bad, mm'kay!")
    STDERR.puts(exception) 
    STDERR.puts("Please let people on this mail list know.")
  end

  # The full path to the build archive, including directory.
  def full_build_archive_path
    FilePath.new(build_archive_dir, short_build_archive_path.to_s)
  end

  # The short path to the build archive -- i.e., everything that's produced by the archive tag.
  def short_build_archive_path
    @archive_tag.to_path("build-archive", "zip")
  end

  # A 'pattern' for the build archive. This is used only to put it into a monkey XML file, so that
  # later parts of the monkey infrastructure -- like the log publisher -- can put other files at the
  # same location as the build archive, by using this path. This leaves the buildsystem in full
  # control of the archive path, making it very easy to change in the future.
  def build_archive_path_pattern
    @archive_tag.to_path("${FILENAME}", "${EXTENSION}")
  end

  # Runs the given class, using the given JVM, with the given arguments and JVM arguments, against the
  # given subtree.
  def do_run_class(jvm, classname, arguments, jvmargs, subtree, home=nil)
    home ||= FilePath.new(@basedir)
    puts "Running:"
    puts "    against module '%s', subtree '%s'," % [ subtree.build_module.name, subtree.name ]
    puts "    using the JVM at '%s'," % jvm.home.to_s
    puts "    in directory '%s'" % home.to_s
    puts ""
    puts "    java %s %s %s" % [ jvmargs.join(" "), classname, arguments.join(" ") ]
    puts ""

    subtree.run_java(ant, classname, home, jvm, jvmargs, arguments, { }, @build_results, @build_environment)
  end

  # Where should we aggregate test result files (XML files) to? This is used so that CruiseControl can
  # pick up test results from just one directory, instead of looking all over the place. Normally, it
  # will be unset, except in the monkeys.
  def tests_aggregation_directory
    out = config_source['tests_aggregation_directory']
    out = FilePath.new(out).ensure_directory unless out.nil?
    out
  end

  # Fetches the TestRunRecord with the given name.
  def named_testrun_record(testrun_name)
    TestRunRecord.for_directory(FilePath.new(@build_results.build_dir, testrun_name).to_s, @module_set)
  end

  # Runs the given test set, using the given TestRunResults (or generating a new one if testrun_results is nil).
  # Mainly just prints lots of information to the screen, although also makes sure the TestRunRecord is
  # properly set up and torn down, and so on.
  def run_tests(test_set, testrun_results=nil)
    testrun_results = TestRunResults.next_results(@build_results) if testrun_results.nil?
    testrun_record = TestRunRecord.for_directory(testrun_results.root_dir, @module_set)

    puts ""
    puts "========================================================================"
    puts "  Running tests: %s" % test_set.to_s
    puts "========================================================================"
    puts ""

    testrun_record.setUp(test_set)

    begin
      prepare_and_run_block_on_tests(test_set, testrun_results, Proc.new do |testrun|
          testrun.run(@script_results)
        end, testrun_record)
    ensure
      testrun_record.tearDown
      # copy failed-tests.txt file to the aggreation directory
      if monkey?
        FileUtils.cp(testrun_record.failed_test_classnames_file.to_s, tests_aggregation_directory.to_s)
      end
      puts "\n\n"
      puts "========================================================================"
      puts "Test Results:\n\n"
      puts testrun_record.dump

      puts "\n\n"
      puts "========================================================================"
      if testrun_record.failed?
        @script_results.failed("Tests FAILED. See results above.")
      elsif testrun_record.total_suites == 0
        @script_results.failed("No test found.")
      else
        puts "     Tests passed."
      end
      puts "  Testrun directory: #{testrun_results.root_dir}"
      puts ""
    end
  end

  # Creates a SubtreeTestRun object for each subtree we're going to run tests on, sets it up, runs it,
  # and tears it down -- making sure this all happens correctly even in the face of exceptions.
  #
  # DO NOT make this last object ('proc') an actual block. Although this is, in fact, the right way to
  # do it, it seems to trigger a weird interation between JRuby and leafcutter that results in some very
  # odd/nonsensical leafcutter error messages. (On the other hand, if you can manage to make it work,
  # and you're sure it works in all cases, by all means, go ahead and change it. It certainly is gross
  # the way it is right now.)
  def prepare_and_run_block_on_tests(test_set, testrun_results, testrun_proc, testrun_record = nil)
    test_runs = { }

    test_set.run_on_subtrees(@module_set) do |subtree, test_patterns|
      test_runs[subtree] =
        subtree.test_run(testrun_results, test_patterns, tests_aggregation_directory)

      begin
        test_runs[subtree].setUp
        testrun_proc.call(test_runs[subtree])
      rescue JvmVersionMismatchException => e
        if monkey?
          puts("#{e.message}\n...skipping subtree #{subtree}")
        else
          raise e
        end
      ensure
        test_runs[subtree].tearDown
      end
    end
  end

  # Finds the JVMs that we need to use -- one each for compiling and testing
  def find_jvms

    return @jvm_set if @jvm_set

    @jvm_set = JVMSet.new(YAML.load_file('jdk.def.yml'))

    @jvm_set.add_config_jvm('tests-jdk')
    @jvm_set.add_config_jvm('jdk')

    appserver_compatibility = YAML::load_file("appservers.yml")
    Registry[:appserver_compatibility] = appserver_compatibility
    factory, major, minor = %w(factory.name major-version minor-version).map { |key|
      @internal_config_source["tc.tests.configuration.appserver.#{key}"]
    }
    Registry[:appserver_generic] = "#{factory}-#{major}"
    Registry[:appserver] = "#{Registry[:appserver_generic]}.#{minor}"
    Registry[:jvm_set] = @jvm_set
  end

  # Writes out the given set of keys, and corresponding values, from the given hash to the given
  # file, as XML property elements (as is required by the XML build-information file we generate
  # that CruiseControl reads).
  def write_keys(file, hash, keys)
    keys.sort.each do |key|
      value = hash[key]
      file << "      <property name=\"%s\" value=\"%s\" />\n" % [ key.to_s.xml_escape, value.to_s.xml_escape ]
    end
  end

  # Takes the JVM specified in the JVM set by the given key, and returns a hash
  # with a single key, prop, which maps to a short description of that JVM (something
  # a human can read, but which is also usable as, e.g., a database key representing
  # that JVM).
  def jvm_data(prop, key)
    out = { }

    if @jvm_set.has?(key)
      out[prop] = @jvm_set[key].short_description
    else
      out[prop] = '<none>'
    end

    out
  end

  # Writes out our "build information file". This is a file that gets integrated into the master
  # XML log file that CruiseControl creates at the end of every build; it contains properties
  # that the resulting HTML email and, even more importantly, the monkey database.
  #
  # This is divided into three sections: configuration, parameters, and extra.
  #
  # * *Configuration* data is that which can reasonably be expected to affect the results of tests. For example, branch, platform (including OS type and processor architecture), and JVMs used to run the tests are included here.
  # * *Parameters* is that data which should not affect the results of tests, but which may well be common to many test runs. This is put in the database in a manner that allows us to share it between many monkey runs. (Note that this includes, among other things, hostname -- if two hosts have the same JVM, the same OS, architecture, etc., they really, truly should produce the same results.)
  # * *Extra* data is that data which likely differs between every run of the monkey, and so which is simply stored directly associated with the monkey run. This is things like build date and time, revision number, etc.
  def write_build_info_file

    # Configuration data.
    configuration_data = {
      'build-target' => config_source['tc.build-control.build.target'],

      'monkey-host' => @build_environment.build_hostname,
      'monkey-name' => config_source['monkey-name'],
      'monkey-platform' => @build_environment.platform,

      'branch' => @build_environment.current_branch,
      'is_ee_branch' => @build_environment.is_ee_branch?,
      'revision' => @build_environment.combo_revision,
      
      'jvmargs'  => config_source['jvmargs'],

      'JAVA_HOME_15' => @jvm_set['1.5'].short_description,
      'JAVA_HOME_16' => @jvm_set['1.6'].short_description
    }

    if config_source['tc.tests.configuration.appserver.factory.name']
      name = config_source['tc.tests.configuration.appserver.factory.name']
      major = config_source['tc.tests.configuration.appserver.major-version']
      minor = config_source['tc.tests.configuration.appserver.minor-version']
      configuration_data['appserver'] = "#{name}-#{major}.#{minor}"
    end
    
    if @jvm_set['tests-jdk']
      configuration_data['tests-jdk'] = @jvm_set['tests-jdk'].short_description
    end
    
    # Parameters data.
    parameters_data = {
      # nothing right now
    }

    # Extra data.
    extra_environment_data = {
      'tc.build-archive.path.pattern' => build_archive_path_pattern.to_s
    }

    # Test configuration data, which goes in the configuration section.
    test_config_data = { }
    config_source.keys.each do |key|
      next if key =~ /appserver/
      test_config_data[key] = config_source[key] if key =~ /^tc\.tests\.configuration\..*/i
    end

    File.open(@build_results.build_information_file.to_s, "w") do |file|
      file << "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n\n"
      file << "<configinfo>\n"

      file << "  <monkeydb>\n"
      file << "    <configuration>\n"

      write_keys(file, configuration_data, configuration_data.keys)


      file << "    </configuration>\n"
      file << "    <parameters>\n"

      write_keys(file, parameters_data, parameters_data.keys)
      write_keys(file, test_config_data, test_config_data.keys)

      file << "    </parameters>\n"
      file << "    <extra>\n"

      write_keys(file, extra_environment_data, extra_environment_data.keys)

      file << "    </extra>\n"
      file << "  </monkeydb>\n"
      file << "\n"

      file << "</configinfo>\n"
    end
  end
end

# Terracotta software package making extension for BaseTerracottaBuilder
require 'buildscripts/extensions/distribution'

builder = BaseCodeTerracottaBuilder.new(ARGV)
builder.run
