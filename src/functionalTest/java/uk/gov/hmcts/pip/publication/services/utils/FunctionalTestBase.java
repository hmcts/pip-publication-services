package uk.gov.hmcts.pip.publication.services.utils;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pip.publication.services.Application;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@SpringBootTest(classes = {Application.class, OAuthClient.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FunctionalTestBase extends RedisConfigurationTestBase {

    protected static final String CONTENT_TYPE_VALUE = "application/json";

    @Autowired
    private static OAuthClient authClient;

    private static String accessToken;

    @Value("${test-url}")
    private static String testUrl;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = testUrl;
        accessToken = authClient.generateAccessToken();
    }

    protected static String getAccessToken() {
        return accessToken;
    }

    protected Response doPostRequest(final String path, final Map<String, String> additionalHeaders,
                                     final Object body) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .body(body)
            .when()
            .post(path)
            .thenReturn();
    }

    private static Map<String, String> getRequestHeaders(final Map<String, String> additionalHeaders) {
        final Map<String, String> headers = new ConcurrentHashMap<>(Map.of(CONTENT_TYPE, CONTENT_TYPE_VALUE));
        if (!CollectionUtils.isEmpty(additionalHeaders)) {
            headers.putAll(additionalHeaders);
        }
        return headers;
    }
}
