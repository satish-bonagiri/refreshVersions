package de.fayard

import com.squareup.kotlinpoet.FileSpec
import de.fayard.internal.BuildSrcVersionsExtensionImpl
import de.fayard.internal.Dependency
import de.fayard.internal.DependencyGraph
import de.fayard.internal.OutputFile
import de.fayard.internal.PluginConfig
import de.fayard.internal.kotlinpoet
import de.fayard.internal.parseGraph
import de.fayard.internal.sortedBeautifullyBy
import org.gradle.api.Action
import org.gradle.api.DefaultTask
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
    fun taskActionUpdateBuildSrc() {
        val outputDir = project.file(OutputFile.OUTPUTDIR.path)

        val deps = parseGraph(dependencyGraph, PluginConfig.MEANING_LESS_NAMES)

        val libsFile: FileSpec = kotlinpoet(deps)

        libsFile.writeTo(outputDir)
        OutputFile.logFileWasModified(OutputFile.LIBS.path, OutputFile.LIBS.existed)
    }


    private val dependencyGraph: DependencyGraph by lazy {
        val extension: BuildSrcVersionsExtensionImpl = extension()

        val message = with(PluginConfig) {
            """
                |Running plugins.id("$PLUGIN_ID").version("$PLUGIN_VERSION") with useRefreshVersions=${useRefreshVersions} and extension: $extension
                |See documentation at $issue53PluginConfiguration
                |
            """.trimMargin()

        }
        println(message)
        OutputFile.configure(extension)

        val jsonInput = project.file(PluginConfig.BENMANES_REPORT_PATH)

        return@lazy PluginConfig.readGraphFromJsonFile(jsonInput)
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
