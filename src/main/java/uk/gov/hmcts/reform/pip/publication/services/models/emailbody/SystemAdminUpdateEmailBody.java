package uk.gov.hmcts.reform.pip.publication.services.models.emailbody;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.ChangeType;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;

@Getter
@Setter
@NoArgsConstructor
public class SystemAdminUpdateEmailBody extends BatchEmailBody {
    private String requesterName;
    private ActionResult actionResult;
    private ChangeType changeType;
    private String additionalChangeDetail;

    public SystemAdminUpdateEmailBody(SystemAdminAction systemAdminAction) {
        super(systemAdminAction.getEmailList());
        this.requesterName = systemAdminAction.getRequesterName();
        this.actionResult = systemAdminAction.getActionResult();
        this.changeType = systemAdminAction.getChangeType();
        this.additionalChangeDetail = systemAdminAction.createAdditionalChangeDetail();
    }
}
