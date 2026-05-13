package de.vptr.midas.gui.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.button.AddCommentButton;
import de.vptr.midas.gui.component.button.CreateButton;
import de.vptr.midas.gui.component.button.DeleteButton;
import de.vptr.midas.gui.component.button.EditButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.AdminPostCommentPanel;
import de.vptr.midas.gui.component.dialog.AdminPostDialog;
import de.vptr.midas.gui.component.layout.DateFilterLayout;
import de.vptr.midas.gui.component.layout.IntegerFilterLayout;
import de.vptr.midas.gui.component.layout.SearchLayout;
import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.service.PostCategoryService;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.service.PostService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Admin view for managing posts. */
@Route(value = "admin/posts", layout = AdminMainLayout.class)
public class AdminPostView extends AbstractAdminEntityView<PostResponseDto> {

    @Inject
    transient PostService postService;

    @Inject
    transient PostCategoryService postCategoryService;

    @Inject
    transient UserService userService;

    @Inject
    transient PostCommentService postCommentService;

    @Nullable
    private Button showPublishedButton;

    @Nullable
    private AdminPostDialog postDialog;
    @Nullable
    private AdminPostCommentPanel commentPanel;
    @Nullable
    private List<PostCategoryResponseDto> availableCategories;

    @Override
    protected void onBeforeEnterAuthenticated() {
        this.loadCategoriesAsync();
        this.setupLazyDataProvider();
    }

    private void loadCategoriesAsync() {
        this.loadDataAsync(() -> this.postCategoryService.getAllCategories(), categories -> {
            this.availableCategories = categories;
        }, throwable -> {
            this.availableCategories = List.of();
            MidasErrorHandler.handleInUiContext(throwable);
        }, 15);
    }

    @Override
    protected String getViewTitle() {
        return "Posts";
    }

    @Override
    protected Class<PostResponseDto> getEntityClass() {
        return PostResponseDto.class;
    }

    @Override
    protected String getEntityNameSingle() {
        return "Post";
    }

    @Override
    protected String getSearchPlaceholder() {
        return "Search by title or content...";
    }

    @Override
    protected String getSearchButtonText() {
        return "Search Posts";
    }

    @Override
    protected void initAdditionalComponents() {
        final Runnable authFailure = this.getAuthFailureRunnable();
        this.postDialog =
                new AdminPostDialog(this.postService, this.userService, this::setupLazyDataProvider, authFailure);
        this.commentPanel = new AdminPostCommentPanel(this.postCommentService, authFailure);
    }

    @Override
    protected void appendExtraFilters(final SearchLayout searchLayout, final DateFilterLayout dateFilterLayout,
            final IntegerFilterLayout userFilterLayout) {
        this.showPublishedButton =
                new Button("Show Published Only", ignore -> this.setupLazyDataProviderPublishedPosts());
        this.showPublishedButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        searchLayout.add(this.showPublishedButton, dateFilterLayout, userFilterLayout);
    }

    private void setupLazyDataProviderPublishedPosts() {
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.postService.getPublishedPosts(query.getPage(), query.getPageSize()).stream());
    }

    @Override
    protected void setupLazyDataProvider() {
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.postService.getAllPosts(query.getPage(), query.getPageSize()).stream());
    }

    @Override
    protected void executeSearch() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        this.executeAsyncSearch(query, q -> this.postService.searchPosts(q.trim()));
    }

    @Override
    protected void executeDateRangeFilter(final LocalDate startDate, final LocalDate endDate) {
        this.loadDataAsync(() -> this.postService.getPostsByDateRange(startDate, endDate),
                posts -> Objects.requireNonNull(this.grid).setItems(posts));
    }

    @Override
    protected void executeUserFilter(final Integer userId) {
        this.loadDataAsync(() -> this.postService.getPostsByUser(userId.toString()),
                posts -> Objects.requireNonNull(this.grid).setItems(posts));
    }

    @Override
    protected HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.add(
                new CreateButton(
                        ignore -> Objects.requireNonNull(this.postDialog).open(null, this.availableCategories)),
                new RefreshButton(ignore -> this.setupLazyDataProvider()));
        return layout;
    }

    @Override
    protected void configureGrid(final Grid<PostResponseDto> g) {
        g.addColumn((ValueProvider<PostResponseDto, ?>) PostResponseDto::getPublicId).setHeader("ID").setWidth("80px")
                .setFlexGrow(0);

        g.addComponentColumn(post -> {
            final var titleSpan = new Span(post.getTitle());
            titleSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            titleSpan.getStyle().set("cursor", "pointer");
            titleSpan.getStyle().set("width", "100%");
            titleSpan.getStyle().set("display", "block");
            titleSpan.addClickListener(
                    ignore -> Objects.requireNonNull(this.postDialog).open(post, this.availableCategories));
            return titleSpan;
        }).setHeader("Title").setFlexGrow(2);

        g.addColumn(post -> post.getUsername() != null ? post.getUsername() : "").setHeader("Author").setWidth("120px")
                .setFlexGrow(0);
        g.addColumn(post -> post.getCategoryName() != null ? post.getCategoryName() : "").setHeader("Category")
                .setWidth("120px").setFlexGrow(0);

        g.addComponentColumn(post -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(post.isPublished() != null ? post.isPublished() : false);
            checkbox.setReadOnly(true);
            return checkbox;
        }).setHeader("Published").setWidth("100px").setFlexGrow(0);

        g.addComponentColumn(post -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(post.isCommentable() != null ? post.isCommentable() : false);
            checkbox.setReadOnly(true);
            return checkbox;
        }).setHeader("Commentable").setWidth("100px").setFlexGrow(0);

        g.addColumn((ValueProvider<PostResponseDto, ?>) PostResponseDto::getCreated).setHeader("Created")
                .setWidth("150px").setFlexGrow(0);
        g.addColumn((ValueProvider<PostResponseDto, ?>) PostResponseDto::getLastEdit).setHeader("Last Edit")
                .setWidth("150px").setFlexGrow(0);

        g.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("200px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PostResponseDto post) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton =
                new EditButton(ignore -> Objects.requireNonNull(this.postDialog).open(post, this.availableCategories));
        final var deleteButton = new DeleteButton(ignore -> this.handleDeletion(() -> {
            this.postService.deletePost(Objects.requireNonNull(post.getPublicId()));
        }));
        final var commentButton = new AddCommentButton(ignore -> Objects.requireNonNull(this.commentPanel).open(post));

        layout.add(editButton, deleteButton, commentButton);
        return layout;
    }
}
