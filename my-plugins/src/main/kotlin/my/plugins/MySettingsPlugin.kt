package my.plugins

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

class MySettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.gradle.lifecycle.beforeProject { project ->
            project.plugins.apply("org.jetbrains.kotlin.jvm")
            project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
                it.compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_1_9)
            }
            project.afterEvaluate {
                println("During configuration ${project.getVersionByName("kotlin")}")
            }
            val kotlinVersion = project.provider {
                project.getVersionByName("kotlin")
            }
            project.tasks.register("myTask", MyTask::class.java) { task ->
                task.kotlinVersion.set(kotlinVersion)
            }
        }
    }
}

abstract class MyTask : DefaultTask() {
    @get:Input
    abstract val kotlinVersion: Property<String>
    @TaskAction
    fun printVersion() {
        println("During task execution ${kotlinVersion.get()}")
    }
}

val Project.versionCatalog: VersionCatalog
    get() = project.extensions.getByType(VersionCatalogsExtension::class.java).find("libs").get()

fun Project.getLibraryByName(name: String): MinimalExternalModuleDependency {
    val library = versionCatalog.findLibrary(name)
    return if (library.isPresent) {
        library.get().get()
    } else {
        throw Exception("Could not find a library for `$name`")
    }
}

fun Project.getVersionByName(name: String): String {
    val version = versionCatalog.findVersion(name)
    return if (version.isPresent) {
        version.get().requiredVersion
    } else {
        throw Exception("Could not find a version for `$name`")
    }
}
