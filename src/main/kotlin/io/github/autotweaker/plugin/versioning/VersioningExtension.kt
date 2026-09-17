package io.github.autotweaker.plugin.versioning

import org.gradle.api.provider.Property

abstract class VersioningExtension {
	abstract val mode: Property<VersionMode>
}
