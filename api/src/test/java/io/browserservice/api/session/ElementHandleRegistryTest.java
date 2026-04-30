package io.browserservice.api.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.browserservice.api.error.ElementHandleNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.WebElement;

class ElementHandleRegistryTest {

    @Test
    void putReturnsMonotonicIdsAndGetResolves() {
        ElementHandleRegistry registry = new ElementHandleRegistry();
        WebElement first = Mockito.mock(WebElement.class);
        WebElement second = Mockito.mock(WebElement.class);

        String firstHandle = registry.put(first);
        String secondHandle = registry.put(second);

        assertThat(firstHandle).isEqualTo("el_1");
        assertThat(secondHandle).isEqualTo("el_2");
        assertThat(registry.get(firstHandle)).isSameAs(first);
        assertThat(registry.get(secondHandle)).isSameAs(second);
        assertThat(registry.size()).isEqualTo(2);
        assertThat(registry.keys()).containsExactlyInAnyOrder("el_1", "el_2");
    }

    @Test
    void getUnknownHandleThrows() {
        ElementHandleRegistry registry = new ElementHandleRegistry();
        assertThatThrownBy(() -> registry.get("el_999"))
                .isInstanceOf(ElementHandleNotFoundException.class);
    }

    @Test
    void clearEmptiesRegistry() {
        ElementHandleRegistry registry = new ElementHandleRegistry();
        registry.put(Mockito.mock(WebElement.class));
        registry.clear();

        assertThat(registry.size()).isZero();
        assertThat(registry.keys()).isEmpty();
    }
}
