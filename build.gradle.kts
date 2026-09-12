plugins {
	kotlin("jvm") version "2.4.10"
	`java-gradle-plugin`
	`maven-publish`
}

repositories {
	mavenCentral()
}

kotlin {
	jvmToolchain(25)
}

gradlePlugin {
	plugins {
		create("versioning") {
			id = "io.github.autotweaker.versioning"
			implementationClass = "io.github.autotweaker.gradle.plugin.versioning.VersioningPlugin"
			displayName = "AutoTweaker Versioning"
			description = "Computes build versions from git tags and injects them into every project"
		}
	}
}

publishing {
	repositories {
		maven {
			name = "GitHubPackages"
			url = uri("https://maven.pkg.github.com/AutoTweaker/gradle-versioning")
			credentials {
				username = System.getenv("GITHUB_ACTOR").orEmpty()
				password = System.getenv("GITHUB_TOKEN").orEmpty()
			}
		}
	}
}
