package com.gmail.uprial.takeaim.config;

import com.gmail.uprial.takeaim.common.CustomLogger;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.gmail.uprial.takeaim.config.ConfigReaderSimple.getStringList;

public final class ConfigReaderEnums {
    public static Set<String> getStringSet(FileConfiguration config, CustomLogger customLogger, String key, String title) throws InvalidConfigException {
        List<String> strings = getStringList(config, customLogger, key, title);
        if (strings == null) {
            return null;
        }

        Set<String> set = new HashSet<>();
        int stringSize = strings.size();
        for(int i = 0; i < stringSize; i++) {
            String string = strings.get(i);
            if (set.contains(string)) {
                throw new InvalidConfigException(String.format("String '%s' in %s is not unique", string, title));
            }
            set.add(string);
        }
        return set;
    }}
