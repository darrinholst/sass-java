package com.darrinholst.sass;

import org.jruby.embed.ScriptingContainer;

import javax.servlet.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class SassCompiler implements Filter {
    private String templateLocation;
    private String cssLocation;
    private String cacheLocation;

    public void init(FilterConfig filterConfig) throws ServletException {
        String root = new File(filterConfig.getServletContext().getRealPath("/")).getAbsolutePath();
        templateLocation = root + File.separator + "WEB-INF/sass";
        cssLocation = root + File.separator + "stylesheets";
        cacheLocation = root + File.separator + "WEB-INF/.sass-cache";
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        StringWriter raw = new StringWriter();
        PrintWriter script = new PrintWriter(raw);

        script.println("  require 'rubygems'                                  ");
        script.println("  require 'sass/plugin'                               ");
        script.println("  Sass::Plugin.options.merge!(                        ");
        script.println("    :template_location => '" + templateLocation + "', ");
        script.println("    :css_location => '" + cssLocation + "',           ");
        script.println("    :cache_store => nil,                              ");
        script.println("    :cache_location => '" + cacheLocation + "'        ");
        script.println("  )                                                   ");
        script.println("  Sass::Plugin.check_for_updates                      ");
        script.flush();
        
        ScriptingContainer ruby = new ScriptingContainer();
        ruby.runScriptlet(raw.toString());
        
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
    }
}
