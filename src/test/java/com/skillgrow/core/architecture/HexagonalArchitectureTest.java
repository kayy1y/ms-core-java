package com.skillgrow.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Reglas de dependencia de la arquitectura hexagonal. Si alguien importa Spring o JPA dentro del
 * dominio, o un controller usa un repositorio, el build falla.
 */
@AnalyzeClasses(packages = "com.skillgrow.core", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domainIsPure = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "com.azure..",
                    "com.fasterxml..",
                    "..application..",
                    "..infrastructure..")
            .because("el dominio no conoce frameworks ni adaptadores")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationDoesNotKnowAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..",
                    "jakarta.persistence..",
                    "com.azure..")
            .because("los casos de uso solo hablan con los ports")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllersDoNotUseRepositories = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter.out..")
            .because("los controllers llaman a casos de uso, nunca a repositorios")
            .allowEmptyShould(true);
}
