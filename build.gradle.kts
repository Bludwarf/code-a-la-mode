plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.allopen") version "1.6.21"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-io:commons-io:2.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.assertj:assertj-core:3.23.1")
}

group = "com.codingame"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

allOpen {
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    kotlinOptions.javaParameters = true
}

tasks.register("codingame") {
//    dependsOn("build") // TODO does not work, see tasks.named("build") { finalizedBy("codingame") } below

    val `package` = "com.codingame.codealamode" // TODO property : should be common directory of inputs.files
    val packageDir = `package`.replace('.', '/')

    // Inspired by https://stackoverflow.com/a/47422803/1655155
    inputs.files(fileTree("src/main/kotlin/$packageDir")).skipWhenEmpty()
    outputs.file("${project.buildDir}/codingame/main.kt")
    doLast {
        outputs.files.singleFile.bufferedWriter().use { writer ->

            // TODO create enum for importLine & otherLine
            val linePairs: List<Pair<String, String>> = inputs.files.flatMap {file ->
                file.reader().use { reader ->
                    reader.readLines().mapNotNull { line ->
                        if (line.startsWith("package")) {
                            // Strip package line
                            null
                        } else if (line.startsWith("import")) {
                            // TODO en fait il faut plutôt garder uniquement les import autorisés : java.* et kotlin.*
                            val imported = line.substringAfter("import").trim()
                            if (imported.startsWith("java.") || imported.startsWith("kotlin.")) {
                                "importLine" to line
                            } else {
                                null
                            }
                        } else {
                            "otherLine" to line
                        }
                    }
                }
            }

            val categorizedLines: Map<String, List<String>> = linePairs
                .groupingBy { it.first }
                .aggregate { _, acc, element, _ ->
                    val list = acc ?: emptyList()
                    list + element.second
                }

            categorizedLines["importLine"]?.let { importLines ->
                importLines.toSortedSet().forEach {
                    writer.appendLine(it)
                }
            }
            categorizedLines["otherLine"]?.forEach {
                writer.appendLine(it)
            }
        }
    }
}

// Source : https://handstandsam.com/2021/06/07/run-custom-gradle-task-after-build/
// Indeed dependsOn("build") does not work in tasks.register("codingame") lambda
// To bind task with Build in IntelliJ : Grable panel > <project> > Tasks > other > codingame > Execute After Build
tasks.named("build") { finalizedBy("codingame") }
