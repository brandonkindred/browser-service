package com.looksee.browser.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents a touch action that can be performed on a mobile device.
 * This is the mobile counterpart of {@link Action}.
 */
public enum MobileAction {
	TAP("tap"),
	DOUBLE_TAP("doubleTap"),
	LONG_PRESS("longPress"),
	SWIPE_UP("swipeUp"),
	SWIPE_DOWN("swipeDown"),
	SWIPE_LEFT("swipeLeft"),
	SWIPE_RIGHT("swipeRight"),
	SCROLL_UP("scrollUp"),
	SCROLL_DOWN("scrollDown"),
	PINCH("pinch"),
	ZOOM("zoom"),
	SEND_KEYS("sendKeys"),
	UNKNOWN("unknown");

	private String shortName;

	MobileAction(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static MobileAction create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(MobileAction v : values()) {
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
