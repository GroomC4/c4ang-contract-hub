plugins {
    java
    kotlin("jvm") version "1.9.21"
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.springframework.cloud.contract") version "4.1.0"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("maven-publish")
    idea
}

// JitPack 사용을 위한 group 설정
// 실제 배포 시 com.github.{GitHub_유저명}으로 변경 필요
group = "com.c4ang"  // 로컬 개발용
// group = "com.github.your-username"  // JitPack 배포용 (주석 해제 및 유저명 변경)
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

dependencies {
    // Spring Cloud Contract
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-verifier")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")

    // Contract DSL 의존성 (IDE에서 .kts 파일 인식을 위해 필요)
    implementation("org.springframework.cloud:spring-cloud-contract-spec-kotlin")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // Avro
    implementation("org.apache.avro:avro:1.11.3")
    implementation("io.confluent:kafka-avro-serializer:7.5.1")
    implementation("io.confluent:kafka-schema-registry-client:7.5.1")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:json-path")
    testImplementation("io.rest-assured:xml-path")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
    }
}

// Spring Cloud Contract 플러그인이 자동 생성한 contractTest source set 사용

contracts {
    testFramework.set(org.springframework.cloud.contract.verifier.config.TestFramework.JUNIT5)
    testMode.set(org.springframework.cloud.contract.verifier.config.TestMode.EXPLICIT)
    baseClassForTests.set("com.c4ang.contract.BaseContractTest")
    // Spring Cloud Contract 플러그인은 기본적으로 src/contractTest/resources/contracts를 사용
    contractsDslDir.set(file("src/contractTest/resources/contracts"))
}

avro {
    setFieldVisibility("PRIVATE")
    setCreateSetters(true)
    setGettersReturnOptional(false)
    setOptionalGettersForNullableFieldsOnly(false)
    setStringType("String")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named("contractTest") {
    dependsOn("test")
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
        // JitPack은 기본 'java' component를 자동으로 사용
        // 별도 설정 없어도 동작하지만, 명시적으로 정의 가능
        create<MavenPublication>("release") {
            from(components["java"])

            // Artifact 커스터마이징 (선택 사항)
            // artifactId = "c4ang-contract-hub"  // 기본값: 프로젝트 이름
        }
    }

    repositories {
        // Maven Local (로컬 테스트용)
        mavenLocal()

        // JitPack은 별도 repository 설정 불필요
        // Git Tag만 Push하면 자동으로 빌드됨
        // 사용법: https://jitpack.io/#your-username/c4ang-contract-hub
    }
}

// IntelliJ IDEA 설정
idea {
    module {
        // 생성된 파일들을 소스 디렉토리로 인식
        generatedSourceDirs.add(file("build/generated-main-avro-java"))

        // Spring Cloud Contract가 생성한 테스트 소스도 인식
        generatedSourceDirs.add(file("build/generated-test-sources/contracts"))
    }
}
