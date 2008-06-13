#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Contains classes that represent a configuration source -- a set of name-value pairs
# (where both name and value are strings) that control how we build.

# A module that gets mixed in to all configuration sources. Provides useful utility
# methods that are implemented in terms of the base methods. Requires the class that
# mixes this in to implement [] properly.
module ConfigSource
  include Enumerable

  def each
    for key in keys
      yield(key, self[key])
    end
  end

  # An exception subclass for exceptions having to do with the configuration system.
  class ConfigError < StandardError
  end

  # Takes a given line and returns a hash representing the name-value pair it contains,
  # an empty hash if it's a comment line or blank, or nil if it's not a valid name-value
  # pair.
  def self.parse_config_line(line)
    if line =~ /^\s*\#.*$/i || line =~ /^\s*$/i
      { }
    elsif line =~ /^\s*(-D)?(\S[^=]*?)\s*=\s*(.*?)\s*$/i
      { $2 => $3 }
    else
      nil
    end
  end

  # Returns the value of a given key, but raises an exception if it's not present instead
  # of returning nil (which is what [] should do).
  def required(key)
    out = self[key]
    raise ConfigError, "Configuration property '%s' is required; was not found in %s." % [ key, to_s ] if out.blank?
    out
  end

  # Returns the value of a key as an array of strings; the value should be a comma-separated
  # list. If it's not, you'll get back an array of one item -- the original value.
  def as_array(key)
    out = self[key]

    if out.blank?
      out = nil
    else
      out = out.split(/(?<!\\),/).collect { |val| val.gsub(/\\,/, ',').strip }
    end

    out
  end
end

# A configuration source that returns only values that you stuff into it. This
# can be used to let targets override configuration properties set elsewhere, since
# it's typically placed first inside the CompositeConfigSource.
class InternalConfigSource
  include ConfigSource

  def initialize
    @properties = { }
  end

  def []=(key, value)
    @properties[key] = value
  end

  def [](key)
    @properties[key]
  end

  def keys
    @properties.keys
  end

  def to_s
    "internal configuration source"
  end
end

# A configuration source that, when you look up keys in it, looks first for
# the key itself, then looks for the same key but prefixed with a constant
# string (as specified in the constructor). This lets you optionally prefix
# configuration properties with strings (to make 'tc.build-control.x=y' behave
# exactly the same as 'x=y'), which can be useful when you're calling tcbuild
# from external entities (like CruiseControl) that might have scads of their
# own properties. This prefix can be used to distinguish properties that
# control 'tcbuild' from ones that are used to affect JUnit tests, other systems
# (again, like CruiseControl) themselves, and so on.
#
# This source requires another source as input -- it simply applies the prefixing
# mechanism to data that comes from some other source.
class OptionallyPrefixingConfigSource
  include ConfigSource

  # Creates a new instance. prefix is the string that we should allow as a prefix;
  # source is the underlying ConfigSource from which we fetch our data.
  def initialize(prefix, source)
    @prefix = prefix
    @source = source
  end

  def [](key)
    out = @source[key]
    out = @source[@prefix + key] if out.blank?
    out
  end

  def search(*keys)
    prefixed_keys = keys.collect { |key| @prefix + key }
    merged_keys = []
    keys.each_index do |i|
      merged_keys << keys[i] << prefixed_keys[i]
    end

    @source.search(*merged_keys)
  end

  # Returns all keys present. This is the same list of keys that are present in
  # the underlying configuration source, but with the prefix removed (if present).
  # Therefore, the returned array will never, ever contain values that start with
  # the prefix specified in the constructor.
  def keys
    out = [ ]
    @source.keys.each do |key|
      if key.starts_with?(@prefix)
        out << key[@prefix.length..-1]
      else
        out << key
      end
    end
    out
  end

  def to_s
    "optionally prefixed with '%s': %s" % [ @prefix, @source.to_s ]
  end
end

# A configuration source that simply reads values from a file on startup, then
# regurgitates them on demand. The file's lines must be in the format accepted by
# ConfigSource.parse_line, which is basically 'name=value' format (with blank
# lines and lines beginning with a '#' ignored).
class SingleFileConfigSource
  include ConfigSource

  def initialize(filename)
    @filename = filename
    @properties = { }

    if FileTest.file?(filename)
      File.open(filename) do |file|
        lineno = 0
        file.each do |line|
          lineno += 1
          data = ConfigSource.parse_config_line(line)
          raise ConfigError, "'%s', line %d: Line is in an invalid format." % [ filename, lineno ] if data.nil?
          @properties.merge!(data)
        end
      end
    end
  end

  def [](key)
    @properties[key]
  end

  def keys
    @properties.keys
  end

  def to_s
    "file at '#{@filename}'"
  end
end

# A ConfigSource that returns values that are passed on the command line.
class CommandLineConfigSource
  include ConfigSource

  # Creates a new CommandLineConfigSource from the supplied list of arguments,
  # and returns two values -- the first is a list of the remaining passed-in
  # arguments, once configuration settings (e.g., of the format of 'x=y') are
  # removed, and the second is the created CommandLineConfigSource.
  def self.from_args(args)
    out_args = [ ]
    properties = { }

    can_be_property = true
    args.each do |arg|
      if arg == '--' && can_be_property
        can_be_property = false
        next
      end

      data = ConfigSource.parse_config_line(arg)
      if data != nil && can_be_property
        properties.merge!(data)
      else
        out_args << arg
      end
    end

    [ out_args, CommandLineConfigSource.new(properties) ]
  end

  def [](key)
    @properties[key]
  end

  def keys
    @properties.keys
  end

  def to_s
    "command line"
  end

  private
  def initialize(properties)
    @properties = properties.dup
  end
end

# A ConfigSource that gets its settings from the environment. For a key 'foo',
# it will return the value of the environment variable 'TC_foo', if present.  If
# 'TC_foo' is not present, it will return the value of the environment variable
# 'foo', or nil if it does not exist.
class EnvironmentConfigSource
  include ConfigSource

  def initialize(ant)
    @ant = ant
    @ant.property(:environment => 'env')
  end

  def [](key)
    key = key.gsub(/[- \.]/, "_")
    @ant.instance_eval("@env_TC_#{key}") || @ant.instance_eval("@env_#{key}")
  end

  def keys
    [ ]
  end

  def to_s
    "environment"
  end
end

# A composite ConfigSource: accepts a list of other ConfigSources as input,
# and, for each key, returns the value from the first config source that has a
# value for that key. This therefore 'composites' together several ConfigSources
# into what appears to be a single config source, giving priority to config sources
# that appear earlier in the list passed to its constructor.
class CompositeConfigSource
  include ConfigSource

  def initialize(sources)
    @sources = sources
  end

  def [](key)
    out = nil
    @sources.each do |source|
      out = source[key] if out.nil?
    end
    out
  end

  # Searches all config sources for each key in turn.  If a block is given, returns the first
  # value for which the block returns true.  If no block is given, returns the first non-nil
  # value.
  def search(*keys)
    @sources.each do |source|
      keys.each do |key|
        value = source[key]
        found = block_given? ? yield(key, value) : value
        if found
          return value
        else
        end
      end
    end

    nil
  end

  def keys
    @sources.collect { |source| source.keys }.flatten.uniq
  end

  def to_s
    @sources.join(", ")
  end
end

# A config source that takes its data from a pair of files with the same base
# name, one ending in '.local' and one ending in '.global'. The one ending in
# '.local' gets priority.
#
# This is how we implement our 'build-config.local overrides build-config.global'
# mechanism.
class StandardFileConfigSource < CompositeConfigSource    

  def get_os()
    os = JavaSystem.getProperty("os.name").downcase
    out = "win32" if os.starts_with?("windows")
    out = "linux" if os.starts_with?("linux")
    out = "macos" if os.starts_with?("mac")
    out = "solaris" if os.starts_with?("sunos")
    out
  end

  def initialize(filename_base)
    super([ SingleFileConfigSource.new("%s.local" % filename_base),
        SingleFileConfigSource.new("%s.global.%s" % [filename_base, get_os]),
        SingleFileConfigSource.new("%s.global" % filename_base) ])
    @filename_base = filename_base
  end

  def to_s
    "files at '%s.local', '%s.global.%s', '%s.global'" % [ @filename_base, @filename_base, get_os, @filename_base ]
  end
end
