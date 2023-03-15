/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestCreateBlankOperation {
    @Test
    void testInstantiation() {
        var operation = new CreateBlankOperation();
        assertNotNull(operation.workDirectory());
        assertTrue(operation.workDirectory().exists());
        assertTrue(operation.workDirectory().isDirectory());
        assertTrue(operation.workDirectory().canWrite());
        assertFalse(operation.downloadDependencies());
        assertNull(operation.packageName());
        assertNull(operation.projectName());
    }

    @Test
    void testPopulation()
    throws Exception {
        var workDirectory = Files.createTempDirectory("test").toFile();
        try {
            var downloadDependencies = true;
            var packageName = "packageName";
            var projectName = "projectName";

            var operation = new CreateBlankOperation();
            operation
                .workDirectory(workDirectory)
                .downloadDependencies(downloadDependencies)
                .packageName(packageName)
                .projectName(projectName);

            assertEquals(workDirectory, operation.workDirectory());
            assertEquals(downloadDependencies, operation.downloadDependencies());
            assertEquals(packageName, operation.packageName());
            assertEquals(projectName, operation.projectName());
        } finally {
            FileUtils.deleteDirectory(workDirectory);
        }
    }

    @Test
    void testExecute()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var create_operation = new CreateBlankOperation()
                .workDirectory(tmp)
                .packageName("com.example")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation.execute();

            assertEquals("""
                    /myapp
                    /myapp/.gitignore
                    /myapp/.idea
                    /myapp/.idea/app.iml
                    /myapp/.idea/bld.iml
                    /myapp/.idea/libraries
                    /myapp/.idea/libraries/bld.xml
                    /myapp/.idea/libraries/compile.xml
                    /myapp/.idea/libraries/runtime.xml
                    /myapp/.idea/libraries/test.xml
                    /myapp/.idea/misc.xml
                    /myapp/.idea/modules.xml
                    /myapp/.idea/runConfigurations
                    /myapp/.idea/runConfigurations/Run Main.xml
                    /myapp/.idea/runConfigurations/Run Tests.xml
                    /myapp/bld.sh
                    /myapp/lib
                    /myapp/lib/bld
                    /myapp/lib/compile
                    /myapp/lib/compile/rife2-1.5.0-20230313.213352-8.jar
                    /myapp/lib/runtime
                    /myapp/lib/test
                    /myapp/lib/test/apiguardian-api-1.1.2.jar
                    /myapp/lib/test/junit-jupiter-5.9.2.jar
                    /myapp/lib/test/junit-jupiter-api-5.9.2.jar
                    /myapp/lib/test/junit-jupiter-engine-5.9.2.jar
                    /myapp/lib/test/junit-jupiter-params-5.9.2.jar
                    /myapp/lib/test/junit-platform-commons-1.9.2.jar
                    /myapp/lib/test/junit-platform-console-standalone-1.9.2.jar
                    /myapp/lib/test/junit-platform-engine-1.9.2.jar
                    /myapp/lib/test/opentest4j-1.2.0.jar
                    /myapp/src
                    /myapp/src/bld
                    /myapp/src/bld/java
                    /myapp/src/bld/java/com
                    /myapp/src/bld/java/com/example
                    /myapp/src/bld/java/com/example/MyappBuild.java
                    /myapp/src/main
                    /myapp/src/main/java
                    /myapp/src/main/java/com
                    /myapp/src/main/java/com/example
                    /myapp/src/main/java/com/example/Myapp.java
                    /myapp/src/main/resources
                    /myapp/src/main/resources/templates
                    /myapp/src/test
                    /myapp/src/test/java
                    /myapp/src/test/java/com
                    /myapp/src/test/java/com/example
                    /myapp/src/test/java/com/example/MyappTest.java""",
                Files.walk(Path.of(tmp.getAbsolutePath()))
                    .map(path -> path.toAbsolutePath().toString().substring(tmp.getAbsolutePath().length()))
                    .filter(s -> !s.isEmpty())
                    .sorted()
                    .collect(Collectors.joining("\n")));

            var compile_operation = new CompileOperation().fromProject(create_operation.project());
            compile_operation.execute();
            assertTrue(compile_operation.diagnostics().isEmpty());
            assertEquals("""
                    /myapp
                    /myapp/.gitignore
                    /myapp/.idea
                    /myapp/.idea/app.iml
                    /myapp/.idea/bld.iml
                    /myapp/.idea/libraries
                    /myapp/.idea/libraries/bld.xml
                    /myapp/.idea/libraries/compile.xml
                    /myapp/.idea/libraries/runtime.xml
                    /myapp/.idea/libraries/test.xml
                    /myapp/.idea/misc.xml
                    /myapp/.idea/modules.xml
                    /myapp/.idea/runConfigurations
                    /myapp/.idea/runConfigurations/Run Main.xml
                    /myapp/.idea/runConfigurations/Run Tests.xml
                    /myapp/bld.sh
                    /myapp/build
                    /myapp/build/main
                    /myapp/build/main/com
                    /myapp/build/main/com/example
                    /myapp/build/main/com/example/Myapp.class
                    /myapp/build/test
                    /myapp/build/test/com
                    /myapp/build/test/com/example
                    /myapp/build/test/com/example/MyappTest.class
                    /myapp/lib
                    /myapp/lib/bld
                    /myapp/lib/compile
                    /myapp/lib/compile/rife2-1.5.0-20230313.213352-8.jar
                    /myapp/lib/runtime
                    /myapp/lib/test
                    /myapp/lib/test/apiguardian-api-1.1.2.jar
                    /myapp/lib/test/junit-jupiter-5.9.2.jar
                    /myapp/lib/test/junit-jupiter-api-5.9.2.jar
                    /myapp/lib/test/junit-jupiter-engine-5.9.2.jar
                    /myapp/lib/test/junit-jupiter-params-5.9.2.jar
                    /myapp/lib/test/junit-platform-commons-1.9.2.jar
                    /myapp/lib/test/junit-platform-console-standalone-1.9.2.jar
                    /myapp/lib/test/junit-platform-engine-1.9.2.jar
                    /myapp/lib/test/opentest4j-1.2.0.jar
                    /myapp/src
                    /myapp/src/bld
                    /myapp/src/bld/java
                    /myapp/src/bld/java/com
                    /myapp/src/bld/java/com/example
                    /myapp/src/bld/java/com/example/MyappBuild.java
                    /myapp/src/main
                    /myapp/src/main/java
                    /myapp/src/main/java/com
                    /myapp/src/main/java/com/example
                    /myapp/src/main/java/com/example/Myapp.java
                    /myapp/src/main/resources
                    /myapp/src/main/resources/templates
                    /myapp/src/test
                    /myapp/src/test/java
                    /myapp/src/test/java/com
                    /myapp/src/test/java/com/example
                    /myapp/src/test/java/com/example/MyappTest.java""",
                Files.walk(Path.of(tmp.getAbsolutePath()))
                    .map(path -> path.toAbsolutePath().toString().substring(tmp.getAbsolutePath().length()))
                    .filter(s -> !s.isEmpty())
                    .sorted()
                    .collect(Collectors.joining("\n")));

            final var run_operation = new RunOperation().fromProject(create_operation.project());
            final String[] check_result = new String[1];
            run_operation.runOutputConsumer(s -> check_result[0] = s);
            run_operation.execute();
            assertEquals("""
                Hello World!
                """, check_result[0]);
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExecuteNoDownload()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var create_operation = new CreateBlankOperation()
                .workDirectory(tmp)
                .packageName("org.stuff")
                .projectName("yourthing");
            create_operation.execute();

            assertEquals("""
                    /yourthing
                    /yourthing/.gitignore
                    /yourthing/.idea
                    /yourthing/.idea/app.iml
                    /yourthing/.idea/bld.iml
                    /yourthing/.idea/libraries
                    /yourthing/.idea/libraries/bld.xml
                    /yourthing/.idea/libraries/compile.xml
                    /yourthing/.idea/libraries/runtime.xml
                    /yourthing/.idea/libraries/test.xml
                    /yourthing/.idea/misc.xml
                    /yourthing/.idea/modules.xml
                    /yourthing/.idea/runConfigurations
                    /yourthing/.idea/runConfigurations/Run Main.xml
                    /yourthing/.idea/runConfigurations/Run Tests.xml
                    /yourthing/bld.sh
                    /yourthing/lib
                    /yourthing/lib/bld
                    /yourthing/lib/compile
                    /yourthing/lib/runtime
                    /yourthing/lib/test
                    /yourthing/src
                    /yourthing/src/bld
                    /yourthing/src/bld/java
                    /yourthing/src/bld/java/org
                    /yourthing/src/bld/java/org/stuff
                    /yourthing/src/bld/java/org/stuff/YourthingBuild.java
                    /yourthing/src/main
                    /yourthing/src/main/java
                    /yourthing/src/main/java/org
                    /yourthing/src/main/java/org/stuff
                    /yourthing/src/main/java/org/stuff/Yourthing.java
                    /yourthing/src/main/resources
                    /yourthing/src/main/resources/templates
                    /yourthing/src/test
                    /yourthing/src/test/java
                    /yourthing/src/test/java/org
                    /yourthing/src/test/java/org/stuff
                    /yourthing/src/test/java/org/stuff/YourthingTest.java""",
                Files.walk(Path.of(tmp.getAbsolutePath()))
                    .map(path -> path.toAbsolutePath().toString().substring(tmp.getAbsolutePath().length()))
                    .filter(s -> !s.isEmpty())
                    .sorted()
                    .collect(Collectors.joining("\n")));

            var compile_operation = new CompileOperation() {
                public void executeProcessDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
                    // don't output errors
                }
            };
            compile_operation.fromProject(create_operation.project());
            compile_operation.execute();
            var diagnostics = compile_operation.diagnostics();
            assertEquals(4, diagnostics.size());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
