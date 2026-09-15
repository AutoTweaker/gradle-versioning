pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
	}
}

plugins {
	id("com.gradleup.nmcp.settings") version "1.6.2"
}

nmcpSettings {
	centralPortal {
		username = providers.gradleProperty("centralPortalUsername").getOrElse("")
		password = providers.gradleProperty("centralPortalPassword").getOrElse("")
		publishingType = "USER_MANAGED"
	}
}

rootProject.name = "gradle-versioning"
