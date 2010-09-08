#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# This file contains implementations of Platform objects, which offer services
# for executing commands on the system and getting the value of environment
# variables. This encapsulates some of the differences between Ruby and JRuby.
#
# Note that Platform objects are not completely interchangeable; for example,
# the JRuby object offers methods that let you get the next Ant output-
# property name, which doesn't even make any sense under Ruby.
#
# To get a Platform object, you should simply call
# CrossPlatform.create_implementation. If you're on JRuby, you'll need to
# pass in a hash with an AntBuilder object under the :ant key; if you're
# under Ruby, you can pass in any hash you want, or none at all.

# This module provides functionality for figuring out whether we're running
# under JRuby or not (i.e., normal Ruby), and for figuring out what class we
# should instantiate to get the correct kind of Platform object for our platform.
module CrossPlatform
  # Are we running under JRuby, or not?
  def self.is_jruby?
    if $is_jruby.nil?
      begin                
        $is_jruby = true
      rescue LoadError
        $is_jruby = false
      end
    end

    $is_jruby
  end

  # Sets the class that we should instantiate to get a valid Platform object
  # for our platform.
  def self.set_implementation_class(the_class)
    $platform_implementation_class = the_class
  end

  # Creates an instance of the right Platform class for our platform.
  def self.create_implementation(data)
    $platform_implementation_class.new(data)
  end
end

# We can actually only evaluate a bunch of this code -- like the 'include_class'
# call -- under certain platforms, so we have to surround the entire class
# definition with this conditional. (God, I love Ruby. Doing this in Java is a
# big pain in the ass, like if you need some classes compiled with 1.4 and some
# with 1.5 -- you have to run the compiler twice, use reflection, etc., etc., etc.)
if CrossPlatform.is_jruby?

  # The JRuby implementation of Platform.
  class JRubyPlatform
    attr_reader :ant

    #include_class('java.lang.System') { |p, name| "Java" + name }

    # Create a new instance.  You must pass in a hash that contains a valid
    # AntBuilder instance under the :ant key. (However, generally speaking, you
    # should use CrossPlatform.create_implementation anyway, not create one of
    # these directly.)
    def initialize(data)
      raise "You must set 'ant' to a valid Ant instance" unless data[:ant]
      @ant = data[:ant]

      # Make ENV use Ant to get environment variables.
      unless $ant
        $ant = @ant
        def ENV.[](key)
          $ant.instance_eval("@env_#{key}")
        end
      end
    end

    $ant_execute_count = 0
    $next_ant_property_counts = { }

    # What are the next property names we should next use when capturing Ant output? Returns
    # an array containing three values: the name of the property we should use for the
    # standard output, the name of the property we should use for the standard error, and
    # the name of the property we should use for the return code.
    #
    # Basically, the only way to run commands on JRuby and capture their output is to use
    # AntBuilder.exec() and redirect the output and error streams to a pair of Ant
    # properties. Since properties stay at their given values once set, we have to have
    # a way of dynamically generating new property names each time we execute something.
    # This is it.
    def next_output_properties
      this_execute_count = $ant_execute_count
      $ant_execute_count += 1
      [ 'executer_output_%d' % this_execute_count, 'executer_error_%d' % this_execute_count, 'executer_result_code_%d' % this_execute_count ]
    end

    # Given a base, returns a new, unique property name that starts with that base on every
    # call. Useful when you need to use some Ant task that sets a property repeatedly, since
    # otherwise you're stuck -- generally speaking, Ant won't overwrite properties that are
    # already set, so you have to have _some_ way of dynamically generating these.
    def next_ant_property_name(name_base)
      $next_ant_property_counts[name_base] ||= 0
      val = $next_ant_property_counts[name_base] += 1
      "%s_%d" % [ name_base, val ]
    end

    # Executes the given executable_path with the given arguments, running it in the given
    # directory. Raises an exception if the command fails (that is, unless the result code
    # is 0). Returns the standard output of the command as a string, unless it's blank, in
    # which case it returns the standard error of the command.
    def exec_at(exec_dir, executable_path, *arguments)
      outputproperty, errorproperty, resultproperty = next_output_properties

      actual_arguments = normalize_ant_exec_args(*arguments)
      line = actual_arguments.join(" ")

      # Use Ant to execute this.
      ant.exec(:executable => executable_path.to_s, :dir => exec_dir, :errorproperty => errorproperty,
        :outputproperty => outputproperty, :failonerror => false, :resultproperty => resultproperty) {
        ant.arg(:line => line) unless line.blank?
      }

      output     = ant.get_ant_property(outputproperty)
      error      = ant.get_ant_property(errorproperty)
      resultcode = ant.get_ant_property(resultproperty)

      raise SystemCallError, "Execution failed (%s): '%s %s'\n\nstdout:\n%s\n\nstderr:\n%s" % [ resultcode, executable_path, line, output, error ] unless resultcode == "0"

      unless output.blank?
        output
      else
        error
      end
    end

    # Executes the given command with the given arguments, running it in the current working
    # directory. Raises an exception if the command fails (that is, unless the result code
    # is 0). Returns the standard output of the command as a string, unless it's blank, in
    # which case it returns the standard error of the command.
    def exec(command, *args)
      exec_at(JavaSystem.getProperty("user.dir"), command, *args)
    end

    # Returns the value of the environment variable with the given name.
    def get_env(name)
      @ant.instance_eval("@env_%s" % name)
    end

    private
    # Quotes a list of arguments properly, so that we can put them on a command line and have
    # the shell parse them properly. This probably isn't perfect, but it's also probably close
    # enough.
    def normalize_ant_exec_args(*arguments)
      actual_arguments = [ ]
      arguments.each do |arg|
        actual_arg = arg.dup
        unless actual_arg =~ /^\s*\".*\"\s*$/
          actual_arg.gsub!(/([^\\])"/, '\1\\\"')
          actual_arg = "\"%s\"" % actual_arg
        end
        actual_arguments << actual_arg
      end
      actual_arguments
    end
  end

  CrossPlatform.set_implementation_class(JRubyPlatform)
else
  # The Platform implementation for standard Ruby (i.e., not JRuby).
  class StandardRubyPlatform
    # Creates a new instance. But don't call this -- use CrossPlatform.create_implementation
    # instead.
    def initialize(data)
      # Nothing to do here
    end

    if ENV['OS'] =~ /windows/i
      # Runs the given command in a subshell, capturing its output. This is the
      # variant for Windows, which we have to do differently than on Unix --
      # basically because Windows really sucks.
      #
      # This returns the standard output and standard error, all interleaved.
      def exec(command, *args)
        cmd = "%s %s 2>&1" % [ command.to_s, (args || [ ]).join(" ") ]
        out = `#{cmd}`
        out
      end
    else
      # Runs the given command in a subshell, capturing its output. This is the
      # variant for Unix.
      #
      # This returns the standard output and standard error, all interleaved.
      def exec(command, *args)
        cmd = command.to_s.to_shell_escaped_s
        unless args.nil?
          args.each do |arg|
            cmd += " %s" % arg.to_shell_escaped_s
          end
        end

        cmd = "/bin/sh -c '%s' 2>&1" % cmd.to_shell_escaped_s
        out = `#{cmd}`
        out
      end
    end

    # Runs the given command exactly like #exec, except that it's run with the specified
    # current working directory.
    def exec_at(dir, command, *args)
      Dir.chdir(dir) do
        exec(command, *args)
      end
    end

    # Returns the value of the given environment variable.
    def get_env(name)
      ENV[name]
    end
  end

  CrossPlatform.set_implementation_class(StandardRubyPlatform)
end
