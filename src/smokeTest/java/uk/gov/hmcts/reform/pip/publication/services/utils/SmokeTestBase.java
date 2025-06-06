package uk.gov.hmcts.reform.pip.publication.services.utils;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@SpringBootTest(classes = {OAuthClient.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SmokeTestBase {
    @Value("${TEST_URL:http://localhost:8081}")
    private String testUrl;

    @Autowired
    private OAuthClient authClient;

    @BeforeAll
    void startup() {
        RestAssured.baseURI = testUrl;
    }

    protected Response doGetRequest(final String path) {
        return given()
            .relaxedHTTPSValidation()
            .when()
            .get(path)
            .thenReturn();
    }

    protected Response doPostRequest(final String path, final Object body) {
        final Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(AUTHORIZATION, "bearer " + authClient.generateAccessToken());
        headers.put(CONTENT_TYPE, "application/json");

        return given()
            .relaxedHTTPSValidation()
            .headers(headers)
            .body(body)
            .when()
            .post(path)
            .thenReturn();
    }
}
