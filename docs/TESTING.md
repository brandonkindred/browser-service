# looksee-browser Module — Testing Guide

## Overview

The `looksee-browser` module provides Selenium-based browser automation, Appium mobile device interaction, and supporting utilities for CSS/HTML parsing. The test suite uses **JUnit 5** (Jupiter) with **Mockito** for mocking WebDriver, JavaScript executors, and Appium drivers.

## Running Tests

```bash
# Run all tests in the browser module
mvn test -pl looksee-browser -am

# Run tests with JaCoCo coverage report
mvn test -pl looksee-browser -am
# Report generated at: looksee-browser/target/site/jacoco/index.html

# Run a single test class
mvn test -pl looksee-browser -Dtest=BrowserTest

# Run a single test method
mvn test -pl looksee-browser -Dtest=BrowserTest#testNavigateTo
```

## Coverage

The module is configured with **JaCoCo** for code coverage reporting. After running `mvn test`, the HTML report is available at:

```
looksee-browser/target/site/jacoco/index.html
```

**Coverage target: 95%**

## Test Structure

Tests are located under `src/test/java/com/looksee/` mirroring the main source layout:

```
src/test/java/com/looksee/
├── browsing/
│   ├── enums/
│   │   ├── ActionTest.java              — Action enum: creation, short names, invalid values
│   │   ├── AlertChoiceTest.java         — AlertChoice enum: creation, toString, invalid values
│   │   ├── BrowserEnvironmentTest.java  — BrowserEnvironment enum: creation, invalid values
│   │   ├── BrowserTypeTest.java         — BrowserType enum: creation, isMobile(), invalid values
│   │   └── MobileActionTest.java        — MobileAction enum: creation, all 13 values, invalid
│   ├── helpers/
│   │   └── BrowserConnectionHelperTest.java — Round-robin URL selection, BrowserStack config,
│   │                                          mobile connection, Appium URL validation
│   ├── ActionFactoryTest.java           — All 7 action types (click, double-click, etc.)
│   ├── BrowserFactoryTest.java          — Unsupported type errors, Chrome/Firefox hub connection,
│   │                                      BrowserStack driver creation with all properties
│   ├── CoordinatesTest.java             — WebElement & Point constructors, pixel ratio scaling
│   ├── CrawlerTest.java                 — scrollDown, performAction, random location generation
│   ├── CssPropertyFactoryTest.java      — Margin, LineHeight, and generic CSS property types
│   ├── ElementNodeTest.java             — Tree operations: add/remove children, root/leaf checks
│   ├── MobileActionFactoryTest.java     — Constructor validation, SEND_KEYS, SwipeDirection enum
│   ├── MobileFactoryTest.java           — Unsupported platform errors, Android/iOS drivers,
│   │                                      BrowserStack mobile with device defaults
│   ├── RateLimitExecutorTest.java       — Constants, constructor, calculation verification
│   └── ValueDomainTest.java             — Domain generation, alphabet, special characters
├── config/
│   ├── AppiumPropertiesTest.java        — All properties, defaults, URL array parsing
│   ├── BrowserStackPropertiesTest.java  — All 15 properties, null defaults
│   └── SeleniumPropertiesTest.java      — URL parsing, defaults, empty/null handling
├── models/
│   ├── BrowserTest.java                 — Constructor, navigation, screenshots, scrolling,
│   │                                      element finding, attribute extraction, DOM manipulation,
│   │                                      alerts, viewport state, getter/setter coverage
│   ├── MobileDeviceTest.java            — Constructor, navigation, screenshots (viewport/element),
│   │                                      scrolling, element finding, attribute extraction,
│   │                                      DOM manipulation, viewport state, getter/setter
│   └── PageAlertTest.java               — Constructor, equals/hashCode, clone, performChoice,
│   │                                      static getMessage, key generation
└── utils/
    ├── CssUtilsTest.java                — loadCssProperties, loadTextCssProperties,
    │                                      loadPreRenderCssProperties (matching, @-rules, pseudo-selectors)
    ├── HtmlUtilsTest.java               — cleanSrc (scripts/styles/links/meta/whitespace),
    │                                      extractBody, is503Error, extractStylesheets,
    │                                      extractRuleSetsFromStylesheets (valid/invalid/multi)
    └── NetworkUtilsTest.java            — Invalid host IOException, HTTP ClassCast validation
```

## Test Categories

### Models (Browser, MobileDevice, PageAlert)
- **Browser.java** — Full lifecycle testing: construction (no-arg and parameterized), navigation with wait exceptions, viewport screenshots, element finding by XPath, attribute extraction (including duplicates, quotes, multi-value), DOM manipulation (removeElement, removeDriftChat, removeGDPR), scrolling (top/bottom/full/percent/element with nav/header shortcuts), viewport offset tracking, mouse actions, alert detection, page source, 503 error detection, and getter/setter coverage.
- **MobileDevice.java** — Mirrors Browser tests for mobile: construction, navigation, viewport/element/full-page screenshots via native Appium, element finding, attribute extraction edge cases, DOM manipulation, all scrolling methods, viewport offset (string/non-string), page source, 503 detection, and getter/setter coverage.
- **PageAlert.java** — Alert construction, key generation (SHA-256 consistency), equals/hashCode contract, clone semantics, performChoice (accept/dismiss/no-alert-present), static getMessage.

### Factories
- **BrowserFactory** — Unsupported browser type exception, Chrome/Firefox connection attempts, BrowserStack driver creation with full/minimal properties, capitalize helper.
- **MobileFactory** — Unsupported platform exception, Android/iOS driver creation, case-insensitive matching, BrowserStack mobile with default device names per platform.
- **ActionFactory** — All 7 Selenium action types exercised via mock driver.
- **MobileActionFactory** — Constructor validation (non-Appium driver rejection), SEND_KEYS delegation, SwipeDirection enum coverage.

### Connection Helpers
- **BrowserConnectionHelper** — Selenium URL configuration, Appium URL configuration, BrowserStack setup/clear lifecycle, round-robin hub selection (Chrome/Firefox DISCOVERY), TEST environment path, BrowserStack connection paths (desktop/mobile), empty Appium URL validation.

### Configuration Properties
- **SeleniumProperties** — Full constructor, null defaults, URL array parsing (multi/single/null/empty).
- **BrowserStackProperties** — All 15 fields, null defaults for booleans (realMobile, local, debug) and integers (connectionTimeout, maxRetries).
- **AppiumProperties** — All fields, null defaults, URL array parsing.

### Utilities
- **CssUtils** — Computed CSS via JavaScript, text CSS properties (partial/null/empty), pre-render CSS matching (element selectors, @-rule skipping, pseudo-selector stripping), no-match scenarios.
- **HtmlUtils** — Source cleaning (script/style/link/meta removal, whitespace normalization, carriage returns), body extraction, 503 error detection, stylesheet extraction (no links, non-stylesheet links, protocol-relative URLs, malformed URLs), CSS rule set parsing (valid/invalid/multi-sheet/font-face/media skipping).
- **NetworkUtils** — Invalid host IOException, HTTP-only URL ClassCastException.

### Enums
All 5 enums have complete coverage: creation from strings (case-insensitive), null/invalid argument exceptions, short name getters, toString, values() count, and type-specific methods (e.g., `BrowserType.isMobile()`).

### Other
- **Coordinates** — Both constructors, device pixel ratio scaling, zero coordinates.
- **ElementNode** — Tree construction, child management (by data/node), root/leaf detection, parent removal, multi-level tree traversal.
- **ValueDomain** — Domain generation (real numbers, decimals, alphabetic, special characters), toString, getter/setter.
- **RateLimitExecutor** — Constants verification, constructor, seconds-per-action calculation.
- **CssPropertyFactory** — Margin, LineHeight, and fallback property handling.

## Mocking Strategy

Since the module depends on live Selenium/Appium connections, all tests use **Mockito mocks**:

- **Combined mock interfaces** — `MockDriver extends WebDriver, JavascriptExecutor, TakesScreenshot` allows a single mock to satisfy all Browser/MobileDevice cast requirements.
- **`@TempDir`** — Used for screenshot tests that need real files for `ImageIO.read()`.
- **Exception-tolerant tests** — Tests for factory methods that attempt real connections wrap in `try/catch` or `assertThrows` since no Selenium/Appium hub is running during unit tests.

## Adding New Tests

1. Place tests in the matching package under `src/test/java/`.
2. Name the class `<ClassName>Test.java` (matched by Surefire's `**/*Test.java` pattern).
3. Use `@Mock` annotations with `MockitoAnnotations.openMocks(this)` in `@BeforeEach`.
4. For classes needing `JavascriptExecutor`, create a combined mock interface.
5. Run `mvn test -pl looksee-browser` to verify.
