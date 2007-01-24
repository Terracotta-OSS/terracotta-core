#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# A set of classes useful for dealing with JVMs. Lets you compare JVM versions,
# keep track of a set of JVMs, and find various commands from a JVM, specify
# necessary versions, and so on.

# Represents the version of a JVM. This is typically something like '1.4.2_07', but
# some JVMs (like IBM's) can be just '1.4.2', too.
class JavaVersion
    attr_reader :major, :minor, :patch, :release

    # Creates a new representation of a version, as specified by a string. This can
    # currently be either something like '1.4.2' or something like '1.4.2_07'.
    def initialize(version_string)
        if version_string =~ /^\s*(\d+)\.(\d+)\.(\d+)(?:_(\d+))?\s*$/
            @major, @minor, @patch = $1.to_i, $2.to_i, $3.to_i
            @release = $4.blank? ? 0 : $4.to_i
        else
            raise "Version '#{version_string}' isn't a valid Java version"
        end
    end

    # Is this version less than another version?
    def <(other)
        [ :major, :minor, :patch, :release ].each do |part|
            this_part = method(part).call
            that_part = other.method(part).call

            return true if this_part < that_part
            return false if this_part > that_part
        end

        return false
    end

    # Is this version greater than another version?
    def >(other)
        [ :major, :minor, :patch, :release ].each do |part|
            this_part = method(part).call
            that_part = other.method(part).call

            return true if this_part > that_part
            return false if this_part < that_part
        end

        return false
    end

    # Is this version exactly equal to another version?
    def ==(other)
        major == other.major && minor == other.minor && patch == other.patch && release == other.release
    end

    # Returns a number less than zero, zero, or a number greater than zero as this
    # version is less than, exactly equal to, or greater than the other supplied version.
    def <=>(other)
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

# A set of JVMs. This lets you bind JVMs to names, alias JVMs (meaning the same JVM
# can show up as more than one key), and fetch JVMs out of it.
class JVMSet
    # Creates a new JVMSet.
    def initialize
        @jvms = { }
    end

    # Returns the JVM that's bound to the given key.
    def [](key)
        raise "No JVM for '%s'." % key unless @jvms.has_key?(key)
        @jvms[key]
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

    # A human-readable string representation of this JVMSet.
    def to_s
        sorted_keys = @jvms.keys.sort
        out = "<JVMSet:\n"
        sorted_keys.each { |key| out += "  '%s': %s\n" % [ key, @jvms[key] ] }
        out += ">"
        out
    end
end

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
    def initialize(platform, java_home, data={ })
        @java_home = FilePath.new(java_home).canonicalize
        @java_path = FilePath.new(@java_home, "bin", "java").executable_extension
        @javac_path = FilePath.new(@java_home, "bin", "javac").executable_extension

        @min_version = data[:minimum_version]
        @max_version = data[:maximum_version]

        @actual_version = nil

        @platform = platform
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
    def self.from_config(platform, config_source, descrip, min_version, max_version, *config_property_names)
        root = nil
        config_property_names.each do |config_property_name|
            unless config_source[config_property_name].nil?
                root = config_source[config_property_name]
                break
            end
        end

        out = nil
        unless root.nil?
            out = JVM.new(platform, root, { :minimum_version => min_version, :maximum_version => max_version })
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
            raise message % 'java'
        end
        unless FileTest.file?(javac)
            raise message % 'javac'
        end
    end

    # Checks that the version of this JVM lies within the limits set by the :min_version and
    # :max_version arguments to the constructor.
    def check_version(descrip)
        unless @min_version.nil? && @max_version.nil?
            this_version = JavaVersion.new(actual_version)

            # Template for error messages if the JVM does not meet the version requirements
            message = "The version of #{descrip}, '#{this_version}', is %s than the %s required of '%s'."

            unless @min_version.nil?
                required_version = JavaVersion.new(@min_version)

                if this_version < required_version
                    raise message % ['less', 'minimum', required_version]
                end
            end

            unless @max_version.nil?
                required_version = JavaVersion.new(@max_version)

                if this_version > required_version
                    raise message % ['greater', 'maximum', required_version]
                end
            end
        end
    end

    # A short description of this JVM -- the type followed by the version. Therefore, something
    # like 'hotspot-1.4.2_07'.
    def short_description
        "%s-%s" % [ actual_type, actual_version ]
    end

    # The type of this JVM -- something like 'hotspot' or 'jrockit'.
    def actual_type
        ensure_have_version
        @actual_type
    end

    # The actual version of this JVM, as a string. something like '1.4.2_07'.
    def actual_version
        ensure_have_version
        @actual_version
    end

    # A human-readable string representation of this JVM.
    def to_s
        "<JVM at '%s', version '%s'>" % [ @java_home, @actual_version ]
    end

    private
    # Makes sure the @actual_version and @actual_type member variables are set.
    def ensure_have_version
        if @actual_version.nil?
	    output, error = @platform.exec(@java_path, "-version")

            @actual_version = nil
            [ output, error ].each do |val|
                @actual_version = val if @actual_version.blank? && (! val.blank?)
                @actual_version = $1 if @actual_version =~ /java\s*version\s*\"([^\"]+)\"/
                @actual_type = "jrockit" if @type.blank? && val =~ /jrockit/i
                @actual_type = "hotspot" if @type.blank? && val =~ /hotspot/i
            end
        end
    end
end
