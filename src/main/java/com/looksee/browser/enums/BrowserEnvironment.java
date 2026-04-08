package com.looksee.browser.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all browser environments that exist in the system
 */
public enum BrowserEnvironment {
	TEST("test"),
    DISCOVERY("discovery");

	private String shortName;

	BrowserEnvironment(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static BrowserEnvironment create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(BrowserEnvironment v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    public String getShortName() {
        return shortName;
    }
}
