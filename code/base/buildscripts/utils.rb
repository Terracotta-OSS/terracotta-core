#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#
require 'rexml/document'
require 'rexml/xpath'

unless defined?(JavaSystem)
  include_class('java.lang.System') { |p, name| "Java" + name }
end

unless defined?(JavaFile)
  include_class('java.io.File') { |p, name| "Java" + name }
end

# A basic assertion facility for Ruby. Takes a block, and fails if
# the block returns false.
def assert(message="Assertion Failed")
  raise RuntimeError, message unless yield
end

# Prints a "loud", very visible message to the console.  This is used for
# warning messages that, even while the scroll by quickly on the console,
# should call out for the user's attention.
def loud_message(message)
  banner_char = "*"
  puts(banner_char * 80)
  puts(banner_char)
  puts("#{banner_char} #{message}")
  puts(banner_char)
  puts(banner_char * 80)
end

def time
  start = Time.now
  yield
  Time.now - start
end

def get_version(file, xpath)
  version = nil
  File.open(file) do |f|
    doc = REXML::Document.new(f)
    version = REXML::XPath.first(doc, xpath).text 
  end
  version
end

def compare_maven_version(maven_version, pom, xpath)
  version = get_version(pom, xpath)
  printf("%-50s: %s\n", pom, version)
  if version != maven_version
    raise "version #{version} in #{pom} " +
          "doesn't match maven.version #{maven_version}"
  end
end

class Hash
  def boolean(key, default = false)
    if has_key?(key)
      value = self[key]
      if value.is_a?(String)
        case value.downcase.strip
        when /^true$/: true
        when /^false$/: false
        when "": default
        else
          raise("Cannot convert value for key '#{key}' to boolean: #{value}")
        end
      else
        value ? true : false
      end
    else
      default
    end
  end
end

module CallWithVariableArguments
    # A method to call a procedure that may take variable arguments, but
    # which issues much nicer error messages when something fails than
    # the default messages issued by JRuby.
    def call_proc_variable(proc, args)
        if proc.arity == 0
            proc.call
        elsif proc.arity > 0
            raise RuntimeError, "Insufficient arguments: proc takes %d, but we only have %d." % [ proc.arity, args.length ] if proc.arity > args.length
            proc.call(*(args[0..(proc.arity - 1)]))
        else
            required = proc.arity.abs - 1
            raise RuntimeError, "Insufficient arguments: proc takes %d, but we only have %d." % [ required, args.length ] if required > args.length
            proc.call(*args)
        end
    end
end
