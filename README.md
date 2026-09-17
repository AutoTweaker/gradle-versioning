# AutoTweaker Versioning

这是一个 Gradle 插件，用于为项目版本号拼接构建元数据，并将 Manifest 属性 `Implementation-Version` 打包进 jar 供运行时解析。

> 注意，自 2.0.1 版本起，本插件不再发布到 GitHub Packages，请直接通过 Maven Central 访问插件。

## 使用方式

将以下内容添加到仓库 `gradle.properties`：

```properties
# SemVer 风格的版本号
version=0.2.0-alpha.7
```

注意：版本号实际上从 gradle 属性读取，这意味着 `~/.gradle/gradle.properties` 或 `-Pversion` 也能够影响插件读取到的版本号。

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
    id("io.github.autotweaker.plugin.versioning") version "3.0.0"
}

versioning {
    // 指定版本号生成模式，无默认值，必须手动设置。可以根据环境变量来切换生成模式，见下文示例。
    mode.set(VersionMode.RELEASE)
}
```

- 构建期：`project.version` 将被设置为计算后的版本号，在根项目或任意子项目的 `build.gradle.kts` 中直接使用 `project.version` 访问。
- 运行期：可读取 Manifest 属性 `Implementation-Version`，值为计算出的版本号。
- 外部脚本：调用根项目或任意子项目的 `exportVersion` 任务，会将版本号写入对应项目的 `build/generated/versioning/version.txt`，内容形如 `0.2.0-alpha.7+5d41fd9a\n`。

`VersionMode` 是一个枚举，以下是所有这三种枚举项的行为：

下述 `原始版本号` 指代 gradle 属性 `version` 的值。

- `RELEASE`：在原始版本号后拼接构建元数据，内容为当前仓库 HEAD 的短哈希。生成的版本号形如 `0.2.0-alpha.7+5d41fd9a`，如果原始版本号中已经包含了构建元数据，它会被剥离，git 哈希仍然被添加。
- `DEV`：同样拼接构建元数据，但会同时剥离原始版本号中的预发布信息和构建元数据（若有），并同时拼接时间戳和 git 哈希作为构建元数据，形如 `0.2.0-dev+1789480291.852bc71f`。
- `RAW`：不对原始版本号作任何处理，适用于不需要构建元数据拼接，只需要 Manifest 属性的情况。

## 示例代码

根据环境变量切换模式：

```kotlin
versioning {
    mode.set(
        if (System.getenv("RELEASE_MODE") == "1") VersionMode.RELEASE
        else VersionMode.DEV
    )
}
```

运行时取值：

```kotlin
// 在类或伴生对象内
val version: String = javaClass.getPackage().implementationVersion ?: error("Missing Implementation-Version")
```

在 CI 中提取版本号：

```shell
./gradlew :exportVersion
APP_VERSION="$(cat build/generated/versioning/version.txt)"
```
