package com.sass_lang;

import org.jruby.embed.ScriptingContainer;

import javax.servlet.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class SassCompilingFilter implements Filter {
    private static final int DWELL = 2000;
    protected static final String TEMPLATE_LOCATION_PARAM = "templateLocation";
    protected static final String CSS_LOCATION_PARAM = "cssLocation";
    protected static final String CACHE_LOCATION_PARAM = "cacheLocation";
    protected static final String DEFAULT_TEMPLATE_LOCATION = "WEB-INF" + File.separator + "sass";
    protected static final String DEFAULT_CSS_LOCATION = "stylesheets";
    protected static final String DEFAULT_CACHE_LOCATION = "WEB-INF" + File.separator + ".sass-cache";

    private String updateScript;
    private long lastRun;

    public void init(FilterConfig filterConfig) throws ServletException {
        String root = new File(filterConfig.getServletContext().getRealPath("/")).getAbsolutePath();
        String templateLocation = fullPath(root, filterConfig.getInitParameter(TEMPLATE_LOCATION_PARAM), DEFAULT_TEMPLATE_LOCATION);
        String cssLocation = fullPath(root, filterConfig.getInitParameter(CSS_LOCATION_PARAM), DEFAULT_CSS_LOCATION);
        String cacheLocation = fullPath(root, filterConfig.getInitParameter(CACHE_LOCATION_PARAM), DEFAULT_CACHE_LOCATION);
        
        updateScript = buildUpdateScript(templateLocation, cssLocation, cacheLocation);
    }

    private String fullPath(String root, String directory, String defaultDirectory) {
        return root + File.separator + (directory == null ? defaultDirectory : directory);
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        long now = Clock.now().getTime();
        
        if (now - lastRun >= DWELL) {
            lastRun = now;
            ScriptingContainer ruby = new ScriptingContainer();
            ruby.runScriptlet(updateScript);
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String buildUpdateScript(String templateLocation, String cssLocation, String cacheLocation) {
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

        return raw.toString();
    }

    public void destroy() {
    }
}
