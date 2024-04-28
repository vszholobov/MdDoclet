package com.vszholobov;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import org.apache.commons.lang3.StringEscapeUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MdDoclet implements Doclet {
    private static final String L2_HEADER = "## ";
    private static final String L3_HEADER = "### ";
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

        for (TypeElement typeElement : ElementFilter.typesIn(docEnv.getIncludedElements())) {
            StringBuilder classDescription = new StringBuilder();
            // Class / Interface description
            classDescription.append(getElementStringRepresentation(docTrees, typeElement, L2_HEADER));

            // Methods descriptions
            typeElement
                    .getEnclosedElements()
                    .stream()
                    .map(e -> getElementStringRepresentation(docTrees, e, L3_HEADER))
                    .forEach(classDescription::append);

            try {
                String fileName = buildDirPrefix + typeElement.getSimpleName() + ".md";
                stdout.println(typeElement.getKind() + ":" + typeElement + "\n" + "Location: " + fileName);
                PrintWriter writer = new PrintWriter(fileName, UTF_8);
                writer.println(classDescription);
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    public String getElementStringRepresentation(DocTrees trees, Element element, String header) {
        DocCommentTree docCommentTree = trees.getDocCommentTree(element);

        StringBuilder elementDescription = new StringBuilder();
        String elementHeader = header + element.getKind() + " " + element + "\n\n";
        elementDescription.append(elementHeader);

        // If no javadoc present - return signature
        if (docCommentTree == null) {
            return elementDescription.toString();
        }

        docCommentTree
                .getFullBody()
                .stream()
                .map(d -> StringEscapeUtils.unescapeJava(d.toString()))
                .forEach(elementDescription::append);
        elementDescription.append("\n\n");
        docCommentTree
                .getBlockTags()
                .stream()
                .map(d -> StringEscapeUtils.unescapeJava(d.toString()) + "\n\n")
                .forEach(elementDescription::append);
        return elementDescription.toString();
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