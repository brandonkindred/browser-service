package com.looksee.browsing.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents an action that can be performed on a page.
 */
public enum Action {
	CLICK("click"),
	DOUBLE_CLICK("doubleClick"),
	HOVER("hover"),
	CLICK_AND_HOLD("clickAndHold"),
	CONTEXT_CLICK("contextClick"),
	RELEASE("release"),
	SEND_KEYS("sendKeys"),
	MOUSE_OVER("mouseover"),
	UNKNOWN("unknown");

	private String shortName;

	Action (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static Action create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(Action v : values()) {
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
