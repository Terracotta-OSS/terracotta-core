#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Adds methods to BuildSubtree that let it return all kinds of various CLASSPATHs,
# native library paths, and so on, for use both in runtime and compile-time situations.
#
# There's a lot of commonality here among the library-finding code, native-library-finding
# code, and the variant-library-finding code. We should probably factor this out into
# common routines.
#
# Here's what currently goes in a module's CLASSPATH:
#
# * The subtree's own classes (build/<modulename>/<subtreename>.classes)
# * Any overall libraries for this subtree (<modulename>/lib.<subtreename>) -- <subtreename> is omitted if it's 'src'
# * Any runtime libraries for this subtree (<modulename>/lib.<subtreename>.runtime) -- <subtreename> is omitted if it's 'src' -- but *only* if the CLASSPATH is being computed for runtime (as opposed to compile-time)
# * Any compile-time libraries for this subtree (<modulename>/lib.<subtreename>.compile) -- <subtreename> is omitted if it's 'src' -- but *only* if the CLASSPATH is being computed for compile-time (as opposed to runtime)
#
# and then...
#
# * All four of the above, for any subtrees in this same module that this subtree is dependent upon
#
# and then...
#
# * All four of the above, for all other build modules this module is dependent upon, for the subtree in those build modules that this subtree's dependencies are like. (For example, the 'tests.unit' subtree is declared as having dependencies like 'tests.base', so a 'tests.unit' subtree will include all four of the above items for the 'tests.base' and 'src' subtrees in every module this module is dependent upon.)
#
# Variant libraries are *not* included in these paths; you must ask for the variant
# library paths explicitly. They're always runtime-only (since we always compile code
# exactly the same way, and you can put a copy of a variant library into
# lib.<subtree>.compile/ if you need to compile against it), and must be explicitly
# requested and used outside of these methods.
#
# Native library paths are just as above, except that there's no such thing as a
# compile-time-only native library (since there's no such thing in Java as
# 'compiling against' a native library).

class BuildSubtree

  # Returns the CLASSPATH that this subtree should be compiled or run with. type must
  # be either :runtime or :compile; libraries in <module>/lib.<subtree>.runtime will only
  # be added if type is :runtime, while libraries in module/lib.<subtree>.compile will only
  # be added if type is :compile. scope must be either :full or :module_only; if :module_only,
  # only elements referring to this build module will be added to the returned path, while
  # if :full, the entire path will be computed and returned.
  def classpath(build_results, scope, type)
    #assert("Type must be :runtime or :compile, you passed #{type}") { [ :runtime, :compile ].include?(type) }
    #assert("Scope must be :full or :module_only, you passed #{scope}") { [ :full, :module_only ].include?(scope) }

    # JRuby has a nasty bug in handling symbols. At some point in time, it has no idea what a symbol is
    # and it treats symbols as strings instead. The workaround is to treat our "scope" argument as string also.

    if scope == :full || scope == 'full'
      full_classpath(build_results, type)
    else
      module_only_classpath(build_results, type)
    end
  end

  # Returns the native library path that this subtree should be run with. (There's no
  # such thing as a 'type' of native library path, because native libraries aren't compiled
  # against, only run with.) The scope argument is the same as for #classpath, above.
  def native_library_path(build_results, build_environment, scope)
    #assert("Scope must be :full or :module_only, you passed #{scope}") { [ :full, :module_only ].include?(scope) }

    if scope == :full || scope == 'full'
      full_native_library_path(build_results, build_environment)
    else
      module_only_native_library_path(build_results, build_environment)
    end
  end

  # Returns the set of additional libraries that should be included when the given variant name
  # is set equal to the given variant value. This will be libraries from all the subtrees and
  # build modules that this subtree depends upon, but not any of the 'normal' (non-variant)
  # libraries. Thus, this path will need to be added to the path from #classpath, above, before
  # it's useful.
  def full_variant_libraries(variant_name, variant_value)
    out = PathSet.new
    out << module_only_variant_libraries(variant_name, variant_value)
    dependent_subtrees do |the_subtree|
      out << the_subtree.module_only_variant_libraries(variant_name, variant_value)
    end
    out
  end

  # Returns the entire set of variants that this subtree, and all the subtrees it depends upon,
  # can be set to. The return value is a hash whose keys are variant names and whose values are
  # arrays of all the values the given variant name can be set to. (This works by looking at
  # the directories in each module and looking for ones named lib.variants.<name>.<value>; this
  # lets us create variants just by checking libraries into the right location, rather than
  # having to declare them somewhere or something like that.)
  def full_all_variants
    out = { }
    add_module_only_all_variants(out)
    dependent_subtrees do |the_subtree|
      the_subtree.add_module_only_all_variants(out)
    end
    out
  end

  # Returns the libraries for just this subtree (as a PathSet); type is either :runtime or :compile.
  def subtree_only_libraries(type)
    out = dependencies_libraries(type)
    roots = subtree_only_library_roots(type)
    roots.each { |root| out << JarDirectory.new(root).to_classpath }
    out
  end

  def dependencies_libraries(type)
    out = PathSet.new
    ivy_dependencies(type).each do |jar|
      out << FilePath.new(@static_resources.lib_dependencies_directory, jar)
    end
    out
  end

  def aspect_libraries(type)
    aspect_path = PathSet.new
    aspect_path << JarDirectory.new(FilePath.new(build_module.root, "lib.aspects")).to_classpath
    aspect_path
  end

  # Returns an array containing directories that are the roots of libraries for this subtree for
  # the given type (either :runtime or :compile). This is used to figure out which libraries to
  # copy to the distribution. (Variant libraries are never added to the distribution.) Note that
  # this may well return an empty array.
  def subtree_only_library_roots(type)
    out = [ ]
    add_if_directory(out, FilePath.new(build_module.root, "lib" + lib_suffix + "." + type.to_s))
    add_if_directory(out, FilePath.new(build_module.root, "lib" + lib_suffix))
    out
  end

  # Returns an array containing directories that contain native libraries for this subtree. This
  # may well return an empty array, if there are no native libraries for this subtree.
  def subtree_only_native_library_roots(build_environment)
    out = [ ]
    add_if_directory(out, FilePath.new(build_module.root, "lib" + lib_suffix + ".native", native_library_subdir(build_environment)))
    out
  end

  protected
  # Returns a PathSet that contains the path to this subtree's own classes (only). This will either
  # contain one item, or no items (if this subtree has no source).
  def own_classes_only_classpath(build_results)
    out = PathSet.new
    out << build_results.classes_directory(self) if @source_exists
    out
  end

  # Returns a PathSet that contains the path to this subtree's native libraries (only). This may
  # well be an empty PathSet, if this subtree has no native libraries.
  def subtree_only_native_library_path(build_results, build_environment)
    out = PathSet.new
    subtree_only_native_library_roots(build_environment).each { |root| out << root }
    out
  end

  # Returns a PathSet that contains the CLASSPATH for this subtree only. This contains only this
  # subtree's own classes and its own libraries.
  def subtree_only_classpath(build_results, type)
    out = PathSet.new
    out << own_classes_only_classpath(build_results)
    out << subtree_only_libraries(type)
    out
  end

  # Returns a PathSet that's the set of native libraries in this BuildModule (only) that this subtree
  # should use. (So this will contain native libraries from other subtrees in this BuildModule, if
  # this subtree is dependent upon them, but not from any other modules.)
  def module_only_native_library_path(build_results, build_environment)
    out = PathSet.new
    @internal_dependencies.each do |internal_dependency|
      out << build_module.subtree(internal_dependency).subtree_only_native_library_path(build_results, build_environment)
    end

    out << subtree_only_native_library_path(build_results, build_environment)
    out
  end

  # The CLASSPATH for this subtree, but only components that are in the same BuildModule. So this will
  # contain the subtree's own classes and libraries, and also those for any subtrees in the same module
  # that this subtree is dependent upon, but not any from any other modules.
  def module_only_classpath(build_results, type)
    out = PathSet.new

    @internal_dependencies.each do |internal_dependency|
      out << build_module.subtree(internal_dependency).subtree_only_classpath(build_results, type)
    end

    out << subtree_only_classpath(build_results, type)
    prepend_variant_to_classpath(out)
    out
  end

  # Adds all variants found in this subtree to the given hash, whose keys are
  # the names of variants, and whose values are arrays that are the set of values
  # the given named variant can take on.
  def add_subtree_only_all_variants(out={})
    Dir.new(build_module.root.to_s).each do |entry|
      if entry =~ /lib#{lib_suffix}\.variants\.([^\.]+)\.([^\.]+)/
        out[$1] ||= []
        out[$1] = out[$1] | [ $2 ]
      end
    end

    out
  end

  # Adds all variants found in this subtree and all subtrees it's dependent upon,
  # but only in this BuildModule, to the given hash.
  def add_module_only_all_variants(out={})
    add_subtree_only_all_variants(out)
    @internal_dependencies.each do |internal_dependency|
      build_module.subtree(internal_dependency).add_subtree_only_all_variants(out)
    end
    out
  end

  # Returns a PathSet containing all libraries in this subtree (only) that should
  # be included if the given variant_name is set equal to the given variant_value.
  def subtree_only_variant_libraries(variant_name, variant_value)
    out = PathSet.new
    dir = FilePath.new(build_module.root, "lib" + lib_suffix + ".variants." + variant_name + "." + variant_value)
    out << JarDirectory.new(dir).to_classpath if dir.directory?
    out
  end

  # Returns a PathSet containing all libraries in this module (this subtree, plus
  # any subtrees it's dependent upon) that should be included if the given
  # variant_name is set equal to the given variant_value.
  def module_only_variant_libraries(variant_name, variant_value)
    out = PathSet.new
    out << subtree_only_variant_libraries(variant_name, variant_value)
    @internal_dependencies.each do |internal_dependency|
      out << build_module.subtree(internal_dependency).subtree_only_variant_libraries(variant_name, variant_value)
    end
    out
  end

  private
  # Adds a FilePath representing the given path to the given array, if (and only if) that
  # path actually exists and is a directory.
  def add_if_directory(array, path)
    array << FilePath.new(path.to_s) if FileTest.directory?(path.to_s)
  end

  # The suffix we should add to 'lib' to find the libraries for this subtree. This is
  # the name of the tree, prefixed with a dot (e.g., ".tests.base"), unless this subtree
  # is 'src', in which case it's the empty string.
  def lib_suffix
    @name == 'src' ? "" : ("." + @name)
  end

  # Calls the given block with each of the subtrees in all other modules that this
  # subtree's dependencies are like.
  def dependent_subtrees(&the_proc)
    build_module.dependent_modules.each do |dependent_module|
      the_proc.call(dependent_module.subtree(@external_dependencies_like))
    end
  end

  # Finds the full CLASSPATH of the given type (either :compile or :runtime) for this
  # subtree -- that is, includes classes for this subtree and all subtrees it's dependent
  # on in this module, plus for appropriate subtrees in the modules this module is
  # dependent upon.
  def full_classpath(build_results, type)
    out = PathSet.new
    out << module_only_classpath(build_results, type)
    dependent_subtrees do |dependent_subtree|
      out << dependent_subtree.module_only_classpath(build_results, type)
    end
        
    prepend_variant_to_classpath(out)
    out
  end
    
  def prepend_variant_to_classpath(classpath)
    config_source = Registry[:config_source]
    config_source.keys.each do |key|
      if key =~ /variants\.([^\.]+)\.([^\.]+)$/i
        variant_name = $2
        variant_value = config_source[key]
        #puts "name #{variant_name}, value #{variant_value}"
        variant_lib = full_variant_libraries(variant_name, variant_value)
        classpath.prepend(variant_lib)
      end
    end
    classpath
  end

  # Finds the full native library path for this subtree (that is, including the other
  # subtrees this subtree is dependent upon, whether in this build module or not).
  def full_native_library_path(build_results, build_environment)
    out = PathSet.new
    dependent_subtrees do |dependent_subtree|
      out << dependent_subtree.module_only_native_library_path(build_results, build_environment)
    end
    out << module_only_native_library_path(build_results, build_environment)
    out
  end

  # Figures out the subdirectory of the native library directory that we should use.
  # This implies that native libraries must be organized by operating-system test
  def native_library_subdir(build_environment)        
    build_environment.os_type_extra()
  end


  module DOM4J
    # Include some DOM4J classes in this module.
    include_class("org.dom4j.io.SAXReader")
    %w(Document DocumentException Element Node).each do |dom4j_class|
      include_class("org.dom4j.#{dom4j_class}")
    end
  end

  include_class("java.io.FileReader")

  # Returns an Array containing the names of all libraries defined in the Ivy
  # dependency file(s) for this subtree.  The Array elements are simple file
  # names and do not include any paths.
  def ivy_dependencies(type)
    result = Array.new

    dependencies_file = ivy_file_name.canonicalize.to_s
    if File.exists?(dependencies_file)
      xml = DOM4J::SAXReader.new.read(FileReader.new(dependencies_file))
      xml.selectNodes("/ivy-module/dependencies/dependency").each do |node|
        name = node.attribute('name').value
        rev = node.attribute('rev').value
        scope = node.attribute('conf')
        if !scope || scope.value.starts_with?(type.to_s)
          result << "#{name}-#{rev}.jar"
        end
      end
    end

    result
  end

  # The name of the Ivy dependency file for this subtree.
  def ivy_file_name
    FilePath.new(@build_module.root.to_s, "ivy#{lib_suffix.gsub(/\./, '-')}.xml")
  end
end
