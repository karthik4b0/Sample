// Sanitize the settingsJson to remove unwanted characters
        settingsJson = sanitizeJson(settingsJson);

public String sanitizeJson(String json) {
    // Remove the specific characters '?' and '~'
    json = json.replaceAll("[\\?~]", "");
    json = json.replaceAll("[\\u0000-\\u001F\\u0085\\u0028\\u0029\\u003F]", "");
json = json.replaceAll("[\\u0000-\\u001F\\u0085\\u0028\\u0029]", "");

    return json;
}
