#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# A set of classes useful for dealing with JVMs. Lets you compare JVM versions,
# keep track of a set of JVMs, and find various commands from a JVM, specify
# necessary versions, and so on.

class JvmVersionMismatchException < StandardError
end

# Represents the version of a JVM. This is typically something like '1.4.2_07', but
# some JVMs (like IBM's) can be just '1.4.2', too.
class JavaVersion
  include Comparable

  JAVA_MIN_VERSION = '0.0.0_000'
  JAVA_MAX_VERSION = '99999.999.999_999'

  attr_reader :major, :minor, :patch, :release

  # Creates a new representation of a version, as specified by a string. This can
  # currently be either something like '1.4.2' or something like '1.4.2_07'.
  def initialize(version_string)
    if version_string =~ /^\s*(\d+)\.(\d+)\.(\d+)(?:_(\d+))?(?:-\w+)?\s*$/
      @major, @minor, @patch = $1.to_i, $2.to_i, $3.to_i
      @release = $4.blank? ? 0 : $4.to_i
    else
      raise "Version '#{version_string}' isn't a valid Java version"
    end
  end

  # Is this version the same as the other version up to and including the
  # minor version, but not including the patch or release?
  def same_minor_version?(other)
    major == other.major && minor == other.minor
  end

  # Returns a new JavaVersion object that is the same as the major and minor
  # version of this JavaVersion.  For example, if this JavaVersion object has
  # version 1.4.2_11, then this method will return a JavaVersion object that
  # has version 1.4.0.
  def release_version
    JavaVersion.new("#{@major}.#{@minor}.0")
  end

  # Returns a number less than zero, zero, or a number greater than zero as this
  # version is less than, exactly equal to, or greater than the other supplied version.
  def <=>(other)
    other = JavaVersion.new(other) if other.is_a?(String)

    out = major <=> other.major
    out = minor <=> other.minor if out == 0
    out = patch <=> other.patch if out == 0
    out = release <=> other.release if out == 0
    out
  end

  # Returns a string representation of this version.
  def to_s
    "%d.%d.%d_%02d" % [ @major, @minor, @patch, @release ]
  end
end

class InvalidJVM < StandardError; end


# Represents a JVM. This object knows how to tell you the JVM's home, where 'java'
# and 'javac' are, find the actual version of the JVM you point it at, make sure
# you've pointed it at an actual JVM (as opposed to some random directory off
# somewhere), and give you the type (e.g., 'hotspot') and version (e.g., '1.4.2_07') of the
# JVM.
class JVM
  # Creates a new JVM object. platform is the Platform object (see cross_platform.rb)
  # for this platform; java_home is the home directory of the JVM. (In other words,
  # 'java' should be at 'bin/java' with respect to this directory, and 'javac' should
  # be at 'bin/javac' with respect to this directory.)
  #
  # If data is supplied, then you can pass data[:minimum_version], which indicates
  # a minimum valid version for this JVM (as a string), and you can pass
  # data[:maximum_version], which indicates a maximum valid version for this JVM
  # (as a string). These are not required and can be set individually.
  #
  # Note: if you pass a minimum and/or maximum version, they are only checked when
  # you call #validate -- not at object-creation time.
  def initialize(java_home, data={ })
    @java_home = FilePath.new(java_home).canonicalize
    @java_path = FilePath.new(@java_home, "bin", "java").executable_extension
    @javac_path = FilePath.new(@java_home, "bin", "javac").executable_extension

    @min_version = data[:minimum_version] || JavaVersion::JAVA_MIN_VERSION
    @max_version = data[:maximum_version] || JavaVersion::JAVA_MAX_VERSION

    @actual_version = nil
  end

  def min_version
    JavaVersion.new(@min_version)
  end

  def max_version
    JavaVersion.new(@max_version)
  end

  # What's the path to the 'java' executable in this JVM? Returns a string.
  def java
    @java_path.to_s
  end

  # What's the path to the 'javac' executable in this JVM? Returns a string.
  def javac
    @javac_path.to_s
  end

  # What's the Java home for this JVM? Returns a FilePath object.
  def home
    @java_home
  end

  # Creates a JVM object from a config_source. Looks in turn for each of the config_property_names
  # in the config_source, and, if it's set, uses that as the JVM home. Proceeds to validate that
  # the specified JVM is at least the given min_version and at most the given max_version, using
  # descrip to generate any error messages.
  def self.from_config(descrip, min_version, max_version, *config_property_names)
    root = Registry[:config_source].search(*config_property_names)

    out = nil
    unless root.nil?
      out = JVM.new(root, { :minimum_version => min_version, :maximum_version => max_version })
      out.validate(descrip)
      out.check_version(descrip)
    end
    out
  end

  # Checks that this JVM is a valid JVM -- that is, has valid 'java' and 'javac' executables.
  def validate(descrip)
    message = "The Java home for #{descrip}, at '#{@java_home}', has no " +
      "'%s' executable at '#{@java_path}'. Please correct this " +
      "problem and try again."
    unless FileTest.file?(java)
      raise(InvalidJVM, message % 'java')
    end
    unless FileTest.file?(javac)
      raise(InvalidJVM, message % 'javac')
    end
    self
  end

  def valid?
    result = true
    begin
      self.validate(nil)
    rescue
      result = false
    end
    result
  end

  # Checks that the version of this JVM lies within the limits set by the :min_version and
  # :max_version arguments to the constructor.
  def check_version(descrip)
    if Registry[:config_source]["skip.jdk.check"] == "true"
      loud_message("skip.jdk.check detected. Enforcing of JDK version is skipped")
      return
    end
    
    unless @min_version.nil? && @max_version.nil?
      this_version = JavaVersion.new(actual_version)

      # Template for error messages if the JVM does not meet the version requirements
      message = "The version of #{descrip}, '#{this_version}', is %s than the %s required of '%s'."

      unless @min_version.nil?
        required_version = JavaVersion.new(@min_version)

        if this_version < required_version
          raise(InvalidJVM, message % ['less', 'minimum', required_version])
        end
      end

      unless @max_version.nil?
        required_version = JavaVersion.new(@max_version)

        if this_version > required_version
          raise(InvalidJVM, message % ['greater', 'maximum', required_version])
        end
      end
    end
  end

  # A short description of this JVM -- the type followed by the version. Therefore, something
  # like 'hotspot-1.4.2_07'.
  def short_description
    "#{self.actual_type}-#{self.version}"
  end

  # The type of this JVM -- something like 'hotspot' or 'jrockit'.
  def actual_type
    ensure_have_version
    @actual_type
  end

  # The release of this JVM, something like '1.4' or '1.6'.
  def release
    "#{version.major}.#{version.minor}"
  end

  # The actual version of this JVM, as a string. something like '1.4.2_07'.
  def actual_version
    ensure_have_version
    @actual_version
  end

  # A JavaVersion object representing the version of this JVM.
  def version
    @version ||= JavaVersion.new(actual_version)
  end

  # A human-readable string representation of this JVM.
  def to_s
    "<JVM at '#@java_home', version '#{actual_version}'>"
  end

  private

  # Makes sure the @actual_version and @actual_type member variables are set.
  def ensure_have_version
    unless @actual_version
      output, error = Registry[:platform].exec(@java_path, "-version")

      @actual_version = nil
      [ output, error ].each do |val|
        @actual_version = val if @actual_version.blank? && (! val.blank?)
        @actual_version = $1 if @actual_version =~ /java\s*version\s*\"([^\"]+)\"/
        @actual_type = "jrockit" if @type.blank? && val =~ /jrockit/i
        @actual_type = "hotspot" if @type.blank? && val =~ /hotspot/i
        @actual_type = "ibm"      if @type.blank? && val =~ /IBM J9 VM/
      end
    end
  end
end

# A set of JVMs. This lets you bind JVMs to names, alias JVMs (meaning the same JVM
# can show up as more than one key), and fetch JVMs out of it.
class JVMSet

  # This class serves as a stub for JVMs that are specified in jdk.def.yml
  # but that could not be found on the system.  This is not an immediate
  # error condition because some JVMs are not used by default.  Therefore,
  # instead of immediately producing an error, we use a NonExistantJVM object
  # to produce a user-friendly error message if and only if the non-existant
  # JVM is /used/ (as opposed to declared).
  class NonExistantJVM < JVM

    JVM.instance_methods(false).each do |m|
      define_method(m.to_sym) do |*args|
        raise_exception
      end
    end

    def initialize(name, search_names)
      @name = name
      @search_names = search_names
    end

    def valid?
      false
    end

    private

    # Raises an exception describing the non-existant JVM, and the
    # environment variables or configuration properties that may be used
    # to specify the location of the JVM.  The ignored argument is present
    # only for compatibility with the actual JVM.validate method.
    def raise_exception
      msg = "You must specify a valid #{@name} JRE using one of the " +
        "following environment variables or conmmand-line arguments: " +
        @search_names.join(', ')
      raise(InvalidJVM, msg)
    end
  end

  # Creates a new JVMSet.
  def initialize(jdk_defs = Hash.new)
    @jvms = Hash.new

    jdk_defs.each do |name, attributes|
      min_version = attributes['min_version']
      max_version = attributes['max_version']
      search_names = attributes['env']

      unless min_version && max_version && search_names
        raise "Invalid JVM specification: #{name}"
      end

      search_names = [search_names].flatten

      jvm = JVM.from_config(name, min_version, max_version, *search_names)
      if jvm
        @jvms[name] = jvm
        if aliases = attributes['alias']
          [aliases].flatten.each do |jvm_alias|
            @jvms[jvm_alias] = @jvms[name]
          end
        end
      else
        @jvms[name] = NonExistantJVM.new(name, search_names)
      end
    end
  end

  # Returns the JVM that's bound to the given key.
  def [](key)
    jvm = nil
    if @jvms.has_key?(key)
      jvm = @jvms[key]
    else
      jvm = find_jvm(:path => key) ||
        find_jvm(:name_like => /#{key}/) ||
        find_jvm(:version_like => /#{key}/)
    end
    jvm ? jvm.validate(key) : nil
  end

  # Sets a JVM for a the given key.
  def set(key, jvm)
    @jvms[key] = jvm
  end

  # Aliases JVMs; looking up new_key will return the same JVM as old_key after this
  # method returns.
  def alias(new_key, old_key)
    @jvms[new_key] = @jvms[old_key]
  end

  # Is there a JVM bound to the given key?
  def has?(key)
    @jvms.has_key?(key)
  end

  # What are all the available keys?
  def keys
    @jvms.keys
  end

  # Return a JVM object matching the given criteria, or nil if there is no
  # matching JVM.  If there is more than one matching JVM, an arbitrary JVM is
  # selected from among the matching JVMs.
  #
  # The following options may be passed as the criteria argument:
  #
  #   :name_like:: A regular expression that the name of the JVM must match
  #   :min_version:: The minumum version as a string
  #   :max_version:: The maximum version as a string
  #   :version_like:: A regular expression that the JVM version string must match
  #   :path:: The path to the JVM root directory.  This criteria is special in
  #           that if it cannot find a matching existing JVM, it will attempt
  #           to create one with the given path as the JAVA_HOME.  If it succeeds
  #           in creating it, the newly-created JVM will be returned, otherwise
  #           nil is returned.
  #
  # All criteria given are logically ANDed together.
  def find_jvm(criteria)
    name_like = criteria[:name_like] || /.*/
    min_version = JavaVersion.new(criteria[:min_version] || JavaVersion::JAVA_MIN_VERSION)
    max_version = JavaVersion.new(criteria[:max_version] || JavaVersion::JAVA_MAX_VERSION)
    path = criteria[:path]
    version_like = criteria[:version_like]

    @jvms.keys.sort.each do |key|
      jvm = @jvms[key]
      next unless jvm && jvm.valid?

      meets_criteria = false
      jvm_version = JavaVersion.new(jvm.actual_version)
      if key =~ name_like && jvm_version >= min_version && jvm_version <= max_version
        meets_criteria = true

        if meets_criteria && version_like
          meets_criteria = jvm_version.to_s =~ version_like
        end
      end

      if meets_criteria && path
        path_string = FilePath.new(path).canonicalize.to_s
        jvm_path = jvm.home.canonicalize.to_s
        meets_criteria = jvm_path == path_string
      end

      if meets_criteria
        return jvm
      end
    end

    if path
      if FileTest.directory?(path)
        created_jvm = JVM.new(path, :minimum_version => min_version.to_s,
          :maximum_version => max_version.to_s)
        begin
          created_jvm.check_version("created_jvm")
          created_jvm.validate("created_jvm")
        rescue
          created_jvm = nil
        end
      end
    end

    created_jvm
  end

  def add_config_jvm(config_jvm_name, build_config = Hash.new)
    config_source = Registry[:config_source]
    if config_jvm_value = config_source[config_jvm_name] || build_config[config_jvm_name]
      if has?(config_jvm_value)
        self.alias(config_jvm_name, config_jvm_value)
      else
        if created_jvm = find_jvm(:path => config_jvm_value)
          set(config_jvm_name, created_jvm)
        else
          raise("Value of '#{config_jvm_name}' is not valid")
        end
      end
    end
  end

  # A human-readable string representation of this JVMSet.
  def to_s
    out = "<JVMSet:\n"
    @jvms.keys.sort.each do |key|
      out += "  '#{key}': #{@jvms[key]}\n"
    end
    out += ">"
  end
end

