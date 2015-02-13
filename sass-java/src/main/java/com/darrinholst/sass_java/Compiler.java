package com.darrinholst.sass_java;

import org.jruby.embed.ScriptingContainer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Compiler {
    private boolean initialized;
    private File configLocation;

    public void compile() {
        initialize();
        new ScriptingContainer().runScriptlet(buildCompileScript());
    }

    private void initialize() {
        if (!initialized) {
            new ScriptingContainer().runScriptlet(buildInitializationScript());
            initialized = true;
        }
    }

    private String buildCompileScript() {
        StringWriter raw = new StringWriter();
        PrintWriter script = new PrintWriter(raw);

        script.println("Dir.chdir(File.dirname('" + getConfigLocation() + "')) do ");
        script.println("  compiler = Compass.sass_compiler                        ");
        script.println("  compiler.logger = Compass::NullLogger.new               ");
        script.println("  compiler.clean!                                         ");
        script.println("  compiler.compile!                                       ");
        script.println("end                                                       ");
        script.flush();

        return raw.toString();
    }

    private String buildInitializationScript() {
        StringWriter raw = new StringWriter();
        PrintWriter script = new PrintWriter(raw);

        script.println("require 'compass'                                               ");
        script.println("require 'compass/sass_compiler'                                 ");
        script.println("Compass.add_project_configuration '" + getConfigLocation() + "' ");
        script.println("Compass.configure_sass_plugin!                                  ");
        script.flush();

        return raw.toString();
    }

    private String getConfigLocation() {
        return replaceSlashes(configLocation.getAbsolutePath());
    }

    private String replaceSlashes(String path) {
        return path.replaceAll("\\\\", "/");
    }

    public void setConfigLocation(File configLocation) {
        this.configLocation = configLocation;
    }
}
