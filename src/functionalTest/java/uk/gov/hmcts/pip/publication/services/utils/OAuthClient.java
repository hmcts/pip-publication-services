package uk.gov.hmcts.pip.publication.services.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static io.restassured.RestAssured.given;

@Component
public class OAuthClient {
    private static final String BEARER = "Bearer ";

    @Value("${CLIENT_ID_FT}")
    private String clientId;

    @Value("${CLIENT_SECRET_FT}")
    private String clientSecret;

    @Value("${CLIENT_ID_B2C_FT}")
    private String clientIdB2C;

    @Value("${CLIENT_SECRET_B2C_FT}")
    private String clientSecretB2C;

    @Value("${TENANT_ID}")
    private String tenantId;

    @Value("${CLIENT_ID}")
    private String clientIdDataManagement;

    @Value("${CLIENT_SECRET}")
    private String clientSecretDataManagement;

    @Value("${APP_URI}")
    private String scope;

    @Value("${DATA_MANAGEMENT_AZ_API}")
    private String scopeDataManagement;

    public String generateBearerToken() {
        String token = given()
            .relaxedHTTPSValidation()
            .header("content-type", "application/x-www-form-urlencoded")
            .formParam("client_id", clientId)
            .formParam("scope", scope + "/.default")
            .formParam("client_secret", clientSecret)
            .formParam("grant_type", "client_credentials")
            .baseUri("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token")
            .post()
            .body()
            .jsonPath()
            .get("access_token");

        if (token == null) {
            throw new AuthException("Unable to generate access token for the API");
        }
        return BEARER + token;
    }

    public String generateB2cBearerToken() {
        String token = given()
            .relaxedHTTPSValidation()
            .header("content-type", "application/x-www-form-urlencoded")
            .formParam("client_id", clientIdB2C)
            .formParam("scope", scope + "/.default")
            .formParam("client_secret", clientSecretB2C)
            .formParam("grant_type", "client_credentials")
            .baseUri("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token")
            .post()
            .body()
            .jsonPath()
            .get("access_token");

        if (token == null) {
            throw new AuthException("Unable to generate access token for the API");
        }
        return BEARER + token;
    }

    public String generateDataManagementAccessToken() {
        String token = given()
            .relaxedHTTPSValidation()
            .header("content-type", "application/x-www-form-urlencoded")
            .formParam("client_id", clientIdDataManagement)
            .formParam("scope", scopeDataManagement + "/.default")
            .formParam("client_secret", clientSecretDataManagement)
            .formParam("grant_type", "client_credentials")
            .baseUri("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token")
            .post()
            .body()
            .jsonPath()
            .get("access_token");

        if (token == null) {
            throw new AuthException("Unable to generate access token for the API");
        }
        return BEARER + token;
    }
}

