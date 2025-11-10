package com.c4ang.contract

import io.restassured.RestAssured
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

/**
 * Base class for all contract tests.
 * This class sets up the test environment and configures RestAssured.
 *
 * Spring Cloud Contract는 이 클래스를 상속받아 자동 생성된 테스트를 실행합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setup() {
        RestAssured.baseURI = "http://localhost:$port"
    }
}
