import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Avro 스키마 파일(.avsc)로부터 이벤트 명세 마크다운 문서를 자동 생성하는 Gradle Task
 *
 * 사용법:
 * ./gradlew generateAvroEventDocs
 *
 * 기능:
 * - Avro 스키마 파일을 읽어서 이벤트 명세를 자동 생성
 * - 이벤트 흐름 문서의 이벤트 명세 섹션을 자동 업데이트
 * - 단일 진실 공급원(Single Source of Truth): Avro 스키마
 */
open class AvroDocGenerator : DefaultTask() {

    @InputDirectory
    val avroSchemaDir = project.file("src/main/events/avro")

    @OutputDirectory
    val outputDir = project.file("docs/generated")

    @TaskAction
    fun generate() {
        val mapper = ObjectMapper().registerKotlinModule()
        val avroFiles = avroSchemaDir.walkTopDown()
            .filter { it.isFile && it.extension == "avsc" }
            .toList()

        outputDir.mkdirs()

        // Avro 스키마를 파싱하여 맵으로 저장
        val schemaMap = mutableMapOf<String, AvroSchemaInfo>()
        avroFiles.forEach { avroFile ->
            val schema = mapper.readValue<Map<String, Any>>(avroFile)
            val eventName = schema["name"] as String
            schemaMap[eventName] = AvroSchemaInfo(avroFile, schema)
        }

        // 1. 전체 이벤트 명세 문서 생성
        generateEventSpecifications(schemaMap)

        // 2. 이벤트 플로우 문서 자동 업데이트
        updateEventFlowDocuments(schemaMap)

        println("✅ Avro 이벤트 문서 생성 완료:")
        println("   - ${outputDir.absolutePath}/event-specifications.md")
        println("   - 이벤트 플로우 문서 자동 업데이트 완료")
        println("   생성된 이벤트 수: ${avroFiles.size}")
    }

    private fun generateEventSpecifications(schemaMap: Map<String, AvroSchemaInfo>) {
        val allEventsDoc = StringBuilder()
        allEventsDoc.appendLine("# 자동 생성된 이벤트 명세")
        allEventsDoc.appendLine()
        allEventsDoc.appendLine("> ⚠️ 이 파일은 자동 생성됩니다. 직접 수정하지 마세요.")
        allEventsDoc.appendLine("> Avro 스키마를 수정한 후 `./gradlew generateAvroEventDocs`를 실행하세요.")
        allEventsDoc.appendLine()

        schemaMap.values.sortedBy { it.file.name }.forEach { schemaInfo ->
            allEventsDoc.append(generateEventMarkdown(schemaInfo))
        }

        File(outputDir, "event-specifications.md").writeText(allEventsDoc.toString())
    }

    private fun updateEventFlowDocuments(schemaMap: Map<String, AvroSchemaInfo>) {
        val eventFlowsDir = project.file("event-flows")
        if (!eventFlowsDir.exists()) {
            println("⚠️  event-flows 디렉토리가 존재하지 않습니다.")
            return
        }

        var updatedCount = 0
        eventFlowsDir.walkTopDown()
            .filter { it.name == "README.md" }
            .forEach { readmeFile ->
                if (updateEventSpecSection(readmeFile, schemaMap)) {
                    updatedCount++
                    println("   - 업데이트: ${readmeFile.relativeTo(project.projectDir)}")
                }
            }

        if (updatedCount == 0) {
            println("   ℹ️  AUTO_GENERATED 마커가 있는 문서가 없습니다.")
        }
    }

    private fun updateEventSpecSection(readmeFile: File, schemaMap: Map<String, AvroSchemaInfo>): Boolean {
        val content = readmeFile.readText()
        val startMarker = "<!-- AUTO_GENERATED_EVENT_SPEC_START -->"
        val endMarker = "<!-- AUTO_GENERATED_EVENT_SPEC_END -->"

        if (!content.contains(startMarker) || !content.contains(endMarker)) {
            return false
        }

        val beforeSection = content.substringBefore(startMarker) + startMarker
        val afterSection = endMarker + content.substringAfter(endMarker)

        // README에서 언급된 이벤트 추출
        val referencedEvents = extractReferencedEvents(content, schemaMap.keys)

        val generatedSpec = StringBuilder()
        generatedSpec.appendLine()
        generatedSpec.appendLine()
        generatedSpec.appendLine("> ⚠️ 이 섹션은 자동 생성됩니다. `./gradlew generateAvroEventDocs`를 실행하면 업데이트됩니다.")
        generatedSpec.appendLine("> 명세를 수정하려면 `src/main/events/avro/*.avsc` 파일을 수정하세요.")
        generatedSpec.appendLine()

        referencedEvents.forEachIndexed { index, eventName ->
            val schemaInfo = schemaMap[eventName]
            if (schemaInfo != null) {
                generatedSpec.appendLine("### ${index + 1}. $eventName")
                generatedSpec.appendLine()
                generatedSpec.append(generateDetailedEventSpec(schemaInfo))
            }
        }

        val newContent = beforeSection + generatedSpec.toString() + afterSection
        readmeFile.writeText(newContent)
        return true
    }

    private fun extractReferencedEvents(content: String, availableEvents: Set<String>): List<String> {
        // README 내용에서 언급된 이벤트 이름 추출
        return availableEvents.filter { eventName ->
            content.contains(eventName)
        }.sortedBy { eventName ->
            // 문서에 등장하는 순서대로 정렬
            content.indexOf(eventName)
        }
    }

    private fun generateEventMarkdown(schemaInfo: AvroSchemaInfo): String {
        val schema = schemaInfo.schema
        val eventName = schema["name"] as String
        val namespace = schema["namespace"] as String
        val doc = schema["doc"] as? String ?: "설명 없음"
        val fields = schema["fields"] as List<Map<String, Any>>
        val topic = inferKafkaTopic(eventName)

        val markdown = StringBuilder()
        markdown.appendLine("## $eventName")
        markdown.appendLine()
        markdown.appendLine("**Namespace**: `$namespace`")
        markdown.appendLine()
        markdown.appendLine("**설명**: $doc")
        markdown.appendLine()
        markdown.appendLine("**Avro 스키마**: `src/main/events/avro/${schemaInfo.file.name}`")
        markdown.appendLine()
        markdown.appendLine("**Kafka 토픽**: `$topic`")
        markdown.appendLine()

        markdown.appendLine("### 필드")
        markdown.appendLine()
        markdown.appendLine("| 필드명 | 타입 | 필수 | 설명 |")
        markdown.appendLine("|--------|------|------|------|")

        fields.forEach { field ->
            val fieldName = field["name"] as String
            val fieldType = formatFieldType(field["type"])
            val fieldDoc = field["doc"] as? String ?: "-"
            val isRequired = !isOptionalField(field["type"])

            markdown.appendLine("| `$fieldName` | $fieldType | ${if (isRequired) "✅" else "❌"} | $fieldDoc |")
        }

        markdown.appendLine()
        markdown.appendLine("---")
        markdown.appendLine()

        return markdown.toString()
    }

    private fun generateDetailedEventSpec(schemaInfo: AvroSchemaInfo): String {
        val schema = schemaInfo.schema
        val fields = schema["fields"] as List<Map<String, Any>>
        val topic = inferKafkaTopic(schema["name"] as String)

        val markdown = StringBuilder()
        markdown.appendLine("**Kafka 토픽**: `$topic`")
        markdown.appendLine()
        markdown.appendLine("**Avro 스키마**: `src/main/events/avro/${schemaInfo.file.name}`")
        markdown.appendLine()

        markdown.appendLine("**필드**:")
        markdown.appendLine()
        markdown.appendLine("| 필드명 | 타입 | 필수 | 설명 |")
        markdown.appendLine("|--------|------|------|------|")

        fields.forEach { field ->
            val fieldName = field["name"] as String
            val fieldType = formatFieldType(field["type"])
            val fieldDoc = field["doc"] as? String ?: "-"
            val isRequired = !isOptionalField(field["type"])

            markdown.appendLine("| `$fieldName` | $fieldType | ${if (isRequired) "✅" else "❌"} | $fieldDoc |")
        }

        markdown.appendLine()

        return markdown.toString()
    }

    private data class AvroSchemaInfo(
        val file: File,
        val schema: Map<String, Any>
    )

    private fun inferKafkaTopic(eventName: String): String {
        // OrderCreatedEvent -> c4ang.order.created
        // PaymentCompletedEvent -> c4ang.payment.completed
        val words = eventName.replace("Event", "")
            .replace(Regex("([a-z])([A-Z])"), "$1-$2")
            .lowercase()
            .split("-")

        return "c4ang.${words.dropLast(1).joinToString(".")}.${words.last()}"
    }

    private fun formatFieldType(type: Any?): String {
        return when (type) {
            is String -> type
            is Map<*, *> -> {
                when (type["type"]) {
                    "record" -> type["name"] as String
                    "enum" -> "Enum: ${(type["symbols"] as List<*>).joinToString(", ")}"
                    "bytes" -> {
                        val logicalType = type["logicalType"] as? String
                        if (logicalType == "decimal") "Decimal" else "Bytes"
                    }
                    else -> type["type"].toString()
                }
            }
            is List<*> -> {
                // Union 타입 (nullable 처리)
                type.filterNot { it == "null" }.joinToString(" | ")
            }
            else -> type.toString()
        }
    }

    private fun isOptionalField(type: Any?): Boolean {
        return type is List<*> && type.contains("null")
    }
}
