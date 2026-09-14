plugins {
	kotlin("jvm") version "2.4.10"
	`java-gradle-plugin`
	id("org.jetbrains.dokka") version "2.2.0"
	`maven-publish`
	signing
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
			id = "io.github.autotweaker.plugin.versioning"
			implementationClass = "io.github.autotweaker.plugin.versioning.VersioningPlugin"
			displayName = "AutoTweaker Versioning"
			description = "Computes build versions from git tags and injects them into every project"
		}
	}
}

java {
	withSourcesJar()
}

val javadocJar = tasks.register<Jar>("javadocJar") {
	description = "将 Dokka 生成的文档打包为 javadoc jar"
	from(tasks.named("dokkaGeneratePublicationHtml"))
	archiveClassifier.set("javadoc")
}

publishing {
	publications.withType<MavenPublication>().configureEach {
		if (!name.endsWith("PluginMarkerMaven")) {
			artifact(javadocJar)
		}
		pom {
			name.set("AutoTweaker Versioning")
			description.set("A Gradle settings plugin for Git-tag-driven versioning: injects the version into all projects and generates a version.properties resource")
			url.set("https://github.com/AutoTweaker/gradle-versioning")
			licenses {
				license {
					name.set("GNU General Public License v3.0 or later")
					url.set("https://www.gnu.org/licenses/gpl-3.0.html")
				}
			}
			developers {
				developer {
					id.set("WhiteElephant-abc")
					name.set("WhiteElephant-abc")
					url.set("https://github.com/WhiteElephant-abc")
				}
			}
			scm {
				connection.set("scm:git:git://github.com/AutoTweaker/gradle-versioning.git")
				developerConnection.set("scm:git:ssh://git@github.com/AutoTweaker/gradle-versioning.git")
				url.set("https://github.com/AutoTweaker/gradle-versioning")
			}
		}
	}
}

signing {
	val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
	if (signingKey != null) {
		useInMemoryPgpKeys(signingKey, providers.gradleProperty("signingInMemoryKeyPassword").getOrElse(""))
		publishing.publications.configureEach { sign(this) }
	}
}
