class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - inject copyright information into terracotta source files
  protected
  def postscript(ant, build_environment, product_directory, *args)
    return if @no_demo || @no_extra
    ant.taskdef(:name => "java2html", :classname => "de.java2html.anttasks.Java2HtmlTask")
    #ant.taskdef(:name => "jalopy",    :classname => "de.hunsicker.jalopy.plugin.ant.AntPlugin")
    args.each do |entry|
      # generate an html version of the sample application source files
      srcdir  = FilePath.new(product_directory, *entry.split('/'))
      destdir = FilePath.new(srcdir, 'tmp')
      ant.java2html(
        :srcdir           => srcdir.to_s,
        :destdir          => destdir.to_s,
        :includes         => "**/src/**/*/*.java",
        :style            => "eclipse",
        :tabs             => 3,
        :outputFormat     => 'html',
        :addLineAnchors   => true,
        :showDefaultTitle => true,
        :showLineNumbers  => true,
        :showFileName     => true,
        :overwrite        => true)

      # generate the HTML files for source code browsing
      entries = Dir.entries(destdir.to_s).delete_if { |entry| (/^(\.|\.\.)$/ =~ entry) || File.directory?(entry) }
      entries.each do |entry|
        # config file listing
        configs = {
          #:Server => FilePath.new(srcdir, 'tc-config.xml').to_s,
          :Client => FilePath.new(srcdir, entry, 'tc-config.xml').to_s
        }
        configs.each_pair do |id, config|
          if File.exists?(config)
            File.open(FilePath.new(destdir, entry, "#{id.to_s.downcase}-tc-config.xml.html").to_s, "w") do |file|
              file.write(<<-"EOT")
                <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
                <HTML>
                <BODY STYLE="margin:0px">
                  <TABLE cellspacing="2" cellpadding="0" width="100%" style="font:10pt 'Courier New' monospace">
                    <TR><TD colspan="100%" align="center"><B>#{File.basename(config)} (#{id})</B></TD></TR>
                    <TR><TD colspan="100%" align="center">&nbsp;</TD></TR>
              EOT
              File.open(config, "r") do |data|
                n = 0
                in_comment = false
                data.read.split(/\r\n|\n/).each do |line|
                  line.gsub!(/ /, '&nbsp;')
                  line.gsub!(/</, '&lt;')
                  line.gsub!(/>/, '&gt;')

                  line = "<FONT color=\"darkgray\" weigth=\"bold\">#{line}</FONT>" if /&lt;\?xml/
                  line = "<FONT color=\"green\">#{line}</FONT>" if in_comment || (/&lt;!--|--&gt;/ =~ line)
                  if /&lt;!--/ =~ line
                    in_comment = true
                  elsif in_comment && (/--&gt;/ =~ line)
                    in_comment = false
                  end

                  unless in_comment
                    line.gsub!(/&gt;/, "&gt;<FONT color=\"steelblue\" weight=\"normal\">")
                    line.gsub!(/&lt;/, "</FONT>&lt;")
                  end
                  file.write("<TR><TD><FONT color=\"gray\">&nbsp;" + sprintf("%03d: ", n += 1) + "&nbsp;</FONT></TD><TD width=\"*\">#{line}</TD></TR>\n")
                end
              end
              file.write(<<-"EOT")
                  </TABLE>
                </BODY>
                </HTML>
              EOT
            end
          end
        end

        # package listing
        @frontpage = nil
        File.open(FilePath.new(destdir, entry, "package.html").to_s, "w") do |file|
          file.write(<<-"EOT")
            <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
            <HTML>
            <BODY STYLE="width:100%; height:100%; font: 9pt monospace">
          EOT

          configs.each_pair do |id, config|
            file.write(<<-"EOT") if File.exists?(config)
              <B>#{id.to_s} Configuration</B><BR/>
              <A href="#{id.to_s.downcase}-tc-config.xml.html" target="content">tc-config.xml</A><BR/>
              <P/>
            EOT
          end

          create_index(FilePath.new(destdir, entry).to_s) do |data|
            file.write(data)
            data       = data.match(/href *= *\".+\"/).to_s.match(/\".+\"/).to_s.gsub(/\"/, '')
            if @frontpage.nil?
              @frontpage = data unless data.nil? || data.empty?
            end
          end
          file.write(<<-"EOT")
            </BODY>
            </HTML>
          EOT
        end

        # frameset
        @frontpage = 'client-tc-config.xml.html' if File.exists?(FilePath.new(destdir, entry, "client-tc-config.xml.html").to_s)
        @frontpage = 'server-tc-config.xml.html' if File.exists?(FilePath.new(destdir, entry, "server-tc-config.xml.html").to_s)
        File.open(FilePath.new(destdir, entry, "source.html").to_s, "w") do |file|
          file.write(<<-"EOT")
            <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN" "http://www.w3.org/TR/html4/frameset.dtd">
            <HTML>
            <FRAMESET cols="20%, 80%" frameborder="1">
              <FRAME src="package.html"  frameborder="1" name="index">
              <FRAME src="#{@frontpage}" frameborder="1" name="content">
              <NOFRAMES>
                <P/>
                <CENTER>
                <A href="package.html">
                  Package and source file listing for the <CODE>#{entry}</CODE> sample application.
                </A>
                </CENTER>
              </NOFRAMES>
            </FRAMESET>
            </HTML>
          EOT
        end

        # now move it to the appropriate demo's directory
        ant.move(:todir => FilePath.new(srcdir, entry, 'docs').to_s) do
          ant.fileset(:dir => FilePath.new(destdir, entry).to_s)
        end
      end
      ant.delete(:dir => destdir.to_s)
    end
  end

  private
  def create_index(srcdir, &block)
    Dir.chdir(srcdir) do
      entries = Dir.entries(Dir.getwd).delete_if { |entry| (/^(\.|\.\.)$/ =~ entry) || !File.directory?(entry) }
      entries.each do |directory|
        create_index(directory, &block)
        Dir.chdir(directory) do
          sourcefiles = Dir.entries(Dir.getwd).delete_if { |entry| /\.java\.(xhtml|html)/ !~ entry }
          pkg         = Dir.getwd.gsub(/(\/|\\)/, '.').gsub(/^.+(src|main\.java)\./, '')
          url         = Dir.getwd.gsub(/^.+#{directory}\//, '')
          unless sourcefiles.empty?
            block.call(<<-"EOT")
              <B>Package #{pkg}</B>
              <BR/>
            EOT
          end
          sourcefiles.each do |source|
            srcurl = "#{url}/#{source}".gsub(/.*\/src/, 'src')
            block.call(<<-"EOT")
              <A target="content" href="#{srcurl}">
                #{source.gsub(/\.html/, '')}
              </A>
              <BR/>
            EOT
          end
          block.call("<P/>")
        end
      end
    end
  end
end
