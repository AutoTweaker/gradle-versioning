package io.github.autotweaker.plugin.versioning

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class VersioningExtension {
	abstract val propertiesFile: RegularFileProperty
	abstract val versionMode: Property<VersionMode>
	abstract val generateResource: Property<Boolean>
}
