package uk.gov.hmcts.reform.pip.publication.services.service.thirdparty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyAction;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyOauthConfiguration;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.publication.services.models.ThirdPartyPublicationMetadata;
import uk.gov.hmcts.reform.pip.publication.services.service.DataManagementService;

@Service
public class ThirdPartySubscriptionService {
    private final ThirdPartyApiService thirdPartyApiService;
    private final DataManagementService dataManagementService;

    @Autowired
    public ThirdPartySubscriptionService(ThirdPartyApiService thirdPartyApiService,
                                         DataManagementService dataManagementService) {
        this.thirdPartyApiService = thirdPartyApiService;
        this.dataManagementService = dataManagementService;
    }

    public String sendThirdPartySubscription(ThirdPartySubscription thirdPartySubscription) {
        ThirdPartyPublicationMetadata thirdPartyPublicationMetadata = null;
        String payload = null;
        byte[] file = null;
        String filename = null;

        if (ThirdPartyAction.NEW_PUBLICATION.equals(thirdPartySubscription.getThirdPartyAction())
            || ThirdPartyAction.UPDATE_PUBLICATION.equals(thirdPartySubscription.getThirdPartyAction())) {
            Artefact artefact = dataManagementService.getArtefact(thirdPartySubscription.getPublicationId());
            Location location = dataManagementService.getLocation(artefact.getLocationId());
            thirdPartyPublicationMetadata = new ThirdPartyPublicationMetadata()
                .convert(artefact, location.getName());

            if (artefact.getIsFlatFile()) {
                file = dataManagementService.getArtefactFlatFile(thirdPartySubscription.getPublicationId());
                String sourceArtefactId = artefact.getSourceArtefactId();
                String fileExtension = sourceArtefactId != null
                    ? sourceArtefactId.substring(sourceArtefactId.lastIndexOf("."))
                    : "";
                filename = thirdPartySubscription.getPublicationId() + fileExtension;
            } else {
                payload = dataManagementService.getArtefactJsonBlob(thirdPartySubscription.getPublicationId());
            }
        }

        return handleSubscriptionNotification(thirdPartySubscription, thirdPartyPublicationMetadata, payload,
                                              file, filename);
    }

    private String handleSubscriptionNotification(ThirdPartySubscription thirdPartySubscription,
                                                  ThirdPartyPublicationMetadata metadata, String payload,
                                                  byte[] file, String filename) {
        switch (thirdPartySubscription.getThirdPartyAction()) {
            case ThirdPartyAction.NEW_PUBLICATION -> {
                for (ThirdPartyOauthConfiguration oauthConfig
                    : thirdPartySubscription.getThirdPartyOauthConfigurationList()) {
                    thirdPartyApiService.sendNewPublicationToThirdParty(oauthConfig, metadata, payload, file,
                                                                        filename);
                }
                return "Successfully sent new publication to third party subscribers";
            }
            case ThirdPartyAction.UPDATE_PUBLICATION -> {
                for (ThirdPartyOauthConfiguration oauthConfig
                    : thirdPartySubscription.getThirdPartyOauthConfigurationList()) {
                    thirdPartyApiService.sendUpdatedPublicationToThirdParty(oauthConfig, metadata, payload, file,
                                                                            filename);
                }
                return "Successfully sent updated publication to third party subscribers";
            }
            case ThirdPartyAction.DELETE_PUBLICATION -> {
                for (ThirdPartyOauthConfiguration oauthConfig
                    : thirdPartySubscription.getThirdPartyOauthConfigurationList()) {
                    thirdPartyApiService.notifyThirdPartyOfPublicationDeletion(
                        oauthConfig, thirdPartySubscription.getPublicationId()
                    );
                }
                return "Successfully sent publication deleted notification to third party subscribers";
            }
            case ThirdPartyAction.HEALTH_CHECK -> {
                for (ThirdPartyOauthConfiguration oauthConfig
                    : thirdPartySubscription.getThirdPartyOauthConfigurationList()) {
                    thirdPartyApiService.thirdPartyHealthCheck(oauthConfig);
                }
                return "Successfully performed health check for third party subscribers";
            }
            default -> throw new IllegalArgumentException("Unsupported third party action: "
                                                              + thirdPartySubscription.getThirdPartyAction());
        }
    }
}
