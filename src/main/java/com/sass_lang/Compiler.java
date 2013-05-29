package com.sass_lang;

import org.jruby.embed.ScriptingContainer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Compiler {
    private File cacheLocation;
    private File cssLocation;
    private File templateLocation;
    private boolean cache;

    public void compile() {
        new ScriptingContainer().runScriptlet(buildUpdateScript());
    }

    private String buildUpdateScript() {
        StringWriter raw = new StringWriter();
        PrintWriter script = new PrintWriter(raw);

        script.println("  require 'rubygems'                                       ");
        script.println("  require 'sass/plugin'                                    ");
        script.println("  Sass::Plugin.options.merge!(                             ");
        script.println("    :template_location => '" + getTemplateLocation() + "', ");
        script.println("    :css_location => '" + getCssLocation() + "',           ");
        script.println("    :cache => " + wantsCaching() + ",                      ");
        script.println("    :cache_store => nil,                                   ");
        script.println("    :cache_location => '" + getCacheLocation() + "'        ");
        script.println("  )                                                        ");
        script.println("  Sass::Plugin.check_for_updates                           ");
        script.flush();

        return raw.toString();
    }

    protected boolean wantsCaching() {
        return cache;
    }

    private String getCacheLocation() {
        return replaceSlashes(cacheLocation.getAbsolutePath());
    }

    private String getCssLocation() {
        return replaceSlashes(cssLocation.getAbsolutePath());
    }

    private String getTemplateLocation() {
        return replaceSlashes(templateLocation.getAbsolutePath());
    }

    private String replaceSlashes(String path) {
        return path.replaceAll("\\\\", "/");
    }

    public void setCacheLocation(File cacheLocation) {
        this.cacheLocation = cacheLocation;
    }

    public void setCssLocation(File cssLocation) {
        this.cssLocation = cssLocation;
    }

    public void setTemplateLocation(File templateLocation) {
        this.templateLocation = templateLocation;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }
}
