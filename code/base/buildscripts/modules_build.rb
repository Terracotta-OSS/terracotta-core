#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# A BuildModuleSetBuilder can take a root directory and an array of filenames, and,
# for each filename, reads it as a YAML file and adds modules based on that YAML to
# a module set, which it returns. Along the way, it also reads in module groups
# and returns a hash of them.
class BuildModuleSetBuilder
  # Creates a new instance. root_dir is the root directory for all the modules, and
  # filenames is the list of filenames to look for in that directory.
  def initialize(root_dir, *filenames)
    @root_dir = root_dir
    @filenames = filenames
  end

  # Creates a BuildModuleSet and a set of module groups based off the YAML files
  # specified in the constructor, and returns them. Returns a hash with values under
  # :module_set (the BuildModuleSet object) and :module_groups (a hash, whose keys
  # are names of module groups and whose values are arrays of the names of the
  # modules in that group).
  def build_modules
    module_set = BuildModuleSet.new(@root_dir)
    module_groups = { }

    @filenames.each do |filename|
      module_base_dir = FilePath.new(File.dirname(filename.relative_path_from(@root_dir).to_s))
      if FileTest.file?(filename.to_s)
        File.open(filename.to_s) do |file|
          yaml = YAML.load(file)

          modules = yaml['modules']
          modules.each do |a_module|
            options = {}
            if data = a_module.values[0]
              if module_options = data['options']
                module_options.each_pair { |k, v| options[k.to_sym] = v }
                options[:root_dir]     = module_base_dir
                options[:name]         = a_module.keys[0]
                options[:dependencies] = data['dependencies'] || []
              end
            end
            module_set.add(options)
          end

          module_groups_source = yaml['module-groups']
          unless module_groups_source.nil?
            module_groups_source.each do |module_group_name, module_group_contents|
              group_name = module_group_name.to_sym
              arr = [ ]
              module_group_contents.each do |module_name|
                mod = module_set[module_name]
                mod.groups << group_name
                arr << module_set[module_name]
              end
              module_groups[group_name] = arr
            end
          end
        end
      end
    end

    module_groups[:all] = [ ]
    module_set.each do |build_module|
      module_groups[:all] << build_module
    end

    { :module_set => module_set, :module_groups => module_groups }
  end
end

