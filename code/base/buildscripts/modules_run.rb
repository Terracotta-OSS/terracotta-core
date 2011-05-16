#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Adds methods to the BuildSubtree class that let it run arbitrary Java classes against itself.

class BuildSubtree
  # Runs a Java class against this subtree. classname is the fully-qualified clas name,
  # directory is the directory to run in; jvm is the JVM object representing the JVM to use.
  # If jvm_args is non-nil, it's an array that completely overrides the set of JVM
  # arguments to use. args is the array of arguments that should be passed to the main
  # program. And, finally, system_properties is a hash of system properties to set.
  #
  # If no JVM arguments are specified, then, if it exists, they'll be read from the file
  # <modulename>/<subtree>.jvmargs.
  def run_java(ant, classname, directory, jvm, jvm_args, args, system_properties, build_results, build_environment)
    real_jvmargs = effective_jvmargs(jvm_args, build_environment)

    the_native_library_path = native_library_path(build_results, build_environment, :full)
    the_classpath = classpath(build_results, :full, :runtime)

    ant.java(
      :classname => classname,
      :fork => true,
      :failonerror => true,
      :dir => directory.to_s,
      :jvm => jvm.java.to_s,
      :classpath => the_classpath
    ) {
      ant.sysproperty(:key => 'java.library.path', :value => the_native_library_path.to_s) unless the_native_library_path.to_s.blank?
      ant.sysproperty(:key => 'java.awt.headless', :value => true)

      unless system_properties.nil?
        system_properties.each do |key, value|
          ant.sysproperty(:key => key, :value => value)
        end
      end

      unless real_jvmargs.nil?
        real_jvmargs.each do |jvm_arg|
          ant.jvmarg(:value => jvm_arg)
        end
      end

      unless args.nil?
        args.each do |arg|
          ant.arg(:value => arg)
        end
      end
    }
  end

  private
  # Figures out which JVM arguments we should use. specified_jvmargs is the set of
  # JVM argument specified directly (which will be used, if non-nil); otherwise,
  # reads from the <modulename>/<subtree>.jvmargs file, or falls back to a default
  # set (currently, none).
  def effective_jvmargs(specified_jvmargs, build_environment)
    if specified_jvmargs != nil
      out = specified_jvmargs
    elsif FileTest.file?(jvmargs_file.to_s)
      out = [ ]
      File.open(jvmargs_file.to_s) do |file|
        file.each do |line|
          out << line.strip
        end
      end
    else
      out = build_environment.default_jvmargs
    end

    out
  end

  def jvmargs_file
    FilePath.new(build_module.root, name + ".jvmargs")
  end
end
