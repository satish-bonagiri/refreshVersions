package de.fayard.internal

open class Dependency(
    open val group: String = "",
    open val name: String = "",
    open val version: String = ""
) {
    val module: String get() = name
    fun groupModuleVersion() = "$group:$module:$version"
    fun groupModuleUnderscore() = "$group:$module:_"
    fun groupModule() = "$group:$module"
    override fun toString() = groupModuleVersion()
}


data class DependencyGraph(
    val gradle: GradleConfig,
    val current: Dependencies,
    val exceeded: Dependencies,
    val outdated: Dependencies,
    val unresolved: Dependencies,
    val count: Int = 0
)


data class Dependencies(
    val dependencies: List<Dependency> = emptyList(),
    val count: Int = 0
) : List<Dependency> by dependencies

class Deps(
    val dependencies: List<Dependency>,
    val modes : Map<Dependency, VersionMode>,
    val names: Map<Dependency, String>
)

data class DependencyExt (
    override val group: String = "",
    override val version: String = "",
    val reason: String? = "",
    var latest: String? = "",
    val projectUrl: String? = "",
    override val name: String = "",
    var escapedName: String = "",
    var mode: VersionMode = VersionMode.MODULE,
    val available: AvailableDependency? = null
): Dependency(group, name, version)

enum class VersionMode {
    GROUP, GROUP_MODULE, MODULE
}

data class GradleConfig(
    val current: GradleVersion,
    val nightly: GradleVersion,
    val enabled: Boolean = false,
    val releaseCandidate: GradleVersion,
    val running: GradleVersion
)

data class GradleVersion(
        val version: String = "",
        val reason: String = "",
        val isUpdateAvailable: Boolean = false,
        val isFailure: Boolean = false
)

data class AvailableDependency(
        val release: String? = "",
        val milestone: String? = "",
        val integration: String? = ""
)


