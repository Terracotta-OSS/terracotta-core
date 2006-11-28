class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - inject copyright information into terracotta source files
  protected
  def postscript(ant, build_environment, product_directory, *args)
    Dir.chdir(product_directory.to_s) do
      entries = Dir.entries(Dir.getwd).delete_if { |entry| (/^(\.|\.\.)$/ =~ entry) || !File.directory?(entry) }
      entries.each do |directory|
        postscript(ant, build_environment, directory, args)
        Dir.chdir(directory) do
          native_scripts = build_environment.is_unix_like? ? /\.(sh|java)$/ : /\.(bat|java)$/
          native_scripts = Dir.entries(Dir.getwd).delete_if { |entry| native_scripts !~ entry }
          native_scripts.each do |native_script|
            if @copyright_template.nil?
              template_file = FilePath.new(@static_resources.templates_directory, 'COPYRIGHT.template')
              File.open(template_file.to_s) do |file| 
                @copyright_template = file.read.chomp
              end 
            end
            copyright_template = @copyright_template.gsub(/#/, '#')   if /\.sh$/   =~ native_script
            copyright_template = @copyright_template.gsub(/#/, ' *')  if /\.java$/ =~ native_script
            copyright_template = @copyright_template.gsub(/#/, 'rem') if /\.bat$/  =~ native_script
            
            File.open(native_script, 'r+') do |file|
              content = file.read.gsub(/^(#|rem | )?@COPYRIGHT@/i, copyright_template)
              file.rewind
              file.write(content)
            end
          end
        end
      end
    end
  end
end