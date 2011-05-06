package com.sass_lang;

import org.jruby.embed.ScriptingContainer;

import javax.servlet.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class SassCompilingFilter implements Filter {
    protected static final String DEFAULT_TEMPLATE_LOCATION = "WEB-INF" + File.separator + "sass";
    protected static final String DEFAULT_CSS_LOCATION = "stylesheets";
    protected static final String DEFAULT_CACHE_LOCATION = "WEB-INF" + File.separator + ".sass-cache";

    private String templateLocation;
    private String cssLocation;
    private String cacheLocation;

    public void init(FilterConfig filterConfig) throws ServletException {
        String root = new File(filterConfig.getServletContext().getRealPath("/")).getAbsolutePath();

        templateLocation = fullPath(root, filterConfig.getInitParameter("templateLocation"), DEFAULT_TEMPLATE_LOCATION);
        cssLocation = fullPath(root, filterConfig.getInitParameter("cssLocation"), DEFAULT_CSS_LOCATION);
        cacheLocation = fullPath(root, filterConfig.getInitParameter("cacheLocation"), DEFAULT_CACHE_LOCATION);
    }

    private String fullPath(String root, String directory, String defaultDirectory) {
        if(directory == null) {
            directory = defaultDirectory;
        }
        return root + File.separator + directory;
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
