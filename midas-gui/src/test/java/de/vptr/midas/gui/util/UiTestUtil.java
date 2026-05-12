package de.vptr.midas.gui.util;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;

public final class UiTestUtil {

    private UiTestUtil() {
        // Utility class
    }

    public static Button findButton(final Component root, final String text) {
        return findComponent(root, Button.class, button -> text.equals(button.getText()));
    }

    public static <T extends Component> T findComponent(final Component root, final Class<T> type,
            final Predicate<T> predicate) {
        return findComponents(root, type).stream().filter(predicate).findFirst()
                .orElseThrow(() -> new AssertionError("Component not found: " + type.getSimpleName()));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Component> List<T> findComponents(final Component root, final Class<T> type) {
        final List<T> directMatches =
                root.getChildren().filter(type::isInstance).map(component -> (T) component).toList();
        final List<T> nestedMatches =
                root.getChildren().flatMap(component -> findComponents(component, type).stream()).toList();
        return Stream.concat(directMatches.stream(), nestedMatches.stream()).toList();
    }
}
