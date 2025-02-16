package net.microfalx.talos.docker;

import net.microfalx.lang.IdentityAware;
import net.microfalx.lang.StringUtils;
import net.microfalx.lang.UriUtils;

import java.net.URI;
import java.util.StringJoiner;

import static net.microfalx.lang.ArgumentUtils.requireNotEmpty;
import static net.microfalx.lang.StringUtils.EMPTY_STRING;
import static net.microfalx.lang.UriUtils.hasAuthority;

/**
 * Holds information about the registry.
 */
public class Registry extends IdentityAware<String> {

    private static final String DEFAULT_REGISTRY = "docker.io";

    private final String hostname;
    private String userName;
    private String password;
    private String token;

    public static Registry create() {
        return fromHost(DEFAULT_REGISTRY);
    }

    public static Registry fromHost(String hostName) {
        return new Registry(hostName, hostName);
    }

    public static Registry fromRepository(String name) {
        requireNotEmpty(name);
        String finalRepository = isDockerRepository(name) ? DEFAULT_REGISTRY + "/" + name : name;
        URI uri = UriUtils.parseUri((hasAuthority(finalRepository) ? EMPTY_STRING : "https://") + finalRepository);
        String id = uri.getHost() + uri.getPath();
        return new Registry(id, uri.getHost());
    }

    public Registry(String id, String hostname) {
        requireNotEmpty(id);
        requireNotEmpty(hostname);
        setId(id.toLowerCase());
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public Registry withUser(String userName, String password) {
        requireNotEmpty(userName);
        Registry copy = (Registry) copy();
        copy.userName = userName;
        copy.password = password;
        return copy;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Registry.class.getSimpleName() + "[", "]")
                .add("hostname='" + hostname + "'")
                .add("userName='" + userName + "'")
                .add("token='" + token + "'")
                .toString();
    }

    private static boolean isDockerRepository(String name) {
        String[] parts = StringUtils.split(name, "/");
        String host = parts[0];
        return DEFAULT_REGISTRY.equalsIgnoreCase(host) || StringUtils.split(host, ".").length == 1;
    }
}
