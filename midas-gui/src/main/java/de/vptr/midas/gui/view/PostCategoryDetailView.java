package de.vptr.midas.gui.view;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PostCategoryService;
import de.vptr.midas.gui.service.PostService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Detail view for a single post category. */
@Route(value = "categories", layout = MainLayout.class)
public class PostCategoryDetailView extends AbstractAsyncDataView
        implements BeforeEnterObserver, HasUrlParameter<String> {

    private static final Logger LOG = Logger.getLogger(PostCategoryDetailView.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Inject
    transient PostCategoryService categoryService;

    @Inject
    transient PostService postService;

    @Inject
    transient AuthService authService;

    @Nullable
    private String identifier;
    @Nullable
    private Grid<PostResponseDto> postGrid;
    @Nullable
    private H2 header;
    @Nullable
    private transient PostCategoryResponseDto category;

    /** Initializes PostCategoryDetailView. */
    public PostCategoryDetailView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    public void setParameter(final BeforeEvent event, final String parameter) {
        this.identifier = parameter;
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }

        if (this.identifier != null) {
            this.resolveCategoryAsync(this.identifier);
        }
    }

    private void resolveCategoryAsync(final String identifier) {
        LOG.infof("Resolving category for identifier: %s", identifier);

        this.loadDataAsync(() -> {
            try {
                final var categoryId = Long.parseLong(identifier);
                final var opt = this.categoryService.getCategoryById(categoryId);
                if (opt.isPresent()) {
                    return opt.get();
                }
            } catch (final NumberFormatException ignored) {
                LOG.debugf("Identifier '%s' is not numeric, searching by name", identifier);
            }

            final var allCategories = this.categoryService.getAllCategories();
            return allCategories.stream().filter(cat -> cat.getName().equalsIgnoreCase(identifier)).findFirst()
                    .orElse(null);
        }, resolvedCategory -> {
            if (resolvedCategory == null) {
                NotificationUtil.showError("Category not found");
                this.getUI().ifPresent(ui -> ui.navigate(PostCategoryView.class));
                return;
            }
            this.category = resolvedCategory;
            this.buildUi();
            this.loadPostsAsync();
        }, MidasErrorHandler::handleInUiContext, 30);
    }

    private void buildUi() {
        this.removeAll();

        final var backButton = new Button("← Back to Categories",
                ignore -> this.getUI().ifPresent(ui -> ui.navigate(PostCategoryView.class)));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        this.header = new H2(Objects.requireNonNull(this.category).getName());
        this.createPostGrid();

        this.add(backButton, this.header, Objects.requireNonNull(this.postGrid));
    }

    private void createPostGrid() {
        this.postGrid = new Grid<>(PostResponseDto.class, false);
        this.postGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.postGrid.setSizeFull();

        this.postGrid.addComponentColumn(post -> {
            final var titleButton = new Button(post.getTitle(),
                    ignore -> this.getUI().ifPresent(ui -> ui.navigate("posts/" + post.getId())));
            titleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            return titleButton;
        }).setHeader("Title").setFlexGrow(3);

        this.postGrid.addColumn(PostResponseDto::getUsername).setHeader("Author").setFlexGrow(1);

        this.postGrid.addColumn(post -> post.getCreated() != null ? post.getCreated().format(DATE_FORMATTER) : "")
                .setHeader("Date").setFlexGrow(1);

        this.postGrid.setPageSize(20);
    }

    private void loadPostsAsync() {
        final var cat = Objects.requireNonNull(this.category);
        LOG.infof("Loading posts for category: %s", cat.getName());

        this.loadDataAsync(() -> this.postService.getPostsByCategory(cat.getId()), posts -> {
            LOG.infof("Successfully loaded %s posts", posts.size());
            Objects.requireNonNull(this.postGrid).setItems(posts);
        }, MidasErrorHandler::handleInUiContext, 30);
    }
}
