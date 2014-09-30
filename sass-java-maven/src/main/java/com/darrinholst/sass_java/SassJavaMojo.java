package com.darrinholst.sass_java;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(
	name = "sayhi"
)
public class SassJavaMojo extends AbstractMojo {

	
	public void execute() throws MojoExecutionException, MojoFailureException {
		// TODO Auto-generated method stub
		getLog().info( "Hello, world." );
	}

}
