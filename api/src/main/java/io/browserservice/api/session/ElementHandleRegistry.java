package io.browserservice.api.session;

import io.browserservice.api.error.ElementHandleNotFoundException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.openqa.selenium.WebElement;

public class ElementHandleRegistry {

  private final ConcurrentHashMap<String, WebElement> handles = new ConcurrentHashMap<>();
  private final AtomicLong counter = new AtomicLong(0);

  public String put(WebElement element) {
    String id = "el_" + counter.incrementAndGet();
    handles.put(id, element);
    return id;
  }

  public WebElement get(String handle) {
    WebElement element = handles.get(handle);
    if (element == null) {
      throw new ElementHandleNotFoundException(handle);
    }
    return element;
  }

  public int size() {
    return handles.size();
  }

  public Set<String> keys() {
    return Set.copyOf(handles.keySet());
  }

  public void clear() {
    handles.clear();
  }
}
