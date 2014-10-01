package com.darrinholst.sass_java;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(
	name = "compile",
	defaultPhase = LifecyclePhase.COMPILE,
	requiresDirectInvocation = false
)
public class SassJavaMojo extends AbstractMojo {

	private Compiler compiler = new Compiler();

    @Parameter(
		defaultValue = "${project.basedir}",
		readonly = true
	)
    private File basedir;

    @Parameter( 
		defaultValue = "${project.basedir}/src/main/webapp/WEB-INF/sass/config.rb",
		property = "sass-java.configFile",
		readonly = false
	)
    private File config;

	public void execute() throws MojoExecutionException, MojoFailureException {

		if (config.exists()) {
	        compiler.setConfigLocation( config );
	        compiler.compile();
		} else {
			throw new MojoExecutionException( "Configuration does not exist at " + config.getAbsolutePath() );
		}

	}

}
