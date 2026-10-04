package de.vptr.midas.gui.view;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider.HierarchyFormat;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PostCategoryService;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Base view for post category hierarchies shared between normal and admin views. */
public abstract class AbstractPostCategoryBaseView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AbstractPostCategoryBaseView.class);

    @Inject
    protected transient PostCategoryService categoryService;

    @Inject
    protected transient AuthService authService;

    @Nullable
    protected TreeGrid<PostCategoryResponseDto> treeGrid;
    @Nullable
    protected List<PostCategoryResponseDto> allCategories;

    protected AbstractPostCategoryBaseView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        try {
            if (!this.authService.isAuthenticated()) {
                event.forwardTo(LoginView.class);
                return;
            }
        } catch (final AuthenticationException e) {
            LOG.debugf("Authentication required: %s", e.getMessage());
            event.forwardTo(LoginView.class);
            return;
        }

        this.buildUi();
        this.loadCategoriesAsync();
    }

    protected abstract void buildUi();

    protected void initBaseTreeGrid() {
        this.treeGrid = new TreeGrid<>(PostCategoryResponseDto.class);
        this.treeGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.treeGrid.setSizeFull();
        this.treeGrid.removeAllColumns();
    }

    protected void loadCategoriesAsync() {
        LOG.info("Starting async category loading");

        this.loadDataAsync(() -> this.categoryService.getAllCategories(), categories -> {
            LOG.infof("Successfully loaded %s categories", categories.size());
            this.allCategories = categories;
            this.updateTreeGrid();
        });
    }

    protected void updateTreeGrid() {
        if (this.allCategories == null) {
            return;
        }
        final var rootCategories = this.allCategories.stream()
                .filter(category -> Boolean.TRUE.equals(category.getIsRootCategory())).toList();

        final var tg = Objects.requireNonNull(this.treeGrid);
        this.setTreeItems(rootCategories, this::getChildrenOfCategory);
        tg.expandRecursively(rootCategories, this.getDefaultExpansionDepth());
    }

    /** Populate the tree grid while retaining Vaadin's nested hierarchy behavior. */
    protected void setTreeItems(final List<PostCategoryResponseDto> roots,
            final ValueProvider<PostCategoryResponseDto, Collection<PostCategoryResponseDto>> childProvider) {
        Objects.requireNonNull(this.treeGrid).setDataProvider(new TreeDataProvider<>(
                new TreeData<PostCategoryResponseDto>().addItems(roots, childProvider), HierarchyFormat.NESTED));
    }

    protected int getDefaultExpansionDepth() {
        return 1;
    }

    protected List<PostCategoryResponseDto> getChildrenOfCategory(final PostCategoryResponseDto parent) {
        if (this.allCategories == null) {
            return List.of();
        }
        return this.allCategories.stream().filter(category -> category.getParentPublicId() != null
                && category.getParentPublicId().equals(parent.getPublicId())).toList();
    }
}
