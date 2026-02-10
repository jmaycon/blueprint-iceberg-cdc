package edu.jmaycon.cdcapp.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "edu.jmaycon.cdcapp",
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureTest {

    @ArchTest
    static final ArchRule no_cyclic_dependencies =
            slices().matching("edu.jmaycon.cdcapp.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule no_package_should_depend_on_runtime = noClasses()
            .that()
            .resideOutsideOfPackage("..runtime..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..runtime..");

    @ArchTest
    static final ArchRule trigger_can_only_depend_on_config_and_model = classes()
            .that()
            .resideInAPackage("..trigger..")
            .should()
            .onlyDependOnClassesThat(resideInAPackage("..trigger..")
                    .or(resideInAPackage("..config.."))
                    .or(resideInAPackage("..model.."))
                    .or(not(resideInAPackage("edu.jmaycon.cdcapp.."))));

    @ArchTest
    static final ArchRule config_should_only_depend_on_itself = classes()
            .that()
            .resideInAPackage("..config..")
            .should()
            .onlyDependOnClassesThat(resideInAPackage("..config..").or(not(resideInAPackage("edu.jmaycon.cdcapp.."))));

    @ArchTest
    static final ArchRule model_should_only_depend_on_itself = classes()
            .that()
            .resideInAPackage("..model..")
            .should()
            .onlyDependOnClassesThat(resideInAPackage("..model..").or(not(resideInAPackage("edu.jmaycon.cdcapp.."))));
}
