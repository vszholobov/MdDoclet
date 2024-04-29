package com.vszholobov;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;
import org.apache.commons.lang3.StringEscapeUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TypeDocumentationTask implements Callable<String> {
    private static final String L2_HEADER = "## ";
    private static final String L3_HEADER = "### ";
    private final String buildDirPrefix;
    private final DocTrees docTrees;
    private final TypeElement typeElement;

    public TypeDocumentationTask(String buildDirPrefix, DocTrees docTrees, TypeElement typeElement) {
        this.buildDirPrefix = buildDirPrefix;
        this.docTrees = docTrees;
        this.typeElement = typeElement;
    }

    @Override
    public String call() throws Exception {
        StringBuilder classDescription = new StringBuilder();
        // Class / Interface description
        classDescription.append(getElementStringRepresentation(docTrees, typeElement, L2_HEADER));

        // Methods descriptions
        typeElement
                .getEnclosedElements()
                .stream()
                .map(e -> getElementStringRepresentation(docTrees, e, L3_HEADER))
                .forEach(classDescription::append);

        String fileName = buildDirPrefix + typeElement.getSimpleName() + ".md";
        PrintWriter writer = new PrintWriter(fileName, UTF_8);
        writer.println(classDescription);
        writer.close();
        return typeElement.getKind() + ":" + typeElement + "\n" + "Location: " + fileName;
    }

    private String getElementStringRepresentation(DocTrees trees, Element element, String header) {
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
}
