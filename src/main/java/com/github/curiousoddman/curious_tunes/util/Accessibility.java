package com.github.curiousoddman.curious_tunes.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;

/**
 * Screen readers need a name for a button that shows only an icon. The tooltip already says what the button does,
 * so it is used as the name.
 */
public final class Accessibility {
    private Accessibility() {
    }

    /**
     * Gives every button and label under the node that shows only an icon (and has no name yet) the text of its tooltip.
     */
    public static void nameIconControls(Node node) {
        if (node instanceof Labeled labeled) {
            nameFromTooltip(labeled);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                nameIconControls(child);
            }
        }
    }

    public static void nameFromTooltip(Labeled control) {
        boolean noText = control.getText() == null || control.getText()
                                                             .isBlank();
        if (!noText || control.getGraphic() == null || control.getAccessibleText() != null) {
            return;
        }
        Tooltip tooltip = ((Control) control).getTooltip();
        if (tooltip != null && tooltip.getText() != null && !tooltip.getText()
                                                                    .isBlank()) {
            control.setAccessibleText(tooltip.getText()
                                             .lines()
                                             .findFirst()
                                             .orElse(""));
        }
    }
}
