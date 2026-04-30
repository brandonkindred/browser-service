package com.looksee.browser.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all browser types that exist in the system
 */
public enum BrowserType {
	CHROME("chrome"),
	FIREFOX("firefox"),
	SAFARI("safari"),
	IE("ie"),
	ANDROID("android"),
	IOS("ios");

	private String shortName;

	BrowserType(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static BrowserType create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(BrowserType v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    public String getShortName() {
        return shortName;
    }

    public boolean isMobile() {
        return this == ANDROID || this == IOS;
    }
}
