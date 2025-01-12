package net.microfalx.maven.report;

import net.microfalx.lang.Nameable;
import net.microfalx.lang.StringUtils;
import net.microfalx.maven.model.SessionMetrics;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.cache.StandardCacheManager;
import org.thymeleaf.context.Context;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Renders a Thymeleaf templates.
 */
public class Template implements Nameable {

    private final String name;
    private final Map<String, Object> variables = new HashMap<>();
    private SessionMetrics session;
    private String selector;

    private static volatile TemplateEngine templateEngine;

    public static Template create(String name) {
        return new Template(name);
    }

    private Template(String name) {
        requireNonNull(name);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getSelector() {
        return selector;
    }

    public Template setSelector(String selector) {
        this.selector = selector;
        return this;
    }

    public SessionMetrics getSession() {
        return session;
    }

    public Template setSession(SessionMetrics session) {
        this.session = session;
        return this;
    }

    public Template addVariable(String name, Object value) {
        requireNonNull(name);
        variables.put(name, value);
        return this;
    }

    /**
     * Renders a template.
     *
     * @throws IOException if an I/O error occurs
     */
    public void render(Writer writer) throws IOException {
        requireNonNull(writer);
        initEngine();
        Context context = initContext();
        TemplateSpec template = initTemplate();
        templateEngine.process(template, context, writer);
    }

    private static synchronized void initEngine() {
        if (templateEngine != null) return;
        // init resolver
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver(Template.class.getClassLoader());
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCacheTTLMs(3600000L);
        templateResolver.setCacheable(true);
        // create engine
        templateEngine = new TemplateEngine();
        templateEngine.setDialect(new StandardDialect());
        templateEngine.setLinkBuilder(new StandardLinkBuilder());
        templateEngine.setCacheManager(new StandardCacheManager());
        templateEngine.setTemplateResolver(templateResolver);
    }

    private Context initContext() {
        Context context = new Context();
        if (session != null) {
            context.setVariable("session", session);
            context.setVariable("projects", session.getProjects());
            context.setVariable("artifacts", session.getArtifacts());
            context.setVariable("dependencies", session.getDependencies());
            context.setVariable("plugins", session.getPlugins());
        }
        context.setVariables(variables);
        return context;
    }

    private TemplateSpec initTemplate() {
        if (StringUtils.isEmpty(selector)) {
            return new TemplateSpec(name, TemplateMode.HTML);
        } else {
            return new TemplateSpec(name, Set.of(selector), TemplateMode.HTML, Collections.emptyMap());
        }
    }
}
