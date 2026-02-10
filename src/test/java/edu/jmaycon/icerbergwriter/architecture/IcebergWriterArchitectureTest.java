package edu.jmaycon.icerbergwriter.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "edu.jmaycon.icerbergwriter",
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class IcebergWriterArchitectureTest {

    @ArchTest
    static final ArchRule icebergwriter_should_only_depend_on_itself = classes()
            .should()
            .onlyDependOnClassesThat(
                    resideInAPackage("edu.jmaycon.icerbergwriter..").or(not(resideInAPackage("edu.jmaycon.."))));
}
