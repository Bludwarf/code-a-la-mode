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
    implementation("commons-io:commons-io:2.7")
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

tasks.register("concatenateFiles") {
    // TODO https://stackoverflow.com/questions/29692641/how-do-i-concatenate-multiple-files-in-gradle
//    inputs.files( fileTree( "src/main/kotlin" ) ).skipWhenEmpty()
//    outputs.file( "${project.buildDir}/codingame.kt" )
    doLast {
        println("Hello world!") // https://docs.gradle.org/current/userguide/tutorial_using_tasks.html#tutorial_using_tasks
//        outputs.files.singleFile.withOutputStream { out ->
//            for ( file in inputs.files ) file.withInputStream { out << it << '\n' }
//        }
    }
}
