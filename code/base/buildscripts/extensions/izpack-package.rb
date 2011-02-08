#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected

  Pack = Struct.new('Pack', :name, :files, :scripts)

  def make_package(srcdir, destdir, filename, install_name, internal_name)
    puts "-"*80
    puts "pwd           : #{Dir.pwd}"
    puts "srcdir        : #{srcdir}"
    puts "destdir       : #{destdir}"
    puts "filename      : #{filename}"
    puts "install_name  : #{install_name}"
    puts "internal_name : #{internal_name}"
    puts @static_resources.izpack_installer_template.canonicalize.to_s
    puts "-"*80

    izpack_dir = FilePath.new(destdir, 'izpack').ensure_directory

    # build the IzPack installer definition file
    template = File.read(@static_resources.izpack_installer_template.canonicalize.to_s)

    is_script = proc { |file| file =~ /\.(sh|bat)$/ }

    packs = Array.new
    Dir.chdir(srcdir.to_s) do
      dirs, files = Dir['*'].partition { |entry| File.directory?(entry) }

      packs << Pack.new('misc', files, files.find_all(&is_script))

      dirs.each do |dir|
        packs << Pack.new(dir, [dir], scriptfiles(dir, &is_script))
      end
    end

#     files    = Dir.entries(srcdir.to_s).delete_if { |entry| entry =~ /^\./ }
#     scripts  = scriptfiles(srcdir.to_s) { |entry| entry =~ /\.(sh|bat)$/ }
#     scripts.collect! { |entry| FilePath.new(entry).relative_path_from(srcdir) }

    license_file = enterprise? ? 'TERRACOTTA-TRIAL-LICENSE.txt' : 'LICENSE.txt'
    installer_images = if enterprise?
      %w[enterprise-1.jpg enterprise-2.jpg enterprise-3.jpg enterprise-4.jpg
         enterprise-5.jpg enterprise-6.jpg enterprise-6.jpg]
    else
      %w[opensource-1.jpg opensource-2.jpg opensource-3.jpg opensource-4.jpg
         opensource-5.jpg opensource-5.jpg opensource-5.jpg]
    end

    template = ERB.new(template, 0, "%<>").result(binding)
    config   = File.join(izpack_dir.to_s, 'installer.xml')
    File.open(config, 'w') { |out| out << template }

    # build the IzPack shortcuts definitions files for each platform
    template = File.read(@static_resources.izpack_shortcuts_template.canonicalize.to_s)
    shortcuts_spec = YAML.load(ERB.new(template, 0, "%<>").result(binding))
    write_shortcuts_files(izpack_dir, shortcuts_spec)

    copy_izpack_resources(izpack_dir)

    # install IzPack as an Ant task
    ant.taskdef(:name => 'izpack', :classname => 'com.izforge.izpack.ant.IzPackTask') 

    # build the package
    installer_package_name = FilePath.new(destdir.to_s, "#{install_name}-installer.jar")
    ant.izpack(
      :installerType    => 'standard', 
      :inheritAll       => true, 
      :compressionlevel => 9, 
      :compression      => 'deflate', 
      :basedir          => srcdir.to_s, 
      :input            => config, 
      :output           => installer_package_name)
    
    izpack_dir.delete
    installer_package_name
  end
  
  private
  def scriptfiles(srcdir)
    result   = []
    excludes = []
    Find.find(srcdir) do |path|
      if FileTest.directory?(path)
        next unless excludes.include?(File.basename(path))
        Find.prune
      else
        next if block_given? && !yield(path) 
        result << path
      end
    end
    result.reverse
  end

  def write_shortcuts_files(destdir, spec)
    for platform in %w(windows unix)
      file = File.join(destdir.to_s, "#{platform}_shortcuts.xml")
      File.open(file, 'w') do |out|
        out.puts('<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>')
        out.puts
        out.puts('<shortcuts>')

        if spec.has_key?('programGroup')
          pg = spec['programGroup']
          default_name = pg['defaultName']
          location = pg['location']
          out.puts("  <programGroup defaultName=\"#{default_name}\" location=\"#{location}\"/>")
        end

        for shortcut in spec['shortcuts']
          shortcut = shortcut.dup
          windows = shortcut.delete('windows')
          unix = shortcut.delete('unix')

          out.puts("  <shortcut")
          shortcut.merge(platform == 'windows' ? windows : unix).each do |key, value|
            out.puts("      #{key}=\"#{value}\"")
          end
          out.puts("  />")
        end

        out.puts('</shortcuts>')
      end
    end
  end

  def copy_izpack_resources(destdir)
    ant.copy(:todir => destdir.to_s) do
      ant.fileset(:dir => @static_resources.izpack_resources_directory.to_s)
    end
  end
end
