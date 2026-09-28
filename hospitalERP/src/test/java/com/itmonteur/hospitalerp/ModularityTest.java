package com.itmonteur.hospitalerp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Module boundaries, checked by Spring Modulith (docs/MULTI_MODULE_PLAN.md, step 2.4).
 *
 * Each sub-package com.itmonteur.hospitalerp.&lt;module&gt; is a module. Its top-level package is the
 * module's API; sub-packages such as internal and web are hidden from other modules.
 * The test fails on a dependency cycle between modules, on use of another module's
 * internal classes, and on field injection of another module's beans.
 */
class ModularityTest {

    private final ApplicationModules modules = ApplicationModules.of(HospitalErpApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        modules.verify();
    }
}
