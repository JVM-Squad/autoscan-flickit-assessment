package org.flickit.assessment.common.application.domain.kit;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.flickit.assessment.common.application.MessageBundle;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum KitLanguage {

    EN("en"), FA("fa");

    private final String title;

    private static final Map<String, KitLanguage> NAME_MAP = Stream.of(values())
        .collect(toMap(Enum::name, e -> e));

    public String getTitle() {
        return MessageBundle.message(getClass().getSimpleName() + "_" + name());
    }

    public int getId() {
        return this.ordinal();
    }

    public String getCode() {
        return this.name();
    }

    public static KitLanguage valueOfById(int id) {
        if (!isValidId(id))
            return getDefault();
        return values()[id];
    }

    public static boolean isValidId(int id) {
        return id >= 0 && id < KitLanguage.values().length;
    }

    public static KitLanguage getDefault() {
        return EN;
    }

    public static KitLanguage valueOfOrNull(String name) {
        return NAME_MAP.get(name);
    }

    public static KitLanguage getEnum(String name) {
        return EnumUtils.getEnum(KitLanguage.class, name, getDefault());
    }
}
