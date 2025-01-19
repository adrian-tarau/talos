# Maven

Utilities, plugins and extensions for Java projects using Maven.

## Plugins

### Containers

While there are several alternatives available for Java projects, one of the most well-known options is the one developed by Google. However, I needed a simpler setup.

The plugin still utilizes Docker for building, but this is not an issue for most users. It eliminates the need for custom Docker build commands and includes a library that manages the application startup (setting up the classpath and other JVM arguments).

The only required parameter to package a Java application into a container image is as follows:

* `image`: The name of the image (the tag is generated from the application version)
* `repository`: the name of the repository (if hostname is not provided, it is presumed `docker.io`); a <server></server> with an id equal to the contained of the repository tag.
* `mainClass`: The application class (which runs the application)

```xml
<plugin>
    <groupId>net.microfalx.maven</groupId>
    <artifactId>maven-plugin</artifactId>
    <version>0.8.1</version>
    <executions>
        <execution>
            <id>container</id>
            <goals>
                <goal>package</goal>
            </goals>
            <configuration>
                <image></image>
                <repository></repository>
                <mainClass></mainClass>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Extension


