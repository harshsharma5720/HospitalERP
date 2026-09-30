package com.itmonteur.hospitalerp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module boundaries, checked by Spring Modulith (docs/MULTI_MODULE_PLAN.md, step 2.4).
 *
 * Each sub-package com.itmonteur.hospitalerp.&lt;module&gt; is a module. Its top-level package is the
 * module's API; sub-packages such as internal and web are hidden from other modules.
 * The test fails on a dependency cycle between modules, on use of another module's
 * internal classes, and on field injection of another module's beans.
 *
 * It also generates the module documentation (step 2.5): PlantUML component diagrams and one
 * "module canvas" per module. By default into target/spring-modulith-docs; to refresh the
 * committed copy in docs/modules after a module change, run:
 *   ./mvnw test -Dtest=ModularityTest -Dsurefire.failIfNoSpecifiedTests=false -DupdateModuleDocs=true
 */
class ModularityTest {

    private static final Path COMMITTED_DOCS = Path.of("..", "..", "docs", "modules"); // tests run in hospitalERP/app
    private static final Path BUILD_DOCS = Path.of("target", "spring-modulith-docs");

    private final ApplicationModules modules = ApplicationModules.of(HospitalErpApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        modules.verify();
    }

    @Test
    void writeModuleDocumentation() {
        Path folder = Boolean.getBoolean("updateModuleDocs") ? COMMITTED_DOCS : BUILD_DOCS;
        // The Documenter empties the folder first, so make sure it is exactly the docs folder
        Path resolved = folder.toAbsolutePath().normalize();
        assertThat(resolved.endsWith(Path.of("docs", "modules"))
                || resolved.endsWith(Path.of("target", "spring-modulith-docs"))).as(resolved.toString()).isTrue();

        new Documenter(modules, Documenter.Options.defaults().withOutputFolder(folder.toString()))
                .writeDocumentation();

        assertThat(folder.resolve("components.puml")).exists();
    }
}
