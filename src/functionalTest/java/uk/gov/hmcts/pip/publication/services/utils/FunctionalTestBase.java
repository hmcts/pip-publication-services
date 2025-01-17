package uk.gov.hmcts.pip.publication.services.utils;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pip.publication.services.Application;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@SpringBootTest(classes = {Application.class, OAuthClient.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FunctionalTestBase extends RedisConfigurationTestBase {

    protected static final String CONTENT_TYPE_VALUE = "application/json";

    @Autowired
    private OAuthClient authClient;

    protected String bearerToken;
    protected String dataManagementAccessToken;

    @Value("${test-url}")
    private String testUrl;

    @Value("${data-management-test-url}")
    private String dataManagementUrl;

    @BeforeAll
    void setUp() {
        RestAssured.baseURI = testUrl;
        bearerToken = authClient.generateBearerToken();
        dataManagementAccessToken = authClient.generateDataManagementAccessToken();
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

    protected Response doDataManagementPostRequest(final String path, final Map<String, String> additionalHeaders,
                                                   final Object body) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .baseUri(dataManagementUrl)
            .body(body)
            .when()
            .post(path)
            .thenReturn();
    }

    protected Response doDataManagementPostRequestMultiPart(final String path, final Map<String,
        String> additionalHeaders, final File multipartFile) {

        return given()
            .relaxedHTTPSValidation()
            .headers(additionalHeaders)
            .baseUri(dataManagementUrl)
            .accept("*/*")
            .multiPart(multipartFile)
            .when()
            .post(path)
            .thenReturn();
    }

    protected Response doDeleteRequest(final String path, final Map<String, String> additionalHeaders) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .when()
            .delete(path)
            .thenReturn();
    }

    protected Response doDataManagementDeleteRequest(final String path, final Map<String, String> additionalHeaders) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .baseUri(dataManagementUrl)
            .when()
            .delete(path)
            .thenReturn();
    }

    protected Response doPostRequestWithoutBody(final String path, final Map<String, String> additionalHeaders) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
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
