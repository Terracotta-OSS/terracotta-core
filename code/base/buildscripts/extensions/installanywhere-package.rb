#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  require 'tmpdir'
  IA_LOCATION = "C:\\Program Files\\Macrovision\\InstallAnywhere 8.0 Enterprise"
  
  protected
  def make_package(srcdir, destdir, filename, install_name, internal_name)
    installer_directory = FilePath.new('..', '..', 'kits', 'source', 'installer')
    tmpdir              = FilePath.new(Dir.tmpdir).ensure_directory 
    
    ant.copy(:todir => tmpdir.to_s) do
      ant.fileset(:dir => installer_directory.to_s, :includes => "#{internal_name.to_s}/**")
    end

    product_directory = FilePath.new(tmpdir, internal_name.to_s)
    ant.copy(:todir => product_directory.to_s) do
      ant.fileset(:dir => installer_directory.to_s, :includes => "common/**")
    end

    ia_project_directory  = FilePath.new(tmpdir, internal_name)
    ia_output_directory   = FilePath.new(ia_project_directory, 'install_build_output')
    ia_contents_directory = FilePath.new(ia_output_directory, 'Application Files')

    project_file = FilePath.new(ia_project_directory, 'install.iap_xml')

    # HACK: This is a workaround, we have to tar and untar the source directory into IA's content folder
    # instead of a direct file copy because, ant.copy fails for some instances when running this script
    # in Windows     
    #
    # We also remove the JRE that we normally bundle with the kit, because IA script will handle the JRE
    # bundling for us.
    srcfile = FilePath.new(destdir, "#{filename}.tar").to_s
    ant.delete(:includeemptydirs => true) do
      ant.fileset(:dir => srcdir.to_s, :includes => "**/jre/**")
    end
    ant.tar(:destfile => srcfile, :longfile => 'gnu') do
      ant.tarfileset(:dir => srcdir.to_s, :prefix => install_name, :excludes => "**/*.sh **/*.bat **/*.exe **/bin/** **/libexec/**") 
      ant.tarfileset(:dir => srcdir.to_s, :prefix => install_name, :includes => "**/*.sh **/*.bat **/*.exe **/bin/** **/libexec/**", :mode => 755) 
    end
    ant.untar(:src => srcfile, :dest => ia_contents_directory.to_s, :overwrite => true)
    ant.delete(:file => srcfile)

    rt_classes_dir   = FilePath.new(ia_output_directory, 'tmp')
    installer_module = @module_set['installer']
    installer_module.subtree('src').copy_classes(@build_results, rt_classes_dir.to_s, ant)    

    ia_libpath = FilePath.new(ia_output_directory, 'lib').ensure_directory
    jarfile    = FilePath.new(ia_libpath, "tc-installer-custom.jar")
    ant.jar(:destfile => jarfile.to_s, :basedir => rt_classes_dir.to_s)
    rt_classes_dir.delete

    ant.taskdef(:name => "buildinstaller", :classname => "com.zerog.ia.integration.ant.InstallAnywhereAntTask")
    ant.buildinstaller(:IALocation => BuildEnvironment::IA_LOCATION, :IAProjectFile => project_file.to_s, :failonerror => true)

    # TODO: need to make this work for non EXE file outputs from IA
    installer_output_file  = FilePath.new(ia_output_directory, 'Web_Installers', 'InstData', @build_environment.os_type(:nice).capitalize, 'VM', "install-terracotta-#{internal_name}.exe")
    installer_package_name = FilePath.new(File.dirname(srcdir.to_s), "#{filename}.exe")
    ant.move(:tofile => installer_package_name, :file => installer_output_file)
  end
end
