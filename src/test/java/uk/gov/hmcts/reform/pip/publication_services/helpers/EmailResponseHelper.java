package uk.gov.hmcts.reform.pip.publication_services.helpers;

import uk.gov.service.notify.SendEmailResponse;
import java.util.UUID;

public class EmailResponseHelper {

    public static SendEmailResponse stubSendEmailResponseWithReferenceID(String reference) {
        UUID id = UUID.randomUUID();
        String body = String.format("{\n" +
                                        "   \"id\":\"%s\",\n" +
                                        "   \"reference\":\"%s\",\n" +
                                        "   \"content\":{\n" +
                                        "      \"body\":\"mock body\",\n" +
                                        "      \"from_email\":\"mockemail@email.com\",\n" +
                                        "      \"subject\":\"subject\"\n" +
                                        "   },\n" +
                                        "   \"template\":{\n" +
                                        "      \"id\":\"%s\",\n" +
                                        "      \"version\":2,\n" +
                                        "      \"uri\":\"uri\"\n" +
                                        "   }\n" +
                                        "}", id, reference, id);

        return new SendEmailResponse(body);
    }
}
