#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Defines the base classes for all Terracotta buildsystems. Basically, this is infrastructure
# that's generic to pretty much any conceivable way of building our software; it does not
# include the stuff that defines modules, knows about how we lay out our build directory,
# and so on, but rather things that report the results of the build, measure its duration,
# parse arguments, contain configuration properties, and so on.

# A constant defining the property we put a list of all properties into, when you ask our
# Ant to report all the properties it knows about.
ANT_PROPERTY_LIST_PROPERTY_NAME_BASE = 'property_list_property_'

# A variable containing a suffix to append to the above.
$antPropertyListPropertyNextIndex = 0

# The current list of cached property names.
$antCachedPropertyNames = nil

# This is a subclass of AntBuilder that adds a few useful methods. This is actually
# exceptionally tricky -- AntBuilder inherits from Builder (which was designed to let you
# do easy XML generation in Ruby by doing things like 'xml.foo(bar => baz) { xml.quux(a => b) }'),
# which removes nearly all of the built-in methods that Ruby supplies on Object. So we have
# to be careful to only call the remaining methods.
class TerracottaAnt < Builder::AntBuilder
  # Creates a new AntBuilder. Initializes the 'propertyselector' task from ant-contrib,
  # which we use. (The ant-contrib JAR must therefore be in the CLASSPATH of the JVM
  # running JRuby here.)
  def initialize
    super(:debug => false, :backtrace => :filtered)
    taskdef(:name => 'propertyselector', :classname => 'net.sf.antcontrib.property.PropertySelector')
  end

  # Creates a JAR file according to the given options.  Most options are passed
  # directly to the Ant 'jar' task, so any option understood by the jar task
  # can be passed into this method.  In addition to these options, the following
  # options are also available:
  #
  # :include_license:: Indicates whether or not the Terracotta license file should
  #     be included in the JAR.  Defaults to true.
  # :include_thirdparty_license:: Indicates whether or not the thirdparty license
  #     file should be included in the JAR.  Defaults to true.
  def create_jar(jar_file, options, &block)
    def options.get_and_delete(key, default_value)
      if self.has_key?(key)
        result = self[key]
        self.delete(key)
      else
        result = default_value
      end
      result
    end

    include_license = options.get_and_delete(:include_license, true)
    include_thirdparty_license = options.get_and_delete(:include_thirdparty_license, true)

    options[:destfile] = jar_file.to_s
    self.jar(options, &block)

    # Now add the license files to the JAR if necessary.
    static_resources = Registry[:static_resources]
    license_files = []
    license_files << static_resources.license_file if include_license
    license_files << static_resources.thirdparty_license_file if include_thirdparty_license
    license_files.each do |file|
      directory = file.directoryname.to_s
      self.jar(:destfile => jar_file.to_s, :update => 'true',
        :basedir => directory, :includes => file.filename.to_s)
    end
  end

  # Performs a filtered copy from src_dir to dest_dir. Filtering is controlled
  # by the filters_def hash.
  def filtered_copy(src_dir, dest_dir, filters_def = Registry[:filters_def])
    self.copy(:todir => dest_dir.to_s) do
      self.filterset do
        filters_def['filters'].each do |filter|
          token = filter.keys.first
          value = filter[token]
          self.filter(:token => token, :value => value)
        end
      end

      filters_def['filtered_files'].each do |includes_pattern|
        self.fileset(:dir => src_dir.to_s, :includes => includes_pattern)
      end
    end

    self.copy(:todir => dest_dir.to_s) do
      self.fileset(:dir => src_dir.to_s, :includes => '**/*',
        :excludes => filters_def['filtered_files'].join(','))
    end
  end

  # Fetches the Ant property with the given name; raises an exception if it doesn't exist, unless
  # you specify a default. We also transform the key passed in to make sure it contains only
  # numbers, letters, and underscores; other characters will be replaced with underscores. This is
  # because not all valid Ant property names are valid Ruby instance variable names, and
  # AntBuilder does the same transformation if Ant tries to set properties that aren't valid
  # Ruby instance variable names.
  #
  # AntBuilder exposes properties as instance variables, so we have to look it up that way. Also,
  # we have to use #instance_eval -- the simpler Ruby methods for doing this are removed by
  # AntBuilder.
  def get_ant_property(key, default_value=:no_default_specified)
    begin
      key = key.gsub(/[^A-Za-z0-9_]/, '_')
      puts :warn, "Attempting to include Ant property '%s'." % key unless key =~ /^[A-Za-z0-9_]+$/
      out = instance_eval("@%s" % key.to_s)
    rescue => e
      raise if default_value == :no_default_specified
      out = default_value
    end
    out
  end

  # Returns a hash of the current values of all Ant properties.
  def all_ant_properties
    _get_ant_properties_as_hash
  end

  # Sets an Ant property to a particular value. If you pass in a key that's not a valid Ruby
  # instance variable name, you will get an exception back -- you have been warned.
  #
  # We have to use instance_eval here, too, because
  def set_ant_property(key, value)
    instance_eval("@%s = '%s'" % [ key.to_s, value.to_s ])
  end
end

# This class exists because, in JRuby, it seems like Kernel.exit() is broken -- the parent process
# just plain doesn't get back the exit code. Instead, we call java.lang.System.exit() instead.
class ExitCodeHelper
  def self.exit(code)
    JavaSystem.exit(code)
  end
end

# The base class for all Terracotta builders. We do NOT extend AntBuilder -- see the discussion above;
# AntBuilder just does too many weird things with built-in Ruby methods to make that seem safe or
# sane. Instead, we maintain an instance of AntBuilder as a member variable.
class TerracottaBuilder
  require 'optparse'

  attr_reader :ant, :platform, :basedir, :config_source

  # Creates a new TerracottaBuilder instance with the given default target. arguments should be the
  # list of arguments passed on the command line; these will be parsed for targets, arguments to
  # targets, and configuration properties.
  def initialize(default_target, arguments)
    option_parser = OptionParser.new
    option_parser.on('--no-ivy') { @no_ivy = true }
    option_parser.on('--no-compile') { @no_compile = true }
    option_parser.on('--no-demo') { @no_demo = true }
    option_parser.on('--no-schema') { @no_schema = true }
    option_parser.on('--no-jre') { @no_jre = true }
    option_parser.on('--no-external-resources') { @no_external_resources = true }
    option_parser.on('--no-tims') { @no_tims = true }
    option_parser.on('--no-extra') {
      @no_tims = true
      @no_external_resources = true
      @no_demo = true
      @no_schema = true
    }

    option_parser.on('--no-no') do 
      @no_ivy = true
      @no_tims = true
      @no_external_resources = true
      @no_demo = true
      @no_schema = true
    end
    option_parser.on('--emma') do Registry[:emma] = true end
    
    @start_time = Time.now
    @basedir = FilePath.new(File.dirname(File.expand_path(__FILE__)), "..").canonicalize
    Registry[:basedir] = @basedir
    puts("Building with base directory: '#@basedir'.")
    puts
    @flavor = OPENSOURCE
    @default_target = default_target
    @ant = TerracottaAnt.new
    Registry[:ant] = @ant
    @platform = CrossPlatform.create_implementation(:ant => @ant)
    Registry[:platform] = @platform
    
    # The CommandLineConfigSource actually parses its arguments, and returns only the ones
    # that aren't configuration property settings (e.g., of the form 'a=b').
    arguments = option_parser.parse(arguments)
    @arguments, command_line_source = CommandLineConfigSource.from_args(arguments)
    @internal_config_source = InternalConfigSource.new
    Registry[:internal_config_source] = @internal_config_source
    @config_source = create_config_source(command_line_source, @internal_config_source)
    Registry[:config_source] = @config_source
    Registry[:command_line_config] = command_line_source

    @script_results = ScriptResults.new

    if Registry[:emma]
      Registry[:emma_home] = FilePath.new(@basedir.to_s, "..", "..", "buildsystems", "emma-2.0.5312").canonicalize.to_s
      fail("EMMA_HOME does not exist: #{Registry[:emma_home]}") unless File.exists?(Registry[:emma_home])
      Registry[:emma_lib] = "#{Registry[:emma_home]}/lib/emma.jar"
      puts "EMMA_HOME: #{Registry[:emma_home]}"
    end

    # XXX: this is a hack to get around jruby script converting JAVA_HOME to unix path
    begin
      if `uname` =~ /CYGWIN/i
        ENV['JAVA_HOME'] = `cygpath -w #{ENV['JAVA_HOME']}`.strip
      end
    rescue
      # do nothing
    end

    reset
  end

  # Runs the builder -- that is, runs the targets specified on the command line, or the
  # default target if none were specified.
  def run
    # We don't want to mutate the original arguments, so we make a copy.
    actual_targets = @arguments.dup
    actual_targets = [ @default_target ] if actual_targets.empty?

    write_failure_file_if_necessary_at_beginning

    begin
      do_run(actual_targets)
    rescue => e
      # This makes sure that any kind of arbitrary exception from the rest of the
      # buildsystem gets correctly reported as a failure.
      message = "Received exception from the build system: #{e} [#{e.class}]\n" +
        e.backtrace.join("\n     ")
      @script_results.failed(message)
    ensure
      end_time = Time.now
      # This is so that, even if the series of scripts leading from, say, CruiseControl to this
      # code doesn't correctly carry back the exit code (as it inexplicably seems not to on
      # Windows), the parent can tell if it passed or not.
      write_failure_file_if_necessary_at_end

      # Support for making build archives.
      # archive only if "force-archive=true", or when the run fails
      if ((@config_source['monkey-name'] && @script_results.failed?) ||
            @config_source["force-archive"] =~ /true/)
        archive_build_if_necessary
      end

      # Print out the duration and the results at the end.
      puts ""
      puts "[%8.2f seconds] %s" % [ (end_time - @start_time), @script_results.to_s ]
      ExitCodeHelper.exit(@script_results.result_code)
    end


  end

  # Resets the dependency-tracking mechanism.
  def reset
    @targets_run = [ ]
  end

  # Executes the given subcommand of the tim-get tool with the given flags.
  # Some flags may be added to the command-line automatically based on flags
  # passed to tcbuild itself.  For instance, if the kit-type=final flag is
  # passed to tcbuild, then this method will automatically set the
  # includeSnapshots flag to false.
  #
  # Note that tim-get itself is built very late in the dist stage of the build
  # process and that this method will not function until tim-get has been
  # created.  Therefore, this method can only be used for post-processing tasks
  # such as installing TIMs from the Forge into the kit after it has been built.
  def tim_get(subcommand, *flags)
    tim_get_command(subcommand, *flags) do |command|
      system(*command)
    end
  end

  # Executes the given subcommand of the tim-get tool with the given flags,
  # and returns the output as a string.  This method behaves just like the
  # tim_get method except that it captures and returns stdout.
  def tim_get_output(subcommand, *flags)
    tim_get_command(subcommand, *flags) do |command|
      %x[#{command.join(' ')}]
    end
  end

  def maven(goals, *args)
    mvn_cmd = [FilePath.new('mvn').batch_extension.to_s]
    mvn_cmd << goals
    mvn_cmd += args
    system(*mvn_cmd)
  end

  protected

  def tim_get_command(subcommand, *flags)
    unless @tim_get
      @tim_get = FilePath.new(product_directory, 'bin', 'tim-get').script_extension
      unless File.exist?(@tim_get.to_s)
        raise("Cannot find tim-get executable at expected location: #{@tim_get}")
      end
      unless File.executable?(@tim_get.to_s)
        raise("tim-get script exists, but is not executable")
      end
    end

    prefix = 'org.terracotta.modules.tool'
    include_snapshots = @config_source['kit-type'] == 'final' ? false : true
    java_opts = ["-D#{prefix}.includeSnapshots=#{include_snapshots}",
      "-D#{prefix}.cache=#{self.tim_get_index_cache}"]
    if index_url = @config_source['tim-get.index.url']
      java_opts << "-D#{prefix}.dataFileUrl=#{index_url}"
    end

    if relative_url_base = @config_source['tim-get.index.relativeUrlBase']
      java_opts << "-D#{prefix}.relativeUrlBase=#{relative_url_base}"
    end

    ENV['JAVA_OPTS'] = java_opts.join(' ')

    result = yield [@tim_get.to_s.gsub(/\\/, '/'), subcommand.to_s, *flags]

    unless $?.exitstatus == 0
      $stderr.puts("tim-get command failed with exit status '#{$?.exitstatus}'")
      $stderr.puts("tim-get command: #{tim_get_command.join(' ')}")
      $stderr.puts("JAVA_OPTS: #{ENV['JAVA_OPTS']}")
      raise("tim-get command failed.  Exit status: #{$?.exitstatus}")
    end

    ENV['JAVA_OPTS'] = nil

    result
  end

  def tim_get_index_cache
    File.join("build", 'tcbuild-tim-get')
  end

  def delete_tim_get_index_cache
    FileUtils.rm_rf(tim_get_index_cache) if File.exists?(tim_get_index_cache)
  end

  # Where should we archive the build to? Returns nil if none. Value comes from the config source.
  def build_archive_dir
    out = config_source['build-archive-dir']
    out = nil if (out != nil && out.downcase == 'none')
    out = FilePath.new(out) unless out.nil?
    out
  end

  # Creates the configuration source. This is a CompositeConfigSource that looks, in order, at the
  # internal config source (highest priority), the command line, 'build-config.local', 'build-config.global',
  # and the environment.
  #
  # (The internal configuration source is used only to allow targets to explicitly set configuration properties
  # that can't be overridden; this can be a useful technique sometimes.)
  def create_config_source(command_line_source, internal_config_source)
    CompositeConfigSource.new([
        internal_config_source,
        command_line_source,
        StandardFileConfigSource.new('build-config'),
        EnvironmentConfigSource.new(@ant)])
  end

  # Returns a summary of all configuration data, as a string, in a form useful for printing out to a user.
  def configuration_summary
    keys = config_source.keys.sort
    max_length = 0
    keys.each { |key| max_length = [ max_length, key.length ].max }
    str = "%" + (max_length + 2).to_s + "s: %s\n"
    out = ""

    keys.each do |key|
      out += str % [ key, config_source[key] ]
    end

    out
  end

  # If you call this method from a target in a subclass, it will ensure that the given list of targets
  # has been run before this method returns. These 'stack' in the sense that if someone calls
  # 'tcbuild bar baz', and bar and baz each depend on foo, foo will only get once (from bar's call to
  # depends(:foo)), not twice. Also, 'tcbuild foo bar' would also only run foo once.
  #
  # targets should be an array of symbols, each representing the name of the method the caller
  # depends upon.
  def depends(*targets)
    targets.each do |target|
      target = target.to_sym
      unless @targets_run.include?(target)
        @targets_run << target
        method(target).call
      end
    end
  end

  # This gets called when you try to invoke a target that doesn't exist. Subclasses can override this
  # to do special target handling. (For example, the main BaseCodeTerracottaBuilder overrides this
  # to provide implicit support for targets like 'check_spring_unit' and 'check_allbutspring_system' --
  # instead of creating a new explicit target for every possible combination, it just looks at the
  # modules, module groups, and test types, and tries to see if it can figure out what you meant.)
  def target_missing(target)
    raise RuntimeError, "No target named '%s' exists in this build file." % target
  end

  # Called when you should archive the build; passes in the directory you should archive it to.
  # Subclasses should override this so it actually does something.
  def archive_build_if_necessary(target_directory)
    # Nothing here in superclass
  end

  private
  # If the configuration property 'failures_file' is set, calls the procedure with a FilePath pointing
  # to that file. If not, does nothing.
  def with_optional_failures_filename(&proc)
    filename = config_source['failures_file']

    unless filename.blank?
      path = FilePath.new(filename)
      proc.call(path)
    end
  end

  # Writes out a failures file at the beginning. This is our deadman's switch -- pretty important.
  # If all we did was write this out at the end, sure, we could try to catch exceptions and all kinds
  # of stuff to make sure we really truly *always* wrote this out at the end, but certain things (like
  # JVM crashes, hard hangs, etc.) would still keep us from doing this. Instead, we write out this
  # "it done broke" file at the beginning, and replace it with a real one when the build completes;
  # this way, if something breaks hard-core in the middle, we may not know exactly what broke, but
  # we know that something _did_ break -- which is the really important part.
  def write_failure_file_if_necessary_at_beginning
    with_optional_failures_filename do |filename|
      File.open(filename.to_s, "w") do |file|
        file << "Build CRASHED; this error is written out when the build starts, and replaced when it fails or succeeds normally. This is a hard-core problem like a JVM crash of the buildsystem, or something similar, instead.\n"
      end
    end
  end

  # Writes out a failures file at the end, with any failures encountered during the build. If there
  # were no failures, deletes the failures file, if it exists.
  def write_failure_file_if_necessary_at_end
    with_optional_failures_filename do |filename|
      if @script_results.failed?
        File.open(filename.to_s, "w") do |file|
          @script_results.each do |reason|
            file << reason << "\n"
          end
        end
      else
        File.delete(filename.to_s) if FileTest.exist?(filename.to_s)
      end
    end
  end

  # Runs the specified targets. Assigns @remaining_arguments, which targets can get at
  # to pull variable numbers of arguments from the argument list. (This is necessary because,
  # among other things, JRuby has problems with methods that take variable numbers of
  # arguments -- the *argument syntax as the last argument of a method -- and proc.call().)
  def do_run(actual_targets)
    @remaining_arguments = actual_targets
    do_run_arguments
  end

  # Runs the target specified by the first of the remaining arguments.
  def do_run_arguments
    unless @remaining_arguments.empty?
      target = next_argument.to_sym
      puts "Running target: #{target}"
      if respond_to?(target, false)
        already_run = @targets_run.include?(target)
        @targets_run << target unless already_run

        method = method(target)
        if method.arity > 0
          raise ArgumentError, "Not enough arguments for target '%s'; %d remain." % [ target, @remaining_arguments.length ] if method.arity > @remaining_arguments.length
          args = @remaining_arguments[0..method.arity - 1]
          @remaining_arguments = @remaining_arguments[method.arity..-1]
        elsif method.arity == 0
          args = [ ]
        else
          raise ArgumentError, "Not enough arguments for target '%s'; %d remain." % [ target, @remaining_arguments.length ] if (method.arity.abs - 1) > @remaining_arguments.length
          args = @remaining_arguments
          @remaining_arguments = [ ]
        end

        method.call(*args) unless already_run && method.arity == 0

        do_run(@remaining_arguments)
      else
        target_missing(target)
      end
    end
  end

  # Gets the next argument, removing it from the remaining arguments.
  def next_argument
    @remaining_arguments.shift
  end

  # Returns a list of all remaining arguments, removing it from the remaining arguments.
  def all_remaining_arguments
    out = @remaining_arguments
    @remaining_arguments = [ ]
    out
  end
end
