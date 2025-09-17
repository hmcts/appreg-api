package uk.gov.hmcts.appregister.applicationentry.util;

/**
 * Represents a token in a template with its name, type, and maximum length.
 *
 * @param name the name of the token
 * @param type the type of the token (e.g., String, Integer)
 * @param maxLength the maximum length of the token value
 */
public record TemplateToken(String name, String type, int maxLength) {}
