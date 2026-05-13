package de.vptr.midas.gui.view;

import java.util.Objects;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.dto.PostCategoryResponseDto;

/** View for browsing post categories. */
@Route(value = "categories", layout = MainLayout.class)
public class PostCategoryView extends AbstractPostCategoryBaseView {

    @Override
    protected void buildUi() {
        this.removeAll();

        final var header = new H2("Categories");
        this.initBaseTreeGrid();
        final var tg = Objects.requireNonNull(this.treeGrid);

        tg.addHierarchyColumn(PostCategoryResponseDto::getName).setHeader("Category Name").setFlexGrow(3);
        tg.addColumn(PostCategoryResponseDto::getPostsCount).setHeader("Posts").setFlexGrow(1);

        tg.addItemClickListener(event -> {
            final var category = event.getItem();
            this.getUI().ifPresent(ui -> ui.navigate("categories/" + category.getPublicId()));
        });

        tg.setPageSize(20);

        this.add(header, tg);
    }
}
