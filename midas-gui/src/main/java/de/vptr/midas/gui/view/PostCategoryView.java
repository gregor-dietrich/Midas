package de.vptr.midas.gui.view;

import java.util.List;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PostCategoryService;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.inject.Inject;

/** View for browsing post categories. */
@Route(value = "categories", layout = MainLayout.class)
public class PostCategoryView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(PostCategoryView.class);

    @Inject
    PostCategoryService categoryService;

    @Inject
    AuthService authService;

    private TreeGrid<PostCategoryResponseDto> treeGrid;
    private List<PostCategoryResponseDto> allCategories;

    /** Initializes PostCategoryView. */
    public PostCategoryView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        try {
            this.authService.requireAuthorizationHeader();
        } catch (final AuthenticationException e) {
            LOG.debugf("Authentication required: %s", e.getMessage());
            event.forwardTo(LoginView.class);
            return;
        }

        this.buildUi();
        this.loadCategoriesAsync();
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("Categories");
        this.createTreeGrid();

        this.add(header, this.treeGrid);
    }

    private void createTreeGrid() {
        this.treeGrid = new TreeGrid<>(PostCategoryResponseDto.class);
        this.treeGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.treeGrid.setSizeFull();
        this.treeGrid.removeAllColumns();

        this.treeGrid.addHierarchyColumn(PostCategoryResponseDto::getName)
                .setHeader("Category Name")
                .setFlexGrow(3);

        this.treeGrid.addColumn(PostCategoryResponseDto::getPostsCount)
                .setHeader("Posts")
                .setFlexGrow(1);

        this.treeGrid.addItemClickListener(event -> {
            final var category = event.getItem();
            this.getUI().ifPresent(ui -> ui.navigate("categories/" + category.getId()));
        });

        this.treeGrid.setPageSize(20);
    }

    private void loadCategoriesAsync() {
        LOG.info("Starting async category loading");

        this.loadDataAsync(
                () -> this.categoryService.getAllCategories(),
                categories -> {
                    LOG.infof("Successfully loaded %s categories", categories.size());
                    this.allCategories = categories;
                    this.updateTreeGrid();
                });
    }

    private void updateTreeGrid() {
        final var rootCategories = this.allCategories.stream()
                .filter(PostCategoryResponseDto::isRootCategory)
                .toList();

        this.treeGrid.setItems(rootCategories, this::loadChildren);
        this.treeGrid.expandRecursively(rootCategories, 1);
    }

    private List<PostCategoryResponseDto> loadChildren(final PostCategoryResponseDto parent) {
        return this.allCategories.stream()
                .filter(cat -> cat.getParentId() != null && cat.getParentId().equals(parent.getId()))
                .toList();
    }
}
