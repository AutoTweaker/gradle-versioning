package io.github.autotweaker.plugin.versioning

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.bundling.Jar

@Suppress("unused")
class VersioningPlugin : Plugin<Settings> {
	@Suppress("UnstableApiUsage")
	override fun apply(settings: Settings) {
		val extension = settings.extensions.create("versioning", VersioningExtension::class.java)
		val providers = settings.providers
		val baseVersion = providers.gradleProperty("version").orNull
			?: error("Missing version property, define it in gradle.properties")
		val settingsDirectory = settings.layout.settingsDirectory
		val resolvedVersion by lazy { computeVersion(baseVersion, extension, providers, settingsDirectory) }

		settings.gradle.lifecycle.beforeProject { project ->
			project.version = resolvedVersion
			project.registerVersionManifest(resolvedVersion)
		}
	}

	private fun computeVersion(
		baseVersion: String,
		extension: VersioningExtension,
		providers: ProviderFactory,
		settingsDirectory: Directory,
	): String {
		val gitHash = providers.of(GitHashProvider::class.java) { spec ->
			spec.parameters.workingDirectory.set(settingsDirectory)
		}.get()
		return when (extension.mode.get()) {
			VersionMode.RELEASE -> "${baseVersion.substringBefore('+')}+$gitHash"
			
			VersionMode.DEV -> {
				val unixTime = System.currentTimeMillis() / 1000
				"${baseVersion.substringBefore('-').substringBefore('+')}-dev+$unixTime.$gitHash"
			}
			
			VersionMode.RAW -> baseVersion
		}
	}
	
	private fun Project.registerVersionManifest(versionText: String) {
		tasks.withType(Jar::class.java).configureEach { jar ->
			jar.manifest.attributes["Implementation-Version"] = versionText
		}
	}
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
