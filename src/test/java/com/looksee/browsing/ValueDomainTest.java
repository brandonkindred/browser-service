package com.looksee.browsing;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ValueDomainTest {

    @Test
    public void testDefaultConstructor() {
        ValueDomain domain = new ValueDomain();
        assertNotNull(domain.getValues());
        assertEquals(1, domain.getValues().size());
        assertEquals("", domain.getValues().get(0));
    }

    @Test
    public void testAddRandomRealNumbers() {
        ValueDomain domain = new ValueDomain();
        domain.addRandomRealNumbers();
        assertEquals(101, domain.getValues().size()); // 1 empty + 100 numbers
    }

    @Test
    public void testAddRandomDecimals() {
        ValueDomain domain = new ValueDomain();
        domain.addRandomDecimals();
        assertEquals(101, domain.getValues().size());
    }

    @Test
    public void testAddRandomAlphabeticStrings() {
        ValueDomain domain = new ValueDomain();
        domain.addRandomAlphabeticStrings();
        assertEquals(101, domain.getValues().size());
    }

    @Test
    public void testAddRandomSpecialCharacterAlphabeticStrings() {
        ValueDomain domain = new ValueDomain();
        domain.addRandomSpecialCharacterAlphabeticStrings();
        assertEquals(11, domain.getValues().size()); // 1 empty + 10 special
    }

    @Test
    public void testGenerateAllValueTypes() {
        ValueDomain domain = new ValueDomain();
        domain.generateAllValueTypes();
        // 1 + 100 + 100 + 100 + 10 = 311
        assertEquals(311, domain.getValues().size());
    }

    @Test
    public void testToString() {
        ValueDomain domain = new ValueDomain();
        assertEquals("", domain.toString());
    }

    @Test
    public void testGetAndSetValues() {
        ValueDomain domain = new ValueDomain();
        java.util.ArrayList<String> newValues = new java.util.ArrayList<>();
        newValues.add("test");
        domain.setValues(newValues);
        assertEquals(1, domain.getValues().size());
        assertEquals("test", domain.getValues().get(0));
    }

    @Test
    public void testAlphabet() {
        ValueDomain domain = new ValueDomain();
        assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", domain.getAlphabet());
    }

    @Test
    public void testSpecialCharacters() {
        ValueDomain domain = new ValueDomain();
        assertNotNull(domain.getSpecialCharacters());
        assertTrue(domain.getSpecialCharacters().contains("+"));
        assertTrue(domain.getSpecialCharacters().contains("@"));
        assertTrue(domain.getSpecialCharacters().contains("$"));
    }
}
