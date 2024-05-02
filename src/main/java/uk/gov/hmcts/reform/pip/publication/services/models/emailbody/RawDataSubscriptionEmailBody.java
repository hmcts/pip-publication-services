package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RawDataSubscriptionEmailBody extends EmailBody {
    private Map<SubscriptionTypes, List<String>> subscriptions;
    private UUID artefactId;
    private Artefact artefact;
    private String locationName;
    private String artefactSummary;
    private byte[] pdf;
    private byte[] additionalPdf;
    private byte[] excel;

    public RawDataSubscriptionEmailBody(SubscriptionEmail subscriptionEmail, Artefact artefact, String artefactSummary,
                                        byte[] pdf, byte[] additionalPdf, byte[] excel, String locationName) {
        super(subscriptionEmail.getEmail());
        this.subscriptions = subscriptionEmail.getSubscriptions();
        this.artefactId = subscriptionEmail.getArtefactId();
        this.artefact = artefact;
        this.locationName = locationName;
        this.artefactSummary = artefactSummary;
        this.pdf = pdf == null ? new byte[0] : Arrays.copyOf(pdf, pdf.length);
        this.additionalPdf = additionalPdf == null ? new byte[0] : Arrays.copyOf(additionalPdf, additionalPdf.length);
        this.excel = excel == null ? new byte[0] : Arrays.copyOf(excel, excel.length);
    }
}
