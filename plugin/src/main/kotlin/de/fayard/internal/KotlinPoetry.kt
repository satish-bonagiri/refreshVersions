package de.fayard.internal

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import de.fayard.BuildSrcVersionsExtension
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec


fun kotlinpoet(
    versions: List<Dependency>
): FileSpec {
    val indent = "    "

    val libsProperties: List<PropertySpec> = versions
        .distinctBy { it.escapedName }
        .map {
            val libValue = when {
                it.version == "none" -> CodeBlock.of("%S", "${it.group}:${it.name}")
                else -> CodeBlock.of("%S", "${it.group}:${it.name}:_")
            }
            constStringProperty(
                name = it.escapedName,
                initializer = libValue,
                kdoc = null
            )
        }


    val Libs = TypeSpec.objectBuilder("Libs")
        .addKdoc(PluginConfig.KDOC_LIBS)
        .addProperties(libsProperties)
        .build()


    val LibsFile = FileSpec.builder("", "Libs")
        .indent(indent)
        .addType(Libs)
        .build()

    return LibsFile

}

fun List<Dependency>.sortedBeautifullyBy(selection: (Dependency) -> String?) : List<Dependency> {
    return this.filterNot { selection(it) == null }
        .sortedBy { selection(it)!! }
        .sortedBy { it.mode }
}



fun Dependency.versionInformation(): String {
    val newerVersion = newerVersion()
    val comment = when {
        version == "none" -> "// No version. See buildSrcVersions#23"
        newerVersion == null -> ""
        else -> """ // available: "$newerVersion""""
    }
    val addNewLine = comment.length + versionName.length + version.length > 70

    return if (addNewLine) "\n$comment" else comment
}

fun Dependency.newerVersion(): String?  =
    when {
        available == null -> null
        available.release.isNullOrBlank().not() -> available.release
        available.milestone.isNullOrBlank().not() -> available.milestone
        available.integration.isNullOrBlank().not() -> available.integration
        else -> null
    }?.trim()


fun parseGraph(
    graph: DependencyGraph,
    useFdqn: List<String>
): List<Dependency> {
    val dependencies: List<Dependency> = graph.current + graph.exceeded + graph.outdated + graph.unresolved
    val resolvedUseFqdn = PluginConfig.computeUseFqdnFor(dependencies, useFdqn, PluginConfig.MEANING_LESS_NAMES)
    return dependencies.checkModeAndNames(resolvedUseFqdn).findCommonVersions()
}

fun List<Dependency>.checkModeAndNames(useFdqnByDefault: List<String>): List<Dependency> {
    for (d: Dependency in this) {
        d.mode = when {
            d.name in useFdqnByDefault -> VersionMode.GROUP_MODULE
            PluginConfig.escapeVersionsKt(d.name) in useFdqnByDefault -> VersionMode.GROUP_MODULE
            else -> VersionMode.MODULE
        }
        d.escapedName = PluginConfig.escapeVersionsKt(
            when (d.mode) {
                VersionMode.MODULE -> d.name
                VersionMode.GROUP -> d.groupOrVirtualGroup()
                VersionMode.GROUP_MODULE -> "${d.group}_${d.name}"
            }
        )
    }
    return this
}




fun List<Dependency>.findCommonVersions(): List<Dependency> {
    val map = groupBy { d: Dependency -> d.groupOrVirtualGroup() }
    for (deps in map.values) {
        val sameVersions = deps.map { it.version }.distinct().size == 1
        val hasVirtualGroup = deps.any { it.groupOrVirtualGroup() != it.group }
        if (sameVersions && (hasVirtualGroup || deps.size > 1)) {
            deps.forEach { d -> d.mode = VersionMode.GROUP }
        }
    }
    return this
}

fun constStringProperty(name: String, initializer: CodeBlock, kdoc: CodeBlock? = null) =
    PropertySpec.builder(name, String::class)
        .addModifiers(KModifier.CONST)
        .initializer(initializer)
        .apply {
            if (kdoc != null) addKdoc(kdoc)
        }.build()


fun constStringProperty(name: String, initializer: String, kdoc: CodeBlock? = null) =
    constStringProperty(name, CodeBlock.of("%S", initializer), kdoc)


