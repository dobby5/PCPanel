package obsremote.requests;

import java.util.Map;

public class GetSourceSettingsResponse extends ResponseBase {
    private String sourceName;

    private String sourceType;

    private Map<String, Object> sourceSettings;

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public Map<String, Object> getSourceSettings() {
        return sourceSettings;
    }
}
