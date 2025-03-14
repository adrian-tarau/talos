package net.microfalx.talos.extension;

import net.microfalx.lang.StringUtils;
import org.apache.maven.cli.MavenCli;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
public class ExtensionTest {

    private MavenCli cli;
    private File workingDirectory;

    @BeforeEach
    void setup() {
        cli = new MavenCli();
        String workingDirectoryPath = System.getProperty("test.maven.extension.workingDirectory");
        if (StringUtils.isEmpty(workingDirectoryPath)) {
            throw new IllegalArgumentException("Project working directory property (test.maven.extension.workingDirectory) must be set");
        }
        workingDirectory = new File(workingDirectoryPath);
        System.setProperty("maven.multiModuleProjectDirectory", workingDirectory.getAbsolutePath());
    }

    @Test
    void build() {
        cli.doMain(new String[]{"clean", "install"}, workingDirectory.getAbsolutePath(), System.out, System.err);
    }

    @Test
    void dependencyTree() {
        cli.doMain(new String[]{"dependency:tree"}, workingDirectory.getAbsolutePath(), System.out, System.err);
    }

}
