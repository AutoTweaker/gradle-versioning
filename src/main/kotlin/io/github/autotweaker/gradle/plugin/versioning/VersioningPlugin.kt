package io.github.autotweaker.gradle.plugin.versioning

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.util.*

class VersioningPlugin : Plugin<Settings> {
	@Suppress("UnstableApiUsage")
	override fun apply(settings: Settings) {
		val extension = settings.extensions.create("versioning", VersioningExtension::class.java).apply {
			propertiesFile.convention(settings.layout.settingsDirectory.file("gradle.properties"))
			resourcePath.convention("version.properties")
			generateResource.convention(true)
		}
		val providers = settings.providers
		val settingsDirectory = settings.layout.settingsDirectory
		val properties by lazy { readProperties(extension.propertiesFile.get().asFile) }
		val resolvedGroup by lazy { properties.getProperty("group") }
		val resolvedVersion by lazy { computeVersion(properties, extension, providers, settingsDirectory) }
		
		settings.gradle.lifecycle.beforeProject { project ->
			project.group = resolvedGroup
			project.version = resolvedVersion
			if (extension.generateResource.get()) {
				project.registerVersionResource(extension.resourcePath.get(), resolvedVersion)
			}
		}
	}
	
	private fun computeVersion(
		properties: Properties,
		extension: VersioningExtension,
		providers: ProviderFactory,
		settingsDirectory: Directory,
	): String {
		val baseVersion = properties.getProperty("version")
		val gitHash = providers.of(GitHashProvider::class.java) { spec ->
			spec.parameters.workingDirectory.set(settingsDirectory)
		}.get()
		return when (extension.versionMode.get()) {
			VersionMode.RELEASE -> "$baseVersion+$gitHash"
			
			VersionMode.DEV -> {
				val timestamp = System.currentTimeMillis() / 1000
				"${baseVersion.replace(Regex("-[a-zA-Z].*"), "")}-dev+$timestamp.$gitHash"
			}
		}
	}
	
	private fun Project.registerVersionResource(resourcePath: String, versionText: String) {
		val outputDir = layout.buildDirectory.dir("generated/versioning/resources")
		val generateTask = tasks.register("generateVersionProperties") { task ->
			task.description = "生成 $resourcePath，内含本次构建的版本号"
			val target = outputDir.map { it.file(resourcePath) }
			task.inputs.property("version", versionText)
			task.outputs.file(target)
			task.doLast {
				target.get().asFile.apply {
					parentFile.mkdirs()
					writeText("version=$versionText")
				}
			}
		}
		tasks.withType(ProcessResources::class.java).configureEach { processResources ->
			processResources.dependsOn(generateTask)
			processResources.from(outputDir) { spec ->
				spec.include(resourcePath)
			}
		}
	}
	
	private fun readProperties(file: File): Properties =
		Properties().apply { file.inputStream().use { load(it) } }
}

interface GitHashParameters : ValueSourceParameters {
	val workingDirectory: DirectoryProperty
}

abstract class GitHashProvider : ValueSource<String, GitHashParameters> {
	override fun obtain(): String {
		val directory = parameters.workingDirectory.get().asFile
		return runCatching {
			val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
				.directory(directory)
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.start()
			val output = process.inputStream.bufferedReader().readText().trim()
			if (process.waitFor() != 0) throw RuntimeException("git exited non-zero")
			output
		}.getOrDefault("unknown")
	}
}
