package com.looksee.utils;

import com.looksee.models.audit.GenericIssue;
import com.looksee.services.BrowserService;
import java.util.HashSet;
import java.util.Set;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * FormValidator is a class that validates the form structure.
 */
public class FormValidator {

    /**
     * Validates that form elements are grouped correctly within fieldsets as per WCAG 2.1 Section 1.3.1.
     *
     * @param form The JSoup Element representing the form to validate.
     * @return True if the form structure is valid, false otherwise.
     *
     * precondition: form != null
     */
    public Set<GenericIssue> validateFormStructure(Element form) {
        assert form != null;
        // Select all <fieldset> elements in the form
        Elements fieldsets = form.select("fieldset");

        // Select all form controls that should be within a fieldset
        Elements controls = form.select("input, select, textarea, button");

        Set<GenericIssue> issues = new HashSet<>();
        String description = "Grouping form controls within a <fieldset> element is important for accessibility because it provides a clear, semantic structure that enhances the understanding of the form's organization, especially for users with disabilities. The <fieldset> element, often paired with a <legend>, helps screen readers and other assistive technologies to convey related groups of controls as a single, coherent unit, ensuring that users can navigate and comprehend the form's layout more effectively. ";

        // Ensure all form controls are within a fieldset
        for (Element control : controls) {
            String cssSelector = BrowserService.generateCssSelectorFromXpath(BrowserService.getXPath(control));

            if (!isWithinFieldset(control, fieldsets)) {
                String recommendation = "Identify related form controls that belong together and wrap them in a <fieldset> element. Add a <legend> element within the <fieldset> to describe the purpose of the grouped controls. This not only ensures compliance with accessibility guidelines but also improves the form's usability by providing a clear, structured organization for users, including those using assistive technologies.";
                String title = "Form control not grouped within Fieldset element";

                GenericIssue issue = new GenericIssue(description, title, cssSelector, recommendation);
                issues.add(issue);
            }
            else{
                String recommendation = "";
                String title = "Form control is grouped within Fieldset element!!";

                GenericIssue issue = new GenericIssue(description, title, cssSelector, recommendation);
                issues.add(issue);
            }
        }

        return issues; // Valid form structure
    }

    /**
     * Checks if the given control element is within any of the provided fieldsets.
     *
     * @param control The control element to check.
     * @param fieldsets The list of fieldsets to check against.
     * @return True if the control is within a fieldset, false otherwise.
     *
     * precondition: control != null
     * precondition: fieldsets != null
     */
    private boolean isWithinFieldset(Element control, Elements fieldsets) {
        assert control != null;
        assert fieldsets != null;
        for (Element fieldset : fieldsets) {
            if (fieldset.getAllElements().contains(control)) {
                return true;
            }
        }
        return false;
    }
}
