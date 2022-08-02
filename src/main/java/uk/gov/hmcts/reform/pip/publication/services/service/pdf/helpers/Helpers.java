package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import org.apache.commons.text.WordUtils;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for static utility methods assisting with json->html->pdf issues.
 */
public final class Helpers {

    private Helpers() {
        throw new UnsupportedOperationException();
    }

    public static String formatTimestampToBst(String timestamp) {
        Instant unZonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of("Europe/London");
        ZonedDateTime zonedDateTime = unZonedDateTime.atZone(zone);
        DateTimeFormatter dtf;
        dtf = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm");
        return dtf.format(zonedDateTime);
    }

    public static String formatLocalDateTimeToBst(LocalDateTime date) {
        return date.format(
            DateTimeFormatter.ofPattern("dd MMMM yyyy"));
    }

    public static String listTypeToCamelCase (ListType listType) {
        StringBuilder outputString = new StringBuilder();
        List<String> splitList = List.of(listType.toString().split("_"));
        outputString.append(splitList.get(0).toLowerCase());
        outputString.append(splitList.stream().skip(1).map(WordUtils::capitalizeFully).collect(Collectors.joining()));
        return outputString.toString();
    }
}
