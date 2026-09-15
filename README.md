# AutoTweaker Versioning

这是一个 Gradle 插件，用于从 `gradle.properties` 读取版本号，拼接构建元数据，并将 `version.properties` 打包进项目 jar
供运行时解析。

> 注意，自 2.0.1 版本起，本插件不再发布到 GitHub Packages，请直接通过 Maven Central 访问插件。

## 使用方式

将以下内容添加到仓库 `gradle.properties`：

```properties
# SemVer 风格的版本号
version=0.2.0-alpha.7
```

将以下内容添加到仓库 `settings.gradle.kts`：

```kotlin
import io.github.autotweaker.plugin.versioning.VersionMode

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

plugins {
  id("io.github.autotweaker.plugin.versioning") version "2.0.1"
}

versioning {
  versionMode.set(VersionMode.RELEASE) // 模式设置，见下文
}
```

- 构建期：`project.version` 将被设置为计算后的版本号，在根项目或任意子项目的 `build.gradle.kts` 中直接使用
  `project.version` 访问。
- 运行期：可在 `version.properties` 资源中读取到 `version` 属性，值为计算出的版本号。可通过配置关闭此文件的生成，或修改生成路径。

`VersionMode` 是一个枚举，以下是所有这三种枚举项的行为：

下述 `原始版本号` 指代 `gradle.properties` 中配置的 `version` 属性。

- `RELEASE`：在原始版本号后拼接构建元数据，内容为当前仓库 HEAD 的短哈希。生成的版本号形如 `0.2.0-alpha.7+5d41fd9a`
  ，如果原始版本号中已经包含了构建元数据，它会被剥离，git 哈希仍然被添加。
- `DEV`：同样拼接构建元数据，但会同时剥离原始版本号中的预发布信息和构建元数据（若有），并同时拼接时间戳和 git 哈希作为构建元数据，形如
  `0.2.0-dev+1789480291.852bc71f`。
- `RAW`：不对原始版本号作任何处理，适用于不需要构建元数据拼接，只需要 `version.properties` 资源的情况。

## 可配置项

在 `settings.gradle.kts` 中，可以通过 `versioning` 来调整插件设置，其中 `versionMode` 是必须手动设置的。以下是所有的可配置项和它们的用途：

- `versionMode`：指定版本号生成模式，类型为 `VersionMode`，无默认值，必须手动设置。可以根据环境变量来切换生成模式，见下文示例。
- `propertiesFile`：可选，指定插件读取原始版本号的配置文件位置，类型为 `RegularFileProperty`，需要包含 `version` 属性。
- `generateResource`：可选，是否生成资源文件，类型为 `Boolean`。
- `resourcePath`：可选，资源文件在 jar 中的位置，类型为 `String`，默认为 `version.properties`。

## 示例代码

完整配置示例：

```kotlin
versioning {
  versionMode.set(VersionMode.RELEASE)
  // 以下三项均为默认值，在此写法下等价于不写
  propertiesFile.set(layout.settingsDirectory.file("gradle.properties"))
  generateResource.set(true)
  resourcePath.set("version.properties")
}
```

根据环境变量切换模式：

```kotlin
versioning {
  versionMode.set(
    if (System.getenv("RELEASE_MODE") == "1") VersionMode.RELEASE
    else VersionMode.DEV
  )
}
```

运行时取值：

```kotlin
// 在类或伴生对象中
val version: String by lazy {
  val props = Properties()
  javaClass.classLoader.getResourceAsStream("version.properties")
    ?.use { props.load(it) }
  props.getProperty("version")
}
```
