# DriverOps Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify shared `Browser` / `MobileDevice` DOM-nav APIs behind `DriverOps` + `AbstractWebDriverSession`, and migrate API services to `SessionHandle.ops()` while leaving screenshots and input stacks branched.

**Architecture:** New `DriverOps` interface and `AbstractWebDriverSession` abstract class in `engine` (`com.looksee.browser`). `Browser` and `MobileDevice` subclass the abstract base and keep only screenshot (+ desktop-only) methods. `SessionHandle.ops()` returns the non-null session as `DriverOps`. Services call `ops()` for shared work; `asBrowser()` / `asMobileDevice()` remain for screenshots, ActionFactory vs MobileActionFactory, and desktop-only APIs.

**Tech Stack:** Java 21, Maven multi-module (`engine`, `api`), JUnit 5, Mockito, Selenium 4 / Appium, Quarkus 3 API layer, Lombok on engine types.

**Spec:** `docs/superpowers/specs/2026-07-17-driverops-design.md`  
**Issue:** [#154](https://github.com/brandonkindred/browser-service/issues/154)

## Global Constraints

- No intentional REST/WS OpenAPI or wire contract changes
- Screenshots stay off `DriverOps`
- Keep `asBrowser()` / `asMobileDevice()` / `isMobile()` for divergent paths
- Preserve existing swallow-on-close / navigate catch behavior
- Do not implement #155 / #156 / #157 in this work
- Prefer branch off latest `main` (pull/rebase before coding if needed); do not commit unrelated `docker-compose.yml` / `.env.example` changes

---

## File map

| File | Role |
|------|------|
| Create `engine/.../DriverOps.java` | Shared contract |
| Create `engine/.../AbstractWebDriverSession.java` | Shared fields + method bodies |
| Modify `engine/.../Browser.java` | Extends abstract; keep screenshots + mouse/alerts/GDPR + busy-loop scroll overload + `scrollToElementCentered` |
| Modify `engine/.../MobileDevice.java` | Extends abstract; keep screenshots only |
| Create `engine/.../AbstractWebDriverSessionTest.java` | Unit tests for shared behavior via tiny test subclass |
| Modify `api/.../session/SessionHandle.java` | Add `ops()`; route `driver()` / `closeOnce()` through it |
| Modify `api/.../session/SessionHandleTest.java` | Assert `ops()` |
| Modify `api/.../service/BrowserOperationsService.java` | Shared ops via `ops()` |
| Modify `api/.../service/ElementOperationsService.java` | find/attributes via `ops()` |
| Modify `api/.../service/CaptureService.java` | nav/source/find/attributes via `ops()` |
| Modify `api/.../service/SessionService.java` | scroll offset via `ops()` |

Existing `BrowserTest` / `MobileDeviceTest` should keep passing after subclassing (they construct with `(driver, name)`).

---

### Task 1: DriverOps interface + failing AbstractWebDriverSession tests

**Files:**
- Create: `engine/src/main/java/com/looksee/browser/DriverOps.java`
- Create: `engine/src/test/java/com/looksee/browser/AbstractWebDriverSessionTest.java`
- Create (minimal stub only if needed for compile in later steps): nothing yet beyond interface for red tests that reference missing abstract class

**Interfaces:**
- Produces: `DriverOps` method signatures listed below

- [ ] **Step 1: Create `DriverOps.java`**

```java
package com.looksee.browser;

import java.util.Map;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/**
 * Shared DOM / navigation operations for desktop ({@link Browser}) and mobile ({@link
 * MobileDevice}) sessions. Screenshots and input stacks are intentionally not part of this
 * contract.
 */
public interface DriverOps {

  WebDriver getDriver();

  void navigateTo(String url);

  void waitForPageToLoad();

  void close();

  WebElement findWebElementByXpath(String xpath);

  WebElement findElement(String xpath) throws WebDriverException;

  boolean isDisplayed(String xpath);

  Map<String, String> extractAttributes(WebElement element);

  String getSource();

  boolean is503Error();

  void scrollToElement(WebElement element);

  void scrollToTopOfPage();

  void scrollToBottomOfPage();

  void scrollDownPercent(double percent);

  void scrollDownFull();

  Point getViewportScrollOffset();
}
```

- [ ] **Step 2: Write failing tests that require `AbstractWebDriverSession`**

Create `engine/src/test/java/com/looksee/browser/AbstractWebDriverSessionTest.java`:

```java
package com.looksee.browser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AbstractWebDriverSessionTest {

  interface MockDriver extends WebDriver, JavascriptExecutor, TakesScreenshot {}

  /** Concrete subclass for testing the abstract base in isolation. */
  static final class TestSession extends AbstractWebDriverSession {
    TestSession(WebDriver driver) {
      super(driver);
    }
  }

  @Mock private MockDriver driver;
  @Mock private WebElement mockElement;

  private DriverOps session;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(driver.executeScript(anyString(), any(Object[].class)))
        .thenAnswer(
            inv -> {
              String script = inv.getArgument(0);
              if (script.contains("innerWidth")) return "1920";
              if (script.contains("innerHeight")) return "1080";
              return null;
            });
    when(driver.executeScript(anyString()))
        .thenAnswer(
            inv -> {
              String script = inv.getArgument(0);
              if (script.contains("innerWidth")) return "1920";
              if (script.contains("innerHeight")) return "1080";
              if (script.contains("pageXOffset")) return "10,20";
              if (script.contains("readyState")) return "complete";
              return null;
            });
    session = new TestSession(driver);
  }

  @Test
  void getDriverReturnsInjectedDriver() {
    assertSame(driver, session.getDriver());
  }

  @Test
  void navigateToCallsDriverGet() {
    when(driver.executeScript("return document.readyState")).thenReturn("complete");
    session.navigateTo("http://example.com");
    verify(driver).get("http://example.com");
  }

  @Test
  void closeQuitsDriver() {
    session.close();
    verify(driver).quit();
  }

  @Test
  void closeSwallowsQuitExceptions() {
    doThrow(new RuntimeException("boom")).when(driver).quit();
    assertDoesNotThrow(() -> session.close());
  }

  @Test
  void findElementByXpath() {
    when(driver.findElement(By.xpath("//div"))).thenReturn(mockElement);
    assertSame(mockElement, session.findElement("//div"));
  }

  @Test
  void getSourceReturnsPageSource() {
    when(driver.getPageSource()).thenReturn("<html/>");
    assertEquals("<html/>", session.getSource());
  }

  @Test
  void scrollToTopExecutesScrollScript() {
    session.scrollToTopOfPage();
    verify(driver).executeScript(contains("scrollTo(0, 0)"));
  }

  @Test
  void getViewportScrollOffsetParsesJsResult() {
    when(driver.executeScript(contains("pageXOffset"))).thenReturn("12,34");
    var point = session.getViewportScrollOffset();
    assertEquals(12, point.getX());
    assertEquals(34, point.getY());
  }

  @Test
  void extractAttributesParsesJsList() {
    List<String> attrs = new ArrayList<>();
    attrs.add("id::main");
    attrs.add("class::foo bar");
    when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);
    Map<String, String> map = session.extractAttributes(mockElement);
    assertTrue(map.containsKey("id"));
    assertTrue(map.containsKey("class"));
  }
}
```

- [ ] **Step 3: Run tests to verify they fail (class missing)**

```bash
cd /Users/brandonkindred/Documents/GitHub/browser-service
mvn -pl engine -Dtest=AbstractWebDriverSessionTest test
```

Expected: compile failure — `cannot find symbol: class AbstractWebDriverSession`

- [ ] **Step 4: Commit interface + failing test**

```bash
git add engine/src/main/java/com/looksee/browser/DriverOps.java \
  engine/src/test/java/com/looksee/browser/AbstractWebDriverSessionTest.java
git commit -m "$(cat <<'EOF'
Add DriverOps interface and failing AbstractWebDriverSession tests.

EOF
)"
```

---

### Task 2: Implement AbstractWebDriverSession

**Files:**
- Create: `engine/src/main/java/com/looksee/browser/AbstractWebDriverSession.java`
- Test: `engine/src/test/java/com/looksee/browser/AbstractWebDriverSessionTest.java`

**Interfaces:**
- Consumes: `DriverOps`
- Produces: `AbstractWebDriverSession` implementing all `DriverOps` methods; protected `initViewport()`; public `removeElement(String)` (not on interface)

- [ ] **Step 1: Implement `AbstractWebDriverSession`**

Move shared bodies from `Browser.java` (identical in `MobileDevice.java`). File:

`engine/src/main/java/com/looksee/browser/AbstractWebDriverSession.java`

```java
package com.looksee.browser;

import com.looksee.browser.utils.HtmlUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor
@Getter
@Setter
public abstract class AbstractWebDriverSession implements DriverOps {

  private static final Logger log = LoggerFactory.getLogger(AbstractWebDriverSession.class);

  protected static final String JS_GET_VIEWPORT_WIDTH =
      "var width = undefined; if (window.innerWidth) {width = window.innerWidth;} else if (document.documentElement && document.documentElement.clientWidth) {width = document.documentElement.clientWidth;} else { var b = document.getElementsByTagName('body')[0]; if (b.clientWidth) {width = b.clientWidth;}};return width;";
  protected static final String JS_GET_VIEWPORT_HEIGHT =
      "var height = undefined;  if (window.innerHeight) {height = window.innerHeight;}  else if (document.documentElement && document.documentElement.clientHeight) {height = document.documentElement.clientHeight;}  else { var b = document.getElementsByTagName('body')[0]; if (b.clientHeight) {height = b.clientHeight;}};return height;";

  private WebDriver driver = null;
  private long yScrollOffset;
  private long xScrollOffset;
  private Dimension viewportSize;

  /** Used by tests and by subclasses that inject a pre-built driver. */
  protected AbstractWebDriverSession(WebDriver driver) {
    assert driver != null;
    this.driver = driver;
    setYScrollOffset(0);
    setXScrollOffset(0);
    setViewportSize(measureViewport(driver));
  }

  /** Subclasses that build the driver themselves call this after assigning {@code driver}. */
  protected final void initViewportState() {
    setYScrollOffset(0);
    setXScrollOffset(0);
    setViewportSize(measureViewport(this.driver));
  }

  @Override
  public WebDriver getDriver() {
    return this.driver;
  }

  @Override
  public void navigateTo(String url) {
    assert url != null;
    getDriver().get(url);
    try {
      waitForPageToLoad();
    } catch (Exception e) {
    }
  }

  @Override
  public void close() {
    try {
      driver.quit();
    } catch (Exception e) {
      log.debug("Exception occurred when closing session: " + e.getMessage());
    }
  }

  @Override
  public WebElement findWebElementByXpath(String xpath) {
    assert xpath != null;
    assert !xpath.isEmpty();
    return driver.findElement(By.xpath(xpath));
  }

  @Override
  public WebElement findElement(String xpath) throws WebDriverException {
    assert xpath != null;
    assert !xpath.isEmpty();
    return getDriver().findElement(By.xpath(xpath));
  }

  @Override
  public boolean isDisplayed(String xpath) {
    assert xpath != null;
    assert !xpath.isEmpty();
    return driver.findElement(By.xpath(xpath)).isDisplayed();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, String> extractAttributes(WebElement element) {
    assert element != null;
    List<String> attribute_strings =
        (ArrayList<String>)
            ((JavascriptExecutor) driver)
                .executeScript(
                    "var items = []; for (index = 0; index < arguments[0].attributes.length; ++index) { items.push(arguments[0].attributes[index].name + '::' + arguments[0].attributes[index].value) }; return items;",
                    element);
    return loadAttributes(attribute_strings);
  }

  private Map<String, String> loadAttributes(List<String> attributeList) {
    Map<String, String> attributes_seen = new HashMap<>();
    for (int i = 0; i < attributeList.size(); i++) {
      String[] attributes = attributeList.get(i).split("::");
      if (attributes.length > 1) {
        String attribute_name = attributes[0].trim().replace("\'", "'");
        String[] attributeVals = attributes[1].split(" ");
        if (!attributes_seen.containsKey(attribute_name)) {
          attributes_seen.put(attribute_name, Arrays.asList(attributeVals).toString());
        }
      }
    }
    return attributes_seen;
  }

  /** Shared by desktop and mobile; not on {@link DriverOps}. */
  public void removeElement(String class_name) {
    assert class_name != null;
    if (this.getDriver() instanceof JavascriptExecutor) {
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("return document.getElementsByClassName('" + class_name + "')[0].remove();");
    }
  }

  @Override
  public void scrollToElement(WebElement element) {
    assert element != null;
    ((JavascriptExecutor) driver)
        .executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    getViewportScrollOffset();
  }

  @Override
  public void scrollToBottomOfPage() {
    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
    getViewportScrollOffset();
  }

  @Override
  public void scrollToTopOfPage() {
    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    getViewportScrollOffset();
  }

  @Override
  public void scrollDownPercent(double percent) {
    ((JavascriptExecutor) driver)
        .executeScript("window.scrollBy(0, (window.innerHeight*" + percent + "))");
    getViewportScrollOffset();
  }

  @Override
  public void scrollDownFull() {
    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, window.innerHeight)");
    getViewportScrollOffset();
  }

  @Override
  public Point getViewportScrollOffset() {
    int x_offset = 0;
    int y_offset = 0;
    Object offset_obj =
        ((JavascriptExecutor) driver)
            .executeScript("return window.pageXOffset+','+window.pageYOffset;");
    if (offset_obj instanceof String) {
      String[] coord = ((String) offset_obj).split(",");
      x_offset = Integer.parseInt(coord[0]);
      y_offset = Integer.parseInt(coord[1]);
    }
    this.setXScrollOffset(x_offset);
    this.setYScrollOffset(y_offset);
    return new Point(x_offset, y_offset);
  }

  @Override
  public void waitForPageToLoad() {
    new WebDriverWait(driver, Duration.ofSeconds(30))
        .until(
            webDriver ->
                ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState")
                    .equals("complete"));
  }

  @Override
  public String getSource() {
    return this.getDriver().getPageSource();
  }

  @Override
  public boolean is503Error() {
    return HtmlUtils.is503Error(this.getSource());
  }

  protected static Dimension measureViewport(WebDriver driver) {
    return new Dimension(extractViewportWidth(driver), extractViewportHeight(driver));
  }

  protected static int extractViewportWidth(WebDriver driver) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    return Integer.parseInt(js.executeScript(JS_GET_VIEWPORT_WIDTH, new Object[0]).toString());
  }

  protected static int extractViewportHeight(WebDriver driver) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    return Integer.parseInt(js.executeScript(JS_GET_VIEWPORT_HEIGHT, new Object[0]).toString());
  }
}
```

- [ ] **Step 2: Run `AbstractWebDriverSessionTest`**

```bash
mvn -pl engine -Dtest=AbstractWebDriverSessionTest test
```

Expected: all tests PASS

- [ ] **Step 3: Commit**

```bash
git add engine/src/main/java/com/looksee/browser/AbstractWebDriverSession.java
git commit -m "$(cat <<'EOF'
Implement AbstractWebDriverSession shared DriverOps body.

EOF
)"
```

---

### Task 3: Make Browser extend AbstractWebDriverSession

**Files:**
- Modify: `engine/src/main/java/com/looksee/browser/Browser.java`
- Test: `engine/src/test/java/com/looksee/browser/BrowserTest.java` (should still pass; no required test edits if constructors preserved)

**Interfaces:**
- Consumes: `AbstractWebDriverSession`, `DriverOps`
- Produces: `Browser extends AbstractWebDriverSession` with desktop-only APIs retained

- [ ] **Step 1: Slim `Browser` to subclass + desktop-only methods**

Replace class declaration and remove duplicated shared methods. Keep:

- Fields: only `browserName` (shared driver/offsets/viewport live on parent)
- Constructors: after setting driver/name, call `initViewportState()` (URL ctor) or `super(driver)` then `setBrowserName` (injected ctor — prefer `super(driver)` + set name)
- Screenshots: `getViewportScreenshot`, `getFullPageScreenshot*`, `getElementScreenshot`
- Desktop-only: `removeDriftChat`, `removeGDPRmodals`, `removeGDPR`, `scrollToElement(String, WebElement)`, `scrollToElementCentered`, mouse + alert methods
- Inherit: all `DriverOps` methods + `removeElement`

Constructor pattern:

```java
@NoArgsConstructor
@Getter
@Setter
public class Browser extends AbstractWebDriverSession {

  private static final Logger log = LoggerFactory.getLogger(Browser.class);
  private String browserName;

  public Browser(String browser, URL hub_node_url) throws MalformedURLException {
    assert browser != null;
    assert hub_node_url != null;
    this.setBrowserName(browser);
    this.setDriver(BrowserFactory.createDriver(browser, hub_node_url));
    initViewportState();
  }

  public Browser(WebDriver driver, String browserName) {
    super(driver);
    assert browserName != null;
    this.setBrowserName(browserName);
  }

  // ... screenshot + mouse/alert/GDPR + scrollToElement(xpath,elem) + scrollToElementCentered only
}
```

Delete from `Browser` every method now on `AbstractWebDriverSession` (navigate, close, find*, isDisplayed, extractAttributes, loadAttributes, removeElement, scrollToElement(WebElement), scroll top/bottom/percent/full, getViewportScrollOffset, waitForPageToLoad, getSource, is503Error, private viewport helpers, JS_GET_* constants, driver/offset/viewport fields).

Keep `removeElement` usage via inheritance for `removeDom` BY_CLASS.

- [ ] **Step 2: Run Browser + abstract tests**

```bash
mvn -pl engine -Dtest=BrowserTest,AbstractWebDriverSessionTest test
```

Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add engine/src/main/java/com/looksee/browser/Browser.java
git commit -m "$(cat <<'EOF'
Refactor Browser to extend AbstractWebDriverSession.

EOF
)"
```

---

### Task 4: Make MobileDevice extend AbstractWebDriverSession

**Files:**
- Modify: `engine/src/main/java/com/looksee/browser/MobileDevice.java`
- Test: `engine/src/test/java/com/looksee/browser/MobileDeviceTest.java`

**Interfaces:**
- Consumes: `AbstractWebDriverSession`
- Produces: `MobileDevice extends AbstractWebDriverSession`

- [ ] **Step 1: Slim `MobileDevice` like Browser**

```java
@NoArgsConstructor
@Getter
@Setter
public class MobileDevice extends AbstractWebDriverSession {

  private String platformName;

  public MobileDevice(String platformType, URL serverUrl) {
    assert platformType != null;
    assert serverUrl != null;
    this.setPlatformName(platformType);
    this.setDriver(MobileFactory.createDriver(platformType, serverUrl));
    initViewportState();
  }

  public MobileDevice(WebDriver driver, String platformName) {
    super(driver);
    assert platformName != null;
    this.setPlatformName(platformName);
  }

  // keep only: getViewportScreenshot, getFullPageScreenshot, getElementScreenshot
}
```

Delete duplicated shared methods/fields/constants.

- [ ] **Step 2: Run engine tests**

```bash
mvn -pl engine test
```

Expected: PASS (all engine unit tests)

- [ ] **Step 3: Commit**

```bash
git add engine/src/main/java/com/looksee/browser/MobileDevice.java
git commit -m "$(cat <<'EOF'
Refactor MobileDevice to extend AbstractWebDriverSession.

EOF
)"
```

---

### Task 5: SessionHandle.ops()

**Files:**
- Modify: `api/src/main/java/io/browserservice/api/session/SessionHandle.java`
- Modify: `api/src/test/java/io/browserservice/api/session/SessionHandleTest.java`

**Interfaces:**
- Consumes: `DriverOps` (`com.looksee.browser.DriverOps`)
- Produces: `SessionHandle.ops(): DriverOps`

- [ ] **Step 1: Add failing assertions to `SessionHandleTest`**

In `desktopFactoryBuildsADesktopSession`, after existing asserts:

```java
assertThat(handle.ops()).isSameAs(browser);
```

In `mobileFactoryBuildsAMobileSession`:

```java
assertThat(handle.ops()).isSameAs(device);
```

- [ ] **Step 2: Run test — expect fail if `ops` missing**

```bash
mvn -pl api -Dtest=SessionHandleTest#desktopFactoryBuildsADesktopSession test
```

Expected: compile error `cannot find symbol: method ops()`

- [ ] **Step 3: Implement `ops()` and route helpers**

In `SessionHandle.java`:

```java
import com.looksee.browser.DriverOps;

public DriverOps ops() {
  return mobileDevice != null ? mobileDevice : browser;
}

public WebDriver driver() {
  return ops().getDriver();
}

public boolean closeOnce() {
  if (!closed.compareAndSet(false, true)) {
    return false;
  }
  try {
    ops().close();
  } catch (Exception e) {
    log.warn("error while closing session {}: {}", id, e.toString());
  }
  elements.clear();
  return true;
}
```

Leave `asBrowser()` / `asMobileDevice()` / `isMobile()` unchanged.

- [ ] **Step 4: Run SessionHandleTest**

```bash
mvn -pl api -Dtest=SessionHandleTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/io/browserservice/api/session/SessionHandle.java \
  api/src/test/java/io/browserservice/api/session/SessionHandleTest.java
git commit -m "$(cat <<'EOF'
Add SessionHandle.ops() for shared DriverOps access.

EOF
)"
```

---

### Task 6: Migrate BrowserOperationsService to ops()

**Files:**
- Modify: `api/src/main/java/io/browserservice/api/service/BrowserOperationsService.java`
- Test: `api/src/test/java/io/browserservice/api/service/BrowserOperationsServiceTest.java` (verify still works — mocks are the same Browser/MobileDevice instances returned by `ops()`)

**Interfaces:**
- Consumes: `SessionHandle.ops(): DriverOps`

- [ ] **Step 1: Replace shared branches**

**navigate:**

```java
DriverOps ops = h.ops();
ops.navigateTo(req.url());
ops.waitForPageToLoad();
```

**getSource / status source:**

```java
String source = h.ops().getSource();
```

**viewport scroll offset (both call sites):**

```java
Point offset = h.ops().getViewportScrollOffset();
```

**performScroll** — rewrite to use `DriverOps` for shared modes:

```java
private void performScroll(SessionHandle h, ScrollRequest req) {
  DriverOps ops = h.ops();
  switch (req.mode()) {
    case TO_TOP -> ops.scrollToTopOfPage();
    case TO_BOTTOM -> ops.scrollToBottomOfPage();
    case TO_ELEMENT -> {
      if (req.elementHandle() == null || req.elementHandle().isBlank()) {
        throw new ValidationFailedException("element_handle is required for TO_ELEMENT");
      }
      ops.scrollToElement(h.elements().get(req.elementHandle()));
    }
    case TO_ELEMENT_CENTERED -> {
      if (req.elementHandle() == null || req.elementHandle().isBlank()) {
        throw new ValidationFailedException("element_handle is required for TO_ELEMENT_CENTERED");
      }
      WebElement el = h.elements().get(req.elementHandle());
      if (h.isMobile()) {
        ops.scrollToElement(el);
      } else {
        h.asBrowser().scrollToElementCentered(el);
      }
    }
    case DOWN_PERCENT -> {
      if (req.percent() == null) {
        throw new ValidationFailedException("percent is required for DOWN_PERCENT");
      }
      ops.scrollDownPercent(req.percent());
    }
    case DOWN_FULL -> ops.scrollDownFull();
  }
}
```

**Keep unchanged:** `renderPageScreenshot` (mobile/desktop screenshot branch), `removeDom`, `moveMouse`, `executeScript` (already uses `h.driver()`).

Add import: `com.looksee.browser.DriverOps`. Remove unused `MobileDevice` import if no longer referenced outside `renderPageScreenshot` (still needed there).

- [ ] **Step 2: Run BrowserOperationsServiceTest**

```bash
mvn -pl api -Dtest=BrowserOperationsServiceTest test
```

Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/io/browserservice/api/service/BrowserOperationsService.java
git commit -m "$(cat <<'EOF'
Migrate BrowserOperationsService shared paths to SessionHandle.ops().

EOF
)"
```

---

### Task 7: Migrate ElementOperationsService, CaptureService, SessionService

**Files:**
- Modify: `api/src/main/java/io/browserservice/api/service/ElementOperationsService.java`
- Modify: `api/src/main/java/io/browserservice/api/service/CaptureService.java`
- Modify: `api/src/main/java/io/browserservice/api/service/SessionService.java`
- Tests: corresponding `*Test.java` / IT classes as applicable

**Interfaces:**
- Consumes: `SessionHandle.ops(): DriverOps`

- [ ] **Step 1: ElementOperationsService**

Replace find/attributes branches:

```java
WebElement element = h.ops().findElement(req.xpath());
Map<String, String> attrs = h.ops().extractAttributes(element);
```

Keep ActionFactory / MobileActionFactory branching and element screenshot branching.

- [ ] **Step 2: CaptureService**

```java
DriverOps ops = sess.ops();
ops.navigateTo(req.url());
ops.waitForPageToLoad();
// ...
String html = sess.ops().getSource();
// find / attributes:
WebElement el = sess.ops().findElement(xpath);
Map<String, String> attrs = sess.ops().extractAttributes(element);
```

Keep `renderPageScreenshot` mobile/desktop screenshot switch.

- [ ] **Step 3: SessionService.safeScrollOffset**

```java
return handle.ops().getViewportScrollOffset();
```

(wrap in existing try/catch if present)

- [ ] **Step 4: Run affected API tests**

```bash
mvn -pl api -Dtest=ElementOperationsServiceTest,CaptureServiceTest,SessionServiceTest,SessionHandleTest,BrowserOperationsServiceTest test
```

If a named test class does not exist, omit it and run the closest existing suite. Then:

```bash
mvn -pl api -Dtest='*Operations*Test,*Capture*Test,*Session*Test' test
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add \
  api/src/main/java/io/browserservice/api/service/ElementOperationsService.java \
  api/src/main/java/io/browserservice/api/service/CaptureService.java \
  api/src/main/java/io/browserservice/api/service/SessionService.java
git commit -m "$(cat <<'EOF'
Migrate remaining services to SessionHandle.ops() for shared DriverOps.

EOF
)"
```

---

### Task 8: Full verification + issue checklist

**Files:** none required unless tests need small mock adjustments

- [ ] **Step 1: Run full engine + api tests**

```bash
mvn -pl engine,api test
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Grep for leftover shared branching that should be gone**

```bash
rg -n 'asMobileDevice\(\)\.(navigateTo|waitForPageToLoad|getSource|findElement|extractAttributes|getViewportScrollOffset|scrollTo)' \
  api/src/main/java
rg -n 'asBrowser\(\)\.(navigateTo|waitForPageToLoad|getSource|findElement|extractAttributes|getViewportScrollOffset|scrollToTop|scrollToBottom|scrollDown)' \
  api/src/main/java
```

Expected: no matches for those shared methods (screenshot / ActionFactory / centered scroll / removeDom / moveMouse may still use `asBrowser`/`asMobileDevice`).

- [ ] **Step 3: Confirm acceptance criteria**

- [ ] Shared `DriverOps` covers navigate, wait, find, source, viewport offset, shared scroll, attributes, close
- [ ] `Browser` / `MobileDevice` no longer duplicate those method bodies
- [ ] Services use `ops()` for shared ops; branching only where behavior differs
- [ ] Tests green
- [ ] No intentional REST/WS contract changes

- [ ] **Step 4: Final commit only if Step 2/3 required small fixes; otherwise done**

If fixes were needed:

```bash
git add -u
git commit -m "$(cat <<'EOF'
Finish DriverOps migration cleanup for #154.

EOF
)"
```

---

## Spec coverage self-review

| Spec requirement | Task |
|------------------|------|
| `DriverOps` interface | Task 1 |
| `AbstractWebDriverSession` shared bodies | Task 2 |
| `Browser` / `MobileDevice` subclass, no duplicate shared bodies | Tasks 3–4 |
| Screenshots stay off interface | Tasks 3–4, 6–7 keep screenshot branches |
| `SessionHandle.ops()` + close/driver via ops | Task 5 |
| Service migration | Tasks 6–7 |
| Keep `asBrowser`/`asMobileDevice` for divergent paths | Tasks 5–7 |
| Tests green / no API contract change | Task 8 |
| `removeElement` on abstract base | Task 2 |
| `TO_ELEMENT_CENTERED` special case | Task 6 |

## Placeholder / consistency check

- Method names match `DriverOps` throughout (`getViewportScrollOffset`, not `getScrollOffset`)
- Constructors use `super(driver)` or `setDriver` + `initViewportState()`
- No TBD/TODO left in plan steps
