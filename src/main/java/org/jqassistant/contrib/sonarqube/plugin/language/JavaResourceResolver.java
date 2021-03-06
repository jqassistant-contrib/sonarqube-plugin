package org.jqassistant.contrib.sonarqube.plugin.language;

import java.util.Iterator;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.scanner.ScannerSide;

/**
 * Implementation of a {@link ResourceResolver} for java elements.
 */
@ScannerSide
public class JavaResourceResolver implements ResourceResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaResourceResolver.class);

    @Override
    public String getLanguage() {
        return "java";
    }

    @Override
    public InputPath resolve(FileSystem fileSystem, String type, String source, String value) {
        switch (type) {
        case "Type":
        case "Field":
        case "Method":
        case "MethodInvocation":
        case "ReadField":
        case "WriteField":
            String javaFilePath = getJavaSourceFileName(source);
            return findMatchingInputFile(fileSystem, javaFilePath);
        default:
            return null;
        }
    }

    /**
     * This resolver can find only resources in the current project, because only
     * such resources are part of the 'index cache'.
     *
     * @return The matching resource or <code>null</code> if nothing was found and
     *         in case of multiple matches.
     */
    private InputFile findMatchingInputFile(FileSystem fileSystem, String javaFilePath) {
        // in SonarQ Java files have the prefix 'src/main/java' for Maven projects
        // we have to handle such nested project structures without specific
        // knowledge about project structures... so use pattern matcher :-)
        Iterator<InputFile> files = fileSystem.inputFiles(fileSystem.predicates().matchesPathPattern("**/" + javaFilePath)).iterator();
        while (files.hasNext()) {
            InputFile file = files.next();
            if (!files.hasNext()) {
                return file;
            }
            LOGGER.warn("Multiple matches for Java file {}, cannot safely determine source file.", javaFilePath);
            return null;
        }
        return null;
    }

    /**
     * Convert a given entry like
     * <code>com/buschmais/jqassistant/examples/sonar/project/Bar.class</code> into
     * a source file name like
     * <code>com/buschmais/jqassistant/examples/sonar/project/Bar.java</code>.
     */
    private String getJavaSourceFileName(String classFileName) {
        if (classFileName == null || classFileName.isEmpty()) {
            return null;
        }
        String result = classFileName;
        if (result.charAt(0) == '/') {
            result = result.substring(1);
        }
        if (result.toLowerCase(Locale.ENGLISH).endsWith(".class")) {
            result = result.substring(0, result.length() - ".class".length());
        }
        // remove nested class fragments
        int index = result.indexOf('$');
        if (index > -1) {
            result = result.substring(0, index);
        }
        return result.concat(".java");
    }
}
