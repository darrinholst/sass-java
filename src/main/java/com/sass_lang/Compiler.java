package com.sass_lang;

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
        script.println("  Compass.compiler.run                                    ");
        script.println("end                                                       ");
        script.flush();

        return raw.toString();
    }

    private String buildInitializationScript() {
        StringWriter raw = new StringWriter();
        PrintWriter script = new PrintWriter(raw);

        script.println("require 'rubygems'                                                         ");
        script.println("require 'compass'                                                          ");
        script.println("frameworks = Dir.new(Compass::Frameworks::DEFAULT_FRAMEWORKS_PATH).path    ");
        script.println("Compass::Frameworks.register_directory(File.join(frameworks, 'compass'))   ");
        script.println("Compass::Frameworks.register_directory(File.join(frameworks, 'blueprint')) ");
        script.println("Compass.add_project_configuration '" + getConfigLocation() + "'            ");
        script.println("Compass.configure_sass_plugin!                                             ");
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
