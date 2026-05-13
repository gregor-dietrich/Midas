package de.vptr.midas.gui.view;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.ValueProvider;
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
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;

/** AdminPostCategoryView view. */
@Route(value = "admin/categories", layout = AdminMainLayout.class)
public class AdminPostCategoryView extends AbstractPostCategoryBaseView {

    private static final Logger LOG = Logger.getLogger(AdminPostCategoryView.class);

    @Nullable
    private TextField searchField;
    @Nullable
    private Button searchButton;

    @Nullable
    private AdminPostCategoryDialog categoryDialog;

    @Override
    protected int getDefaultExpansionDepth() {
        return 2;
    }

    @Override
    protected void buildUi() {
        this.removeAll();

        final var header = new H2("Post Categories");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createTreeGridColumns();
        this.categoryDialog = new AdminPostCategoryDialog(this.categoryService, this::loadCategoriesAsync,
                () -> this.getUI().ifPresent(ui -> ui.navigate(LoginView.class)));

        this.add(header, searchLayout, buttonLayout, Objects.requireNonNull(this.treeGrid));
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(e -> {
            if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                this.updateTreeGrid();
            }
        }, ignore -> this.searchCategories(), "Search by name...", "Search Categories");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(ignore -> Objects.requireNonNull(this.categoryDialog).open(null,
                this.allCategories != null ? this.allCategories : List.of()));
        final var refreshButton = new RefreshButton(ignore -> this.loadCategoriesAsync());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createTreeGridColumns() {
        this.initBaseTreeGrid();
        final var tg = Objects.requireNonNull(this.treeGrid);

        tg.addHierarchyColumn((ValueProvider<PostCategoryResponseDto, ?>) PostCategoryResponseDto::getName)
                .setHeader("Category Name").setFlexGrow(3);

        tg.addColumn(category -> category.getParentName() != null ? category.getParentName() : "N/A")
                .setHeader("Parent").setFlexGrow(2);

        tg.addColumn((ValueProvider<PostCategoryResponseDto, ?>) PostCategoryResponseDto::getChildrenCount)
                .setHeader("Children").setFlexGrow(1);

        tg.addColumn((ValueProvider<PostCategoryResponseDto, ?>) PostCategoryResponseDto::getPostsCount)
                .setHeader("Posts").setFlexGrow(1);

        tg.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PostCategoryResponseDto category) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(ignore -> Objects.requireNonNull(this.categoryDialog)
                .open(category.toPostCategoryDto(), this.allCategories != null ? this.allCategories : List.of()));
        final var deleteButton = new DeleteButton(ignore -> this.deleteCategory(category));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void deleteCategory(final PostCategoryResponseDto category) {
        if (this.allCategories == null) {
            return;
        }
        final boolean hasChildren = this.allCategories.stream().anyMatch(
                cat -> cat.getParentPublicId() != null && cat.getParentPublicId().equals(category.getPublicId()));

        if (hasChildren) {
            NotificationUtil
                    .showError("Cannot delete category with subcategories. Please delete or move subcategories first.");
            return;
        }

        try {
            if (this.categoryService.deleteCategory(category.getPublicId())) {
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
        final String query = Objects.requireNonNull(this.searchField).getValue();
        if (query == null || query.trim().isEmpty()) {
            this.updateTreeGrid();
            return;
        }

        final var sb = Objects.requireNonNull(this.searchButton);
        sb.setEnabled(false);

        this.loadDataAsync(() -> this.categoryService.searchCategories(query.trim()), categories -> {
            sb.setEnabled(true);
            this.updateSearchTreeGrid(categories);
        }, throwable -> {
            sb.setEnabled(true);
            MidasErrorHandler.handleInUiContext(throwable);
        });
    }

    private void updateSearchTreeGrid(final List<PostCategoryResponseDto> searchResults) {
        final var categoryIdsInResults =
                searchResults.stream().map(PostCategoryResponseDto::getPublicId).collect(Collectors.toSet());

        final var topLevelCategories = searchResults.stream().filter(
                cat -> cat.getParentPublicId() == null || !categoryIdsInResults.contains(cat.getParentPublicId()))
                .toList();

        final var tg = Objects.requireNonNull(this.treeGrid);
        tg.setItems(topLevelCategories, category -> searchResults.stream().filter(
                cat -> cat.getParentPublicId() != null && cat.getParentPublicId().equals(category.getPublicId()))
                .toList());

        tg.expandRecursively(topLevelCategories, 10);
    }
}
