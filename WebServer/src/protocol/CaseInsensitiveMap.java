package protocol;

import java.util.HashMap;

public class CaseInsensitiveMap extends HashMap<String, String> {
    @Override
    public String put(String key, String value) {
        return super.put(key.toLowerCase(), value);
    }

    public String get(String key) {
        return super.get(key.toLowerCase());
    }
}