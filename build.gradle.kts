plugins {
    java
    kotlin("jvm") version "1.9.21"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("maven-publish")
    idea
}

// GitHub Packages 배포를 위한 group 설정
group = "io.github.groomc4"  // GitHub Packages: io.github.{조직명_소문자}
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Avro - 이벤트/API 스키마 및 직렬화
    implementation("org.apache.avro:avro:1.11.3")
    implementation("io.confluent:kafka-avro-serializer:7.5.1")
    implementation("io.confluent:kafka-schema-registry-client:7.5.1")
}

// Avro 소스 디렉토리 설정 (events와 api 모두 포함)
sourceSets {
    main {
        java {
            // Avro 플러그인이 생성한 Java 클래스
            srcDir("build/generated-main-avro-java")
        }
    }
}

avro {
    setFieldVisibility("PRIVATE")
    setCreateSetters(true)
    setGettersReturnOptional(false)
    setOptionalGettersForNullableFieldsOnly(false)
    setStringType("String")
}

// Avro 스키마 경로 설정
tasks.named<com.github.davidmc24.gradle.plugin.avro.GenerateAvroJavaTask>("generateAvroJava") {
    // src/main/events/avro 와 src/main/api/avro 모두 인식
    source("src/main/events/avro", "src/main/api/avro")
}


// Avro 문서 자동 생성 태스크
tasks.register<AvroDocGenerator>("generateAvroEventDocs") {
    group = "documentation"
    description = "Avro 스키마로부터 이벤트 명세 문서를 자동 생성합니다"
}

// 빌드 시 Avro 문서 자동 생성
tasks.named("build") {
    dependsOn("generateAvroEventDocs")
}

// Publishing 설정
publishing {
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "c4ang-contract-hub"
            version = project.version.toString()

            pom {
                name.set("C4ANG Contract Hub")
                description.set("동기/비동기 통신 스키마 통합 관리 라이브러리")
                url.set("https://github.com/GroomC4/c4ang-contract-hub")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("GroomC4")
                        name.set("GroomC4 Team")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/GroomC4/c4ang-contract-hub.git")
                    developerConnection.set("scm:git:ssh://github.com/GroomC4/c4ang-contract-hub.git")
                    url.set("https://github.com/GroomC4/c4ang-contract-hub")
                }
            }
        }
    }

    repositories {
        // Maven Local (로컬 테스트용)
        mavenLocal()

        // GitHub Packages (중앙 패키지 허브로 배포)
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/GroomC4/c4ang-packages-hub")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// IntelliJ IDEA 설정
idea {
    module {
        // Avro 플러그인이 생성한 Java class를 소스 디렉토리로 인식
        generatedSourceDirs.add(file("build/generated-main-avro-java"))

        // Avro 스키마 파일을 리소스로 인식 (events와 api)
        resourceDirs.add(file("src/main/events/avro"))
        resourceDirs.add(file("src/main/api/avro"))
    }
}
