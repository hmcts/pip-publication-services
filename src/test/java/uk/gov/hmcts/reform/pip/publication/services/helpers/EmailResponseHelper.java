package uk.gov.hmcts.reform.pip.publication.services.helpers;

import uk.gov.service.notify.SendEmailResponse;

import java.util.UUID;

public final class EmailResponseHelper {

    private EmailResponseHelper() {
    }

    public static SendEmailResponse stubSendEmailResponseWithReferenceID(String reference) {
        UUID id = UUID.randomUUID();
        String body = String.format("{\"id\":\"%s\", \"reference\":\"%s\", \"content\":{\"body\":\"mock body\",\n"
                                        + "\"from_email\":\"mockemail@email.com\", \"subject\":\"subject\"},"
                                        + "\"template\":{\"id\":\"%s\", \"version\":2, \"uri\":\"uri\"}}",
                                    id, reference, id);

        return new SendEmailResponse(body);
    }
}
