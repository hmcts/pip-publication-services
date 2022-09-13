package uk.gov.hmcts.reform.pip.publication.services;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum Environments {
    PRODUCTION("prod", "Production"),
    STAGING("stg", "Staging"),
    TEST("test", "Test"),
    ITHC("ithc", "ITHC"),
    DEMO("demo", "Demo"),
    SANDBOX("sbox", "Sand box"),
    LOCAL("local", "Local");

    private String originalName;
    private String formattedName;

    public static String convertEnvironmentName(String input) {
        return Arrays.stream(Environments.values())
            .filter(env -> env.originalName.equals(input))
            .findFirst().get().getFormattedName();
    }
}
