package com.c4ang.contract

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * C4ang Contract Hub Application
 *
 * 이 애플리케이션은 MSA 서비스 간 계약 관리를 위한 중앙 집중식 시스템입니다.
 * - Spring Cloud Contract를 통한 HTTP API 계약 관리
 * - Kafka/Avro 기반 이벤트 흐름 문서화
 */
@SpringBootApplication
class ContractHubApplication

fun main(args: Array<String>) {
    runApplication<ContractHubApplication>(*args)
}
