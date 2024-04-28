package com.vszholobov;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.spi.ToolProvider;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Doclet runner. Can be used in build system to execute javadoc generation
 * <p>
 * Bash command example of running underlying javadoc:
 * javadoc
 * -sourcepath {PATH_TO_PROJECT}/src/main/java/
 * --ignore-source-errors
 * -docletpath jars/doclet.jar:jars/commons-lang3-3.13.0.jar:jars/commons-cli:commons-cli:1.6.0
 * -doclet com.example.TestDoclet
 * -subpackages com.example.test
 */

//mvn compile exec:java -Dexec.mainClass="com.vszholobov.RunDoclet"
public class RunDoclet {

    private static final Option BUILD_OPTION = createOption("o",
            "output",
            "OUTPUT",
            "Build output directory. Will be created if it does not exist",
            true,
            true);
    private static final Option SOURCE_PATH_OPTION = createOption("sp",
            "sourcepath",
            "SOURCEPATH",
            "Path to Java source code",
            true,
            true);
    private static final Option DOCLET_PATH_OPTION = createOption("dp",
            "docletpath",
            "DOCLETPATH",
            "Path to JAR file containing doclet",
            false,
            true);
    private static final Option DOCLET_CLASS_OPTION = createOption("dc",
            "docletclass",
            "DOCLETCLASS",
            "Doclet class reference. JAR specified in DOCLETPATH must contain it",
            true,
            true);
    private static final Option SUB_PACKAGES_OPTION = createOption("sb",
            "subpackages",
            "SUBPACKAGES",
            "Specifies which subpackage must be documented",
            false,
            true);
    private static final DefaultParser COMMAND_LINE_PARSER = new DefaultParser();

    public static void main(String[] args) throws ParseException {
        Options options = initOptions();
        CommandLine commandLine = COMMAND_LINE_PARSER.parse(options, args);

        String buildDir = commandLine.getOptionValue(BUILD_OPTION);
        try {
            Files.createDirectories(Paths.get(buildDir));
        } catch (IOException e) {
            throw new RuntimeException("Error creating build directory", e);
        }

        // https://stackoverflow.com/questions/54040274/what-is-the-alternative-of-com-sun-tools-javadoc-main-execute-to-run-doclet-in-j
        ToolProvider javadoc = ToolProvider.findFirst("javadoc").orElseThrow();
        int result = javadoc.run(System.out, System.err,
                "-d", buildDir,
                "--ignore-source-errors",
                "-sourcepath", commandLine.getOptionValue(SOURCE_PATH_OPTION),
                "-docletpath", commandLine.getOptionValue(DOCLET_PATH_OPTION, ""),
                "-doclet", commandLine.getOptionValue(DOCLET_CLASS_OPTION),
                "-subpackages", commandLine.getOptionValue(SUB_PACKAGES_OPTION, ""));
        System.out.println("Execution result code: " + result);
    }

    private static Options initOptions() {
        return new Options()
                .addOption(BUILD_OPTION)
                .addOption(SOURCE_PATH_OPTION)
                .addOption(DOCLET_PATH_OPTION)
                .addOption(DOCLET_CLASS_OPTION)
                .addOption(SUB_PACKAGES_OPTION);
    }

    private static Option createOption(String shortName,
                                       String longName,
                                       String argName,
                                       String description,
                                       boolean required,
                                       boolean hasArg) {
        return Option.builder(shortName)
                .longOpt(longName)
                .argName(argName)
                .desc(description)
                .hasArg(hasArg)
                .required(required)
                .build();
    }

}
