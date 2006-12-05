#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
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
require 'open-uri'

require 'buildscripts/autorequire'

# Get all our shared Ruby code.
#AutoRequire.all_in_directory(File.join("..", "..", "buildsystems", "lib", "ruby", "shared"))

# Get all our buildscripts code.
AutoRequire.all_in_directory('buildscripts')

# The prefix that may optionally be used for all properties that control the build.
# This can be useful to distinguish properties that control the build from properties
# that affect tests directly, etc., especially when you're using some overarching
# system like CruiseControl -- it makes it clear that the property will control tcbuild,
# rather than controlling CruiseControl itself or configuring how the tests respond.
BUILD_CONTROL_PREFIX = "tc.build-control."

class BaseCodeTerracottaBuilder < TerracottaBuilder
    # Creates a new instance. 'arguments' should just be the list of arguments passed on the
    # command line.
    def initialize(arguments)
        super(:compile, arguments)

        # Load up our modules; allow definition of new modules by setting a configuration
        # property that points to additional module files to load. I believe that right now
        # this isn't used, but it was used in the past to (e.g.) let the Spring folks
        # define additional modules without making everybody else compile them (because
        # they did not, in fact, compile, IIRC).
        module_files = [ FilePath.new(basedir, 'modules.def.yml') ]
        additional_files = config_source.as_array('additional_module_files')
        additional_files.each { |file| module_files << FilePath.new(file.to_s) } unless additional_files.nil?

        # Our BuildResults object.
        @build_results = BuildResults.new(FilePath.new(basedir, 'build'))

        # Go get our module set and module groups from the modules.def.yml and associated files.
        built_data = BuildModuleSetBuilder.new(basedir, *module_files).build_modules
        @module_set = built_data[:module_set]
        @module_groups = built_data[:module_groups]

        # Some more objects we need.
        @build_environment = BuildEnvironment.new(platform, config_source)
        @static_resources = StaticResources.new(basedir)
        @archive_tag = ArchiveTag.new(@build_environment)

        # Load the XMLBeans task, so we can use it to process config files when needed by that target.
        ant.taskdef(:name => 'xmlbean', :classname => 'org.apache.xmlbeans.impl.tool.XMLBean')

        # Figure out which JVMs we're using.
        find_jvms
      end

      # Prints a help message.
      def help
        puts(Resources::HELP_MESSAGE)
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
        write_build_info_file
    end

    # Show which modules have been defined. Useful for debugging the buildsystem.
    def show_modules
        puts "Module set: %s" % @module_set.to_s
    end

    # Um. Duh.
    def clean
        # DO NOT make 'clean' depend on 'init'. 'init' writes out certain files to the build/ directory;
        # if you make 'clean' depend on 'init', then if you do something like 'tcbuild clean compile',
        # 'clean' will invoke 'init', then blow away those files, and then 'compile' won't invoke 'init'
        # (even though it depends on it) since it's already been run. All the files written out by 'init'
        # will then be missing.
        @build_results.clean(ant)
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
        depends :init

        @module_set.each do |build_module|
            build_module.compile(@jvm_set, @build_results, ant, config_source, @build_environment)
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
        jvm = to_jvm(config_source['jvm'] || '1.5', 'tests')
        args = all_remaining_arguments
        args << 'javahome=%s' % jvm.home.to_s unless args.find { |arg| arg.starts_with?('javahome=') }
        do_run_class(jvm, 'com.tc.simulator.distrunner.ControlSetup', args, config_source.as_array('jvmargs') || [ ],
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

    # Refreshes the CruiseControl config; this is called by the monkey so that changes in the
    # CruiseControl configuration can get automatically picked up. Uses a separate properties
    # file for overrides that are given on the command line to the Ruby 'monkey' tool, since
    # otherwise they'll be lost when we regenerate the configuration.
    def refresh_cruisecontrol_config
      depends :init

      raise RuntimeError, "You must specify a monkey name via the 'monkey-name' configuration property" if config_source['monkey-name'].nil?
      command = 'ruby'
      args = [ FilePath.new(config_source['tc.build-control.cruise-control.monkey-root'], "monkey.rb").canonicalize.to_s, 'generate-config', config_source['monkey-name'], '--source=' + File.join(File.dirname(__FILE__), "..", "..") ]

      overrides_file = config_source['cruise-control.command-line-data-file']
      unless overrides_file.blank?
          File.open(overrides_file, "r") do |file|
              file.each { |line| args << "%s=%s" % [ $1, $2 ] if line =~ /^\s*([^=]+?)\s*=\s*(.*?)\s*$/ }
          end
      end

      @platform.exec('ruby', *args)
   end

    # Runs the crash tests. Uses the internal configuration source to set the required property.
    def check_crashtests
        depends :init, :compile
        @internal_config_source['tc.tests.configuration.transparent-tests.mode'] = 'crash'
        run_tests(FixedModuleTypeTestSet.new([ 'dso-crash-tests', 'dso-spring-crash-tests' ], [ 'system' ]))
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
    def check_prep(module_name, test_type)
        depends :init, :compile
        test_set = FixedModuleTypeTestSet.new([ module_name ], [ test_type ])
        if test_set.validate(@module_set, @script_results)
            results_dir = FilePath.new(@build_results.build_dir, "externally-run-tests").delete.ensure_directory
            prepare_and_run_block_on_tests(test_set,
                TestRunResults.new(results_dir), Proc.new { |testrun| testrun.prepare_for_external_run })
        end
    end

    # Regenerates the JAR containing the class files that represent the XML schema.
    # You must rerun this whenever you change the .xsd files -- that is, if you want
    # your changes to show up. ;)
    def generate_config_classes
        schema_dir = @static_resources.config_schema_source_directory(@module_set)
        dest_jar = @static_resources.compiled_config_schema_jar(@module_set)
        generated_source_dir = @build_results.config_schema_generation_directory

        text = <<END
BUILDING NEW CONFIG SCHEMA JAR.

This is a pretty big deal: you are creating new Java classes
to correspond to the XML Schema currently in

    %s

The JAR at

    %s

will be overwritten; you must make sure you CHECK THIS FILE IN
to source-code control. (We check this file in, rather than
generating it each time, both to save time and to avoid
accidental changes to our config schema.)
END
        puts text % [ schema_dir.to_s, dest_jar.to_s ]

        generated_source_dir.delete_recursively(ant)

        # FIXME 2005-09-27 andrew - We really should be including:
        #
        #    :executable => @jvm_set['compile-%s' % @module_set['common'].compiler_version].java.to_s
        #
        # as an attribute here. However, doing so seems to cause XMLBeans to not
        # actually work; you get "Unrecognized option: -d" from the JVM. Once they
        # fix XMLBeans (I'm assuming it's their error), we should put this back
        # so we make sure we compile with the right JVM.
        ant.xmlbean(:destfile => dest_jar.to_s,
            :debug => true, :classpath => @module_set['common'].subtree('src').classpath(@build_results, :full, :runtime).to_s,
            :srcgendir => generated_source_dir.to_s) {
            ant.fileset(:dir => schema_dir.to_s, :includes => '*.xsd,*.xsdconfig')
        }
    end

    # A target for the monkeys to run when you just want to test that they're
    # working correctly. We run this test because it's one of the few that's
    # relevant to the monkey system itself.
    def check_monkeytest
        check_one('UsingCorrect14JVMTest')
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

        jvm = to_jvm(config_source['jvm'] || 'compile-%s' % build_module.compiler_version.to_s, 'compile')
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
                boot_jar = BootJar.new(@build_results, jvm, home.dir, @module_set, @ant, @platform, home.config_file).ensure_created
                jvmargs << '-Xbootclasspath/p:%s' % boot_jar.path.to_s
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

        subtree.run_java(ant, 'com.tc.server.TCServerMain', home.dir.to_s, @jvm_set['compile-%s' % build_module.compiler_version],
            config_source.as_array('jvmargs'), all_remaining_arguments, { 'tc.install-root' => home.dir.to_s }, @build_results, @build_environment)
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

        puts "For subtree '%s/%s', the %s %s classpath is:" % [ module_name, subtree_name, scope, type ]
        puts ""
        puts classpath
    end

    # Creates a boot JAR. jvm_spec (the argument) can be either '1.4', '1.5', or a path to a
    # JAVA_HOME. Creates the boot JAR for this JVM, and prints out where it put it.
    def create_boot_jar(jvm_spec)
        depends :init, :compile

        jvm = to_jvm(jvm_spec, 'tests')
        output_path = @build_results.tools_home
        output_path = FilePath.new(config_source['dest']) unless config_source['dest'].nil?
        boot_jar = BootJar.new(@build_results, jvm, output_path, @module_set, ant, @platform, @static_resources.dso_boot_jar_config_file)
        boot_jar.ensure_created
        puts ""
        puts "Boot JAR for JVM '%s' successfully created at '%s'." % [ jvm.to_s, boot_jar.path.to_s ]
    end

    # Regenerates the old-style (monolithic) Eclipse project.
    def eclipse
        depends :init

        eclipseBuilder = EclipseProjectBuilder.new(true, ant)
        @module_set.each { |the_module| the_module.eclipse(@build_environment, eclipseBuilder) }
        eclipseBuilder.generate
    end

    # Regenerates Eclipse projects for all the modules. Only overwrites .classpath files; will not
    # change existing project files, if they're present.
    def meclipse
        depends :init
        eclipseBuilder = EclipseProjectBuilder.new(false, ant)
        @module_set.each { |the_module| the_module.eclipse(@build_environment, eclipseBuilder) }
    end

    # Shows all configuration properties we're using.
    def show_config
        puts "Building with configuration:"
        puts "========================================================================"
        puts ""
        puts configuration_summary
    end

    # Does nothing; can be a useful target occasionally. (No, really.)
    def do_nothing
      # do a whole lot of nothing
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
        elsif target.to_s =~ /^(create|publish)_(.*)\s*$/ && /_package$\s*$/
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

    private
    # The full path to the build archive, including directory.
    def full_build_archive_path
        FilePath.new(build_archive_dir, short_build_archive_path.to_s)
    end

    # The short path to the build archive -- i.e., everything that's produced by the archive tag.
    def short_build_archive_path
        @archive_tag.to_path("build-archive", "jar")
    end

    # A 'pattern' for the build archive. This is used only to put it into a monkey XML file, so that
    # later parts of the monkey infrastructure -- like the log publisher -- can put other files at the
    # same location as the build archive, by using this path. This leaves the buildsystem in full
    # control of the archive path, making it very easy to change in the future.
    def build_archive_path_pattern
        @archive_tag.to_path("${FILENAME}", "${EXTENSION}")
    end

    # Given a 'JVM key', which can be either a pathname on the filesystem, a string like 'compile-1.4', or
    # a simple name like '1.4', finds the appropriate JVM. (If it's a simple name like '1.4', uses the
    # specified default_prefix as a prefix -- e.g., if the default_prefix is 'compile', it will look for
    # a JVM named 'compile-1.4'.)
    def to_jvm(jvm_key, default_prefix)
        if FileTest.directory?(FilePath.new(jvm_key).to_s)
            jvm = JVM.new(platform, FilePath.new(jvm_key).to_s)
            jvm.validate("the specified JVM")
        elsif @jvm_set.has?(jvm_key)
            jvm = @jvm_set[jvm_key]
        elsif @jvm_set.has?('%s-%s' % [ default_prefix, jvm_key ])
            jvm = @jvm_set['%s-%s' % [ default_prefix, jvm_key ]]
        else
            raise RuntimeError, "The JVM you specified, '%s', is neither a valid Java home, nor a valid JDK we already know about. We know about JDKs: %s" %
                [ jvm_key, @jvm_set.keys.join(", ") ]
        end
        jvm
    end

    # Runs the given class, using the given JVM, with the given arguments and JVM arguments, against the
    # given subtree.
    def do_run_class(jvm, classname, arguments, jvmargs, subtree)
        home = TerracottaHome.new(@static_resources, @build_results.tools_home).prepare(ant)

        puts "Running:"
        puts "    against module '%s', subtree '%s'," % [ subtree.build_module.name, subtree.name ]
        puts "    using the JVM at '%s'," % jvm.home.to_s
        puts "    in directory '%s'" % home.dir.to_s
        puts ""
        puts "    java %s %s %s" % [ jvmargs.join(" "), classname, arguments.join(" ") ]
        puts ""

        subtree.run_java(ant, classname, home.dir, jvm, jvmargs, arguments, { }, @build_results, @build_environment)
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
        have_started_at_least_one_test = false

        begin
            prepare_and_run_block_on_tests(test_set, testrun_results, Proc.new do |testrun|
                have_started_at_least_one_test = true
                testrun.run(@script_results)
            end)
        ensure
            raise unless have_started_at_least_one_test

            testrun_record.tearDown

            puts "\n\n"
            puts "========================================================================"
            puts "Test Results:\n\n"
            puts testrun_record.dump

            puts "\n\n"
            puts "========================================================================"
            if testrun_record.failed?
                puts "     Tests FAILED. See results above."
            elsif testrun_record.total_suites == 0
                puts "     No tests ran at all! Something's wrong -- check your configuration or call."
                @script_results.failed("No tests ran at all! Something's wrong -- check your configuration or call.")
            else
                puts "     Tests passed."
            end
            puts "  Testrun directory: %s" % testrun_results.root_dir.to_s
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
    def prepare_and_run_block_on_tests(test_set, testrun_results, testrun_proc)
        test_runs = { }

        test_set.run_on_subtrees(@module_set) do |subtree, test_patterns|
            test_runs[subtree] = subtree.test_run(@static_resources, testrun_results, @build_results, @build_environment, config_source,
                @jvm_set, ant, platform, test_patterns, tests_aggregation_directory)

            test_runs[subtree].setUp
        end

        test_set.run_on_subtrees(@module_set) do |subtree, test_patterns|
            begin
                testrun_proc.call(test_runs[subtree])
            ensure
                test_runs[subtree].tearDown
            end
        end
    end

    # Finds the JVMs that we need to use -- one each for compiling and testing, for 1.4 and 1.5.
    # This is where the 'run-1.4-tests-with-1.5' property comes into play; we assign the
    # 'tests-1.4' JVM to a 1.4 or 1.5 JVM, based on how this property is set.
    def find_jvms

        @jvm_set = JVMSet.new
        compile_14 = JVM.from_config(platform, config_source, "the JVM used to compile Java 1.4 code", "1.4.0_0", "1.4.999_999", "jvms.compile-1.4", "JAVA_HOME_14")
        compile_15 = JVM.from_config(platform, config_source, "the JVM used to compile Java 1.4 code", "1.5.0_0", "1.5.999_999", "jvms.compile-1.5", "JAVA_HOME_15")

        raise RuntimeError, "You must specify a valid 1.4 JVM using the JAVA_HOME_14 configuration property (e.g., via the 'TC_JAVA_HOME_14' environment variable." if compile_14.nil?
        raise RuntimeError, "You must specify a valid 1.5 JVM using the JAVA_HOME_15 configuration property (e.g., via the 'TC_JAVA_HOME_15' environment variable." if compile_15.nil?

        @jvm_set.set('compile-1.4', compile_14)
        @jvm_set.set('compile-1.5', compile_15)

        tests_15 = JVM.from_config(platform, config_source, "the JVM used to run tests against Java 1.5 code", '1.5.0_0', '1.5.999_999', "jvms.tests-1.5", "JAVA_HOME_TESTS_15")
        if tests_15.nil?
            @jvm_set.alias('tests-1.5', 'compile-1.5')
        else
            @jvm_set.set('tests-1.5', tests_15)
        end

        run14_tests_with_15 = config_source['run-1.4-tests-with-1.5']

        # here we want to check if an appserver only run swith certain JDK, ie. tomcat55 only runs with jdk15,
        # weblogic8 only with jdk14
        #tc.tests.configuration.appserver.factory.name=tomcat5
        #tc.tests.configuration.appserver.major-version=5.0
        #tc.tests.configuration.appserver.minor-version=28

        appservers_jdk = YAML::load(File.open("appservers.yml"))
        theappserver = "%s-%s.%s" % [ config_source["tc.tests.configuration.appserver.factory.name"],
                              config_source["tc.tests.configuration.appserver.major-version"],
                              config_source["tc.tests.configuration.appserver.minor-version"] ]

        if appservers_jdk.has_key?(theappserver)
            run14_tests_with_15 = appservers_jdk[theappserver] == 1.5 ? 'true' : 'false'
        end

        if run14_tests_with_15 =~ /^\s*true\s*$/i
            @jvm_set.alias('tests-1.4', 'tests-1.5')
        else
            tests_14 = JVM.from_config(platform, config_source, "the JVM used to run tests against Java 1.4 code", '1.4.0_0', '1.5.999_999', "jvms.tests-1.4", "JAVA_HOME_TESTS_14")
            if tests_14.nil?
                @jvm_set.alias('tests-1.4', 'compile-1.4')
            else
                @jvm_set.set('tests-1.4', tests_14)
            end
        end
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
            'branch' => @build_environment.current_branch,
            'platform' => @build_environment.platform,            
            'target' => config_source['tc.build-control.build.target'],
            'jvm-tests-1.4' => @jvm_set['tests-1.4'].short_description,
            'jvm-tests-1.5' => @jvm_set['tests-1.5'].short_description,            
        }        

        # Parameters data.
        parameters_data = {
          'host' => @build_environment.build_hostname,          
          'monkey-name' => config_source['monkey-name']
        }

        # Extra data.
        extra_environment_data = {
            'revision' => @build_environment.current_revision,
            'tc.build-archive.path.pattern' => build_archive_path_pattern.to_s
        }

        # Test configuration data, which goes in the configuration section.
        test_config_data = { }
        config_source.keys.each do |key|
            test_config_data[key] = config_source[key] if key =~ /^tc\.tests\.configuration\..*/i
        end

        File.open(@build_results.build_information_file.to_s, "w") do |file|
            file << "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n\n"
            file << "<configinfo>\n"

            file << "  <monkeydb>\n"
            file << "    <configuration>\n"

            write_keys(file, configuration_data, configuration_data.keys)
            write_keys(file, test_config_data, test_config_data.keys)

            file << "    </configuration>\n"
            file << "    <parameters>\n"

            write_keys(file, parameters_data, parameters_data.keys)

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

