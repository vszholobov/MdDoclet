package com.vszholobov;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.lang.model.SourceVersion;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

public class MdDoclet implements Doclet {
    private static final String DOCLET_NAME = "MDDoclet";
    private String buildDirPrefix = "javadoc/";

    private Reporter reporter;
    private PrintWriter stdout;

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
        this.reporter.print(Diagnostic.Kind.NOTE, "Doclet using locale: " + locale);
        this.stdout = reporter.getStandardWriter();
    }

    @Override
    public String getName() {
        return DOCLET_NAME;
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return Set.of(
                new Option("-d", true, "Destination directory for output files, default javadoc/", "<string>") {
                    @Override
                    public boolean process(String option, List<String> arguments) {
                        buildDirPrefix = arguments.get(0);
                        if (!buildDirPrefix.endsWith("/")) {
                            buildDirPrefix += "/";
                        }
                        return true;
                    }
                });
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean run(DocletEnvironment docEnv) {
        reporter.print(Diagnostic.Kind.NOTE, "Start " + DOCLET_NAME);

        DocTrees docTrees = docEnv.getDocTrees();

        try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
            ElementFilter
                    .typesIn(docEnv.getIncludedElements())
                    .stream()
                    .map(typeElement -> executorService.submit(new TypeDocumentationTask(buildDirPrefix, docTrees,
                            typeElement)))
                    .forEach(docsTask -> {
                        try {
                            stdout.println(docsTask.get());
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return true;
    }

    abstract static class Option implements Doclet.Option {
        private final String name;
        private final boolean hasArg;
        private final String description;
        private final String parameters;

        Option(String name, boolean hasArg, String description, String parameters) {
            this.name = name;
            this.hasArg = hasArg;
            this.description = description;
            this.parameters = parameters;
        }

        @Override
        public int getArgumentCount() {
            return hasArg ? 1 : 0;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Kind getKind() {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return List.of(name);
        }

        @Override
        public String getParameters() {
            return hasArg ? parameters : "";
        }
    }
}