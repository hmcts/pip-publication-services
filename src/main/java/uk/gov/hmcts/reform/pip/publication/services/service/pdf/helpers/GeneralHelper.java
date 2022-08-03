package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

/**
 * Class for static utility methods assisting with json->html->pdf issues.
 */
public final class GeneralHelper {

    private GeneralHelper() {
        throw new UnsupportedOperationException();
    }

    public static String stringDelimiter(String text, String delimiter) {
        return text.isEmpty() ? "" : delimiter;
    }

    public static String findAndReturnNodeText(JsonNode node, String nodeName) {
        if (node.has(nodeName)) {
            return node.get(nodeName).asText();
        }
        return "";
    }

    public static String trimAnyCharacterFromStringEnd(String text) {
        return StringUtils.isBlank(text) ? "" : text.trim().replaceAll(",$", "");
    }

    public static void appendToStringBuilder(StringBuilder builder, String text, JsonNode node,
                                             String nodeName) {
        builder.append('\n')
            .append(text)
            .append(GeneralHelper.findAndReturnNodeText(node, nodeName));
    }

    public static void loopAndFormatString(JsonNode nodes, String nodeName,
                                           StringBuilder builder, String delimiter) {
        nodes.get(nodeName).forEach(node -> {
            if (!node.asText().isEmpty()) {
                builder
                    .append(node.asText())
                    .append(delimiter);
            }
        });
    }
}
