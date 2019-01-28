package fr.slama.yeahbot.language;

/**
 * Created on 02/10/2018.
 */
public enum Bundle {

    ARGUMENTS("arguments"),
    ARGUMENTS_DESCRIPTION("argumentsDescription"),
    CAPTION("caption"),
    CATEGORY("category"),
    DESCRIPTION("description"),
    ERROR("error"),
    PERMISSION("permission"),
    SETTINGS("settings"),
    STRINGS("strings"),
    UNIT("unit");

    private String name;

    Bundle(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
