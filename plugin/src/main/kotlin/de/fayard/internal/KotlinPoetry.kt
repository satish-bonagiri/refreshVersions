package de.fayard.internal

import com.squareup.kotlinpoet.*


fun kotlinpoet(
    deps: Deps
): FileSpec {
    val dependencies: List<Dependency> = deps.dependencies
    val indent = "    "

    val libsProperties: List<PropertySpec> = dependencies
        .distinctBy { it.groupModule() }
        .map { d ->
            val libValue = when {
                d.version == "none" -> CodeBlock.of("%S", d.groupModule())
                else -> CodeBlock.of("%S", d.groupModuleUnderscore())
            }
            constStringProperty(
                name = deps.names[d]!!,
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

fun List<Dependency>.sortedBeautifullyBy(selection: (Dependency) -> String?): List<Dependency> {
    return this.filterNot { selection(it) == null }
        .sortedBy { selection(it)!! }
        //.sortedBy { it.mode }
}



fun parseGraph(
    graph: DependencyGraph,
    useFdqn: List<String>
): Deps {
    val dependencies: List<Dependency> = graph.current + graph.exceeded + graph.outdated + graph.unresolved
    val resolvedUseFqdn = PluginConfig.computeUseFqdnFor(dependencies, useFdqn, PluginConfig.MEANING_LESS_NAMES)
    return dependencies.checkModeAndNames(resolvedUseFqdn)
}

fun List<Dependency>.checkModeAndNames(useFdqnByDefault: List<String>): Deps {
    val dependencies = this
        .map { d ->
            if (d is DependencyExt) Dependency(d.group, d.name, d.version) else d
        }
        .sortedBeautifullyBy() { it.groupModuleVersion() }

    val modes: MutableMap<Dependency, VersionMode> = dependencies.associate { d: Dependency ->
        val mode = when {
            d.module in useFdqnByDefault -> VersionMode.GROUP_MODULE
            PluginConfig.escapeVersionsKt(d.module) in useFdqnByDefault -> VersionMode.GROUP_MODULE
            else -> VersionMode.MODULE
        }
        Pair(d, mode)
    }.toMutableMap()

    val names = dependencies.associate { d: Dependency ->
        val name = PluginConfig.escapeVersionsKt(
            when (modes[d]!!) {
                VersionMode.MODULE -> d.module
                VersionMode.GROUP -> d.group
                VersionMode.GROUP_MODULE -> "${d.group}_${d.module}"
            }
        )
        Pair(d, name)
    }

    // findCommonVersion
    val map = groupBy { d: Dependency -> d.group }
    for (deps in map.values) {
        val sameVersions = deps.map { it.version }.distinct().size == 1
        val hasVirtualGroup = deps.any { it.group != it.group }
        if (sameVersions && (hasVirtualGroup || deps.size > 1)) {
            deps.forEach { d -> modes[d] = VersionMode.GROUP }
        }
    }

    return Deps(dependencies, modes, names)
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


