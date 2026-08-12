import com.google.cloud.artifactregistry.gradle.plugin.ArtifactRegistryGradlePlugin
import org.cadixdev.gradle.licenser.Licenser
import org.cadixdev.gradle.licenser.LicenseExtension

plugins {
    `java-library`
    id("org.cadixdev.licenser") version "0.5.0" apply false
    id("com.google.cloud.artifactregistry.gradle-plugin") version "2.2.0" apply false
}

val projectName: String by project
val projectUrl: String by project
val projectInceptionYear: String by project
val bombeVersion: String by project

val isSnapshot = version.toString().endsWith("-SNAPSHOT")

allprojects {
    group = "org.cadixdev"
    version = "0.5.13"
}

subprojects {
    apply<JavaLibraryPlugin>()
    apply<MavenPublishPlugin>()
    apply<Licenser>()
    apply<ArtifactRegistryGradlePlugin>()

    repositories {
        mavenCentral()

        maven {
            url = uri("artifactregistry://us-central1-maven.pkg.dev/mw-lunarclient-maven-repo/public")
        }
    }

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.7.0"))
        testImplementation("org.junit.jupiter:junit-jupiter-api")
        testImplementation("org.junit.jupiter:junit-jupiter-engine")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
        withSourcesJar()
        withJavadocJar()
    }

    tasks.javadoc {
        options.optionFiles(rootProject.file("gradle/javadoc.options"))
    }

    configure<LicenseExtension> {
        header = rootProject.file("HEADER.txt")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(8)
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.processResources {
        from(rootProject.file("LICENSE.txt"))
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()

                from(components["java"])
                withoutBuildIdentifier()

                pom {
                    name.set(projectName)
                    description.set(project.description)
                    packaging = "jar"
                    url.set(projectUrl)
                    inceptionYear.set(projectInceptionYear)

                    scm {
                        connection.set("scm:git:https://github.com/CadixDev/Lorenz.git")
                        developerConnection.set("scm:git:git@github.com:CadixDev/Lorenz.git")
                        url.set("https://github.com/CadixDev/Lorenz")
                    }

                    issueManagement {
                        system.set("GitHub")
                        url.set("https://github.com/CadixDev/Lorenz/issues")
                    }

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("jamierocks")
                            name.set("Jamie Mansfield")
                            email.set("jmansfield@cadixdev.org")
                            url.set("https://www.jamiemansfield.me")
                            timezone.set("Europe/London")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                url = uri("artifactregistry://us-central1-maven.pkg.dev/mw-lunarclient-maven-repo/public")
            }
        }
    }

    if (project.hasProperty("ossrhUsername") && project.hasProperty("ossrhPassword")) {
        apply<SigningPlugin>()
        configure<SigningExtension> {
            useGpgCmd()
            setRequired {
                !isSnapshot && (
                    gradle.taskGraph.hasTask("publishAllPublicationsToOssrhRepository")
                        || gradle.taskGraph.hasTask("publishMavenPublicationToOssrhRepository")
                )
            }
            sign(project.extensions.getByType<PublishingExtension>().publications["maven"])
        }
    }
}
