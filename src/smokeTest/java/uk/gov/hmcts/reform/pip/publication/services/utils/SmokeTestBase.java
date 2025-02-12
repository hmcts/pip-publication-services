package uk.gov.hmcts.reform.pip.publication.services.utils;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.pip.publication.services.Application;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@SpringBootTest(classes = {Application.class, OAuthClient.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SmokeTestBase {
    protected String accessToken;
    protected String b2cAccessToken;

    @Value("${TEST_URL:http://localhost:8081}")
    private String testUrl;

    @Autowired
    private OAuthClient authClient;

    @BeforeAll
    void startup() {
        RestAssured.baseURI = testUrl;
        accessToken = authClient.generateAccessToken();
        b2cAccessToken = authClient.generateB2cAccessToken();
    }

    protected Response doGetRequest(final String path) {
        return given()
            .relaxedHTTPSValidation()
            .when()
            .get(path)
            .thenReturn();
    }

    protected Response doPostRequest(final String path, final Object body, String accessToken) {
        final Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(AUTHORIZATION, "bearer " + accessToken);
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
