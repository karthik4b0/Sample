// Sanitize the settingsJson to remove unwanted characters
        settingsJson = sanitizeJson(settingsJson);

public String sanitizeJson(String json) {
    // Remove the specific characters '?' and '~'
    json = json.replaceAll("[\\?~]", "");

    return json;
}
