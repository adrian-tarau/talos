package net.microfalx.talos.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Properties;

import static net.microfalx.talos.core.MavenUtils.CI_BUILD_NUMBER_PROP;
import static org.apache.maven.plugins.annotations.LifecyclePhase.INITIALIZE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

@Mojo(name = "build-number", defaultPhase = INITIALIZE, requiresDependencyResolution = COMPILE_PLUS_RUNTIME,
        threadSafe = true)
public class BuildNumberMojo extends AbstractMojo {

    @Parameter(property = "talos.build.number")
    private Integer buildNumber;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String sessionBuildNumber = getBuildNumber();
        getLog().info("Build number: " + sessionBuildNumber);
        injectBuildNumber(getTopProject().getProperties(), sessionBuildNumber);
        injectBuildNumber(getProject().getProperties(), sessionBuildNumber);
        getProjects().forEach(project -> injectBuildNumber(project.getProperties(), sessionBuildNumber));
        injectBuildNumber(session.getUserProperties(), sessionBuildNumber);
    }

    private void injectBuildNumber(Properties properties, String sessionBuildNumber) {
        properties.put(CI_BUILD_NUMBER_PROP, sessionBuildNumber);
        System.setProperty(CI_BUILD_NUMBER_PROP, sessionBuildNumber);
    }


}
