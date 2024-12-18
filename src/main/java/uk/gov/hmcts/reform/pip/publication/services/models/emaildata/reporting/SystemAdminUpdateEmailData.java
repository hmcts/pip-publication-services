package uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.ChangeType;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.BatchEmailData;

@Getter
@Setter
@NoArgsConstructor
public class SystemAdminUpdateEmailData extends BatchEmailData {
    private String requesterEmail;
    private ActionResult actionResult;
    private ChangeType changeType;
    private String additionalChangeDetail;
    private String envName;

    public SystemAdminUpdateEmailData(SystemAdminAction systemAdminAction, String envName, String referenceId) {
        super(systemAdminAction.getEmailList(), referenceId);
        this.requesterEmail = systemAdminAction.getRequesterEmail();
        this.actionResult = systemAdminAction.getActionResult();
        this.changeType = systemAdminAction.getChangeType();
        this.additionalChangeDetail = systemAdminAction.createAdditionalChangeDetail();
        this.envName = envName;
    }
}
