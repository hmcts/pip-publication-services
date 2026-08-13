package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription;

import uk.gov.hmcts.reform.pip.model.publication.ListType;

import java.util.Set;

import static uk.gov.hmcts.reform.pip.model.publication.ListType.MAGISTRATES_ADULT_COURT_LIST_DAILY;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.MAGISTRATES_ADULT_COURT_LIST_FUTURE;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.MAGISTRATES_PUBLIC_ADULT_COURT_LIST_DAILY;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.MAGISTRATES_PUBLIC_ADULT_COURT_LIST_FUTURE;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.MAGISTRATES_PUBLIC_LIST;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.MAGISTRATES_STANDARD_LIST;

public final class SubscriptionTemplateHelper {

    public static final String IS_MAGISTRATES_MEDIA_PROTOCOL = "is_magistrates_media_protocol";
    public static final String IS_NOT_MAGISTRATES_MEDIA_PROTOCOL = "is_not_magistrates_media_protocol";

    private static final Set<ListType> MAGISTRATES_LIST_TYPES = Set.of(
        MAGISTRATES_PUBLIC_LIST,
        MAGISTRATES_STANDARD_LIST,
        MAGISTRATES_ADULT_COURT_LIST_DAILY,
        MAGISTRATES_ADULT_COURT_LIST_FUTURE,
        MAGISTRATES_PUBLIC_ADULT_COURT_LIST_DAILY,
        MAGISTRATES_PUBLIC_ADULT_COURT_LIST_FUTURE
    );

    private SubscriptionTemplateHelper() {
    }

    public static boolean isMagistratesMediaProtocol(ListType listType) {
        return listType != null && MAGISTRATES_LIST_TYPES.contains(listType);
    }

    public static boolean isNotMagistratesMediaProtocol(ListType listType) {
        return !isMagistratesMediaProtocol(listType);
    }
}

