package de.vptr.midas.gui.view;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.button.CreateButton;
import de.vptr.midas.gui.component.button.DeleteButton;
import de.vptr.midas.gui.component.button.EditButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.AdminPostCategoryDialog;
import de.vptr.midas.gui.component.layout.SearchLayout;
import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PostCategoryService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.inject.Inject;

/** AdminPostCategoryView view. */
@Route(value = "admin/categories", layout = AdminMainLayout.class)
public class AdminPostCategoryView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AdminPostCategoryView.class);

    @Inject
    PostCategoryService categoryService;

    @Inject
    AuthService authService;

    private TreeGrid<PostCategoryResponseDto> treeGrid;
    private TextField searchField;
    private Button searchButton;
    private List<PostCategoryResponseDto> allCategories;

    private AdminPostCategoryDialog categoryDialog;

    /** Initializes AdminPostCategoryView. */
    public AdminPostCategoryView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }

        this.buildUi();
        this.loadCategoriesAsync();
    }

    private void loadCategoriesAsync() {
        LOG.info("Starting async category loading");

        this.loadDataAsync(() -> this.categoryService.getAllCategories(), categories -> {
            LOG.infof("Successfully loaded %s categories", categories.size());
            this.allCategories = categories;
            this.updateTreeGrid();
        });
    }

    private void updateTreeGrid() {
        // Find root categories (categories without parent)
        final var rootCategories = this.allCategories.stream().filter(PostCategoryResponseDto::isRootCategory).toList();

        this.treeGrid.setItems(rootCategories, this::getChildrenOfCategory);
        this.treeGrid.expandRecursively(rootCategories, 2); // Expand up to 2 levels
    }

    private void updateSearchTreeGrid(final List<PostCategoryResponseDto> searchResults) {
        // For search results, we want to show all matching categories
        // If a matching category has a parent that's not in the search results,
        // we show it as a top-level item for better visibility

        // Get all category IDs that exist in the search results
        final var categoryIdsInResults =
                searchResults.stream().map(PostCategoryResponseDto::getId).collect(Collectors.toSet());

        // Find categories to show at the top level:
        // 1. Root categories (no parent)
        // 2. Categories whose parent is not in the search results (orphaned in this
        // context)
        final var topLevelCategories = searchResults.stream()
                .filter(cat -> cat.getParentId() == null || !categoryIdsInResults.contains(cat.getParentId())).toList();

        this.treeGrid.setItems(topLevelCategories, category -> searchResults.stream()
                .filter(cat -> cat.getParentId() != null && cat.getParentId().equals(category.getId())).toList());

        // Expand all search results for better visibility
        this.treeGrid.expandRecursively(topLevelCategories, 10);
    }

    private List<PostCategoryResponseDto> getChildrenOfCategory(final PostCategoryResponseDto parent) {
        return this.allCategories.stream()
                .filter(category -> category.getParentId() != null && category.getParentId().equals(parent.getId()))
                .toList();
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("Post Categories");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createTreeGrid();
        this.categoryDialog = new AdminPostCategoryDialog(this.categoryService, this::loadCategoriesAsync,
                () -> this.getUI().ifPresent(ui -> ui.navigate(LoginView.class)));

        this.add(header, searchLayout, buttonLayout, this.treeGrid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(e -> {
            if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                this.updateTreeGrid();
            }
        }, _ -> this.searchCategories(), "Search by name...", "Search Categories");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(_ -> this.categoryDialog.open(null, this.allCategories));
        final var refreshButton = new RefreshButton(_ -> this.loadCategoriesAsync());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createTreeGrid() {
        this.treeGrid = new TreeGrid<>(PostCategoryResponseDto.class);
        this.treeGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.treeGrid.setSizeFull();
        this.treeGrid.removeAllColumns();

        // Configure columns
        this.treeGrid.addHierarchyColumn((ValueProvider<PostCategoryResponseDto, ?>) PostCategoryResponseDto::getName)
                .setHeader("Category Name").setFlexGrow(3);

        this.treeGrid.addColumn(category -> category.getParentName() != null ? category.getParentName() : "N/A")
                .setHeader("Parent").setFlexGrow(2);

        this.treeGrid.addColumn((ValueProvider<PostCategoryResponseDto, ?>) PostCategoryResponseDto::getChildrenCount)
                .setHeader("Children").setFlexGrow(1);

        this.treeGrid.addColumn((ValueProvider<PostCategoryResponseDto, ?>) PostCategoryResponseDto::getPostsCount)
                .setHeader("Posts").setFlexGrow(1);

        // Add action column
        this.treeGrid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px")
                .setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PostCategoryResponseDto category) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton =
                new EditButton(_ -> this.categoryDialog.open(category.toPostCategoryDto(), this.allCategories));
        final var deleteButton = new DeleteButton(_ -> this.deleteCategory(category));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void deleteCategory(final PostCategoryResponseDto category) {
        // Check if category has children
        final boolean hasChildren = this.allCategories.stream()
                .anyMatch(cat -> cat.getParentId() != null && cat.getParentId().equals(category.getId()));

        if (hasChildren) {
            NotificationUtil
                    .showError("Cannot delete category with subcategories. Please delete or move subcategories first.");
            return;
        }

        try {
            if (this.categoryService.deleteCategory(category.getId())) {
                NotificationUtil.showSuccess("Category deleted successfully");
                this.loadCategoriesAsync();
            } else {
                NotificationUtil.showError("Failed to delete category");
            }
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Deleting category failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error deleting category", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchCategories() {
        final String query = this.searchField.getValue();
        if (query == null || query.trim().isEmpty()) {
            this.updateTreeGrid();
            return;
        }

        this.searchButton.setEnabled(false);

        this.loadDataAsync(() -> this.categoryService.searchCategories(query.trim()), categories -> {
            this.searchButton.setEnabled(true);
            this.updateSearchTreeGrid(categories);
        }, throwable -> {
            this.searchButton.setEnabled(true);
            MidasErrorHandler.handleInUiContext(throwable);
        });
    }
}
