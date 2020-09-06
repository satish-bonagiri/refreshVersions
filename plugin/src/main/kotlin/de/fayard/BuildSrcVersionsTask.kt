package de.fayard

import com.squareup.kotlinpoet.FileSpec
import de.fayard.internal.*
import de.fayard.internal.BuildSrcVersionsExtensionImpl
import de.fayard.internal.OutputFile
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType

@Suppress("UnstableApiUsage")
open class BuildSrcVersionsTask : DefaultTask() {

    @TaskAction
    fun taskActionInitializeBuildSrc() {

        project.file(OutputFile.OUTPUTDIR.path).also {
            if (it.isDirectory.not()) it.mkdirs()
        }
        for (output in OutputFile.values()) {
            output.existed = output.fileExists(project)
        }
        val initializationMap = mapOf(
            OutputFile.BUILD to PluginConfig.INITIAL_BUILD_GRADLE_KTS,
            OutputFile.GIT_IGNORE to PluginConfig.INITIAL_GITIGNORE
        )
        for ((outputFile, initialContent) in initializationMap) {
            if (outputFile.existed.not()) {
                project.file(outputFile.path).writeText(initialContent)
                OutputFile.logFileWasModified(outputFile.path, outputFile.existed)
            }
        }
    }


    @TaskAction
    fun taskUpdateLibsKt() {
        val outputDir = project.file(OutputFile.OUTPUTDIR.path)

        val allDependencies = findDependencies()
        val resolvedUseFqdn = PluginConfig.computeUseFqdnFor(allDependencies, emptyList(), PluginConfig.MEANING_LESS_NAMES)
        val deps = allDependencies.checkModeAndNames(resolvedUseFqdn)

        val libsFile: FileSpec = kotlinpoet(deps)

        libsFile.writeTo(outputDir)
        OutputFile.logFileWasModified(OutputFile.LIBS.path, OutputFile.LIBS.existed)
    }


    fun findDependencies(): List<Dependency> {
        println("> findDependencies()")
        val allDependencies = mutableListOf<Dependency>()
        project.allprojects {
            val projectName = name
            println("Configurations: " + configurations.map { it.name })
            allDependencies += (configurations + buildscript.configurations)
                // TODO: should we filter configurations?
                //.filter { it.name in PluginConfig.knownConfigurations }
                .flatMap {
                    val configurationName = "$projectName:${it.name}"
                    it.allDependencies.filterIsInstance<ExternalDependency>()
                        .map {
                            Dependency(it.group!!, it.name, it.version ?: "none")
                        }
                }
        }
        return allDependencies.distinctBy { d -> d.groupModule() }
    }


    @Input
    @Optional
    @Transient
    private lateinit var _extension: BuildSrcVersionsExtensionImpl

    fun configure(action: Action<BuildSrcVersionsExtension>) {
        val projectExtension = project.extensions.getByType<BuildSrcVersionsExtension>() as BuildSrcVersionsExtensionImpl
        this._extension = projectExtension.defensiveCopy()
        action.execute(this._extension)
        PluginConfig.useRefreshVersions = project.hasProperty("plugin.de.fayard.buildSrcVersions")
    }

    private fun extension(): BuildSrcVersionsExtensionImpl = _extension

}
