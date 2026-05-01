package de.vptr.midas.gui.view;

import java.time.LocalDate;
import java.util.List;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
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
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PostCategoryService;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.service.PostService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.ErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.inject.Inject;

/** Admin view for managing posts. */
@Route(value = "admin/posts", layout = AdminMainLayout.class)
public class AdminPostView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AdminPostView.class);

    @Inject
    PostService postService;

    @Inject
    PostCategoryService postCategoryService;

    @Inject
    AuthService authService;

    @Inject
    UserService userService;

    @Inject
    PostCommentService postCommentService;

    private Grid<PostResponseDto> grid;
    private TextField searchField;
    private Button searchButton;
    private Button showPublishedButton;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private IntegerField userIdField;

    private AdminPostDialog postDialog;
    private AdminPostCommentPanel commentPanel;
    private List<PostCategoryResponseDto> availableCategories;

    /** Initializes AdminPostView. */
    public AdminPostView() {
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
        this.setupLazyDataProviderAllPosts();
    }

    private void setupLazyDataProviderAllPosts() {
        this.grid.setItems(query -> this.postService.getAllPosts(query.getPage(), query.getPageSize()).stream());
    }

    private void setupLazyDataProviderPublishedPosts() {
        this.grid.setItems(
                query -> this.postService.getPublishedPosts(query.getPage(), query.getPageSize()).stream());
    }

    private void loadCategoriesAsync() {
        LOG.info("Starting async category loading");

        this.loadDataAsync(
                () -> this.postCategoryService.getAllCategories(),
                categories -> {
                    LOG.infof("Successfully loaded %s categories", categories.size());
                    this.availableCategories = categories;
                },
                throwable -> {
                    LOG.error("Error loading categories", throwable);
                    this.availableCategories = List.of();
                    ErrorHandler.handleInUiContext(throwable);
                },
                15);
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("Posts");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();

        final Runnable authFailure = () -> this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        this.postDialog = new AdminPostDialog(
                this.postService, this.userService, this::setupLazyDataProviderAllPosts, authFailure);
        this.commentPanel = new AdminPostCommentPanel(
                this.postCommentService, authFailure);

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                        if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                            this.setupLazyDataProviderAllPosts();
                        }
                },
                _ -> this.searchPosts(),
                "Search by title or content...",
                "Search Posts");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        this.showPublishedButton = new Button("Show Published Only",
                _ -> this.setupLazyDataProviderPublishedPosts());
        this.showPublishedButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        // Date range filter
        final var dateFilterLayout = new DateFilterLayout(_ -> this.filterByDateRange());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        // User ID filter
        final var userFilterLayout = new IntegerFilterLayout(
                _ -> this.filterByUser(),
                "Enter User ID...",
                "Filter by User");
        this.userIdField = userFilterLayout.getIntegerField();

        searchLayout.add(this.showPublishedButton, dateFilterLayout, userFilterLayout);
        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(_ -> this.postDialog.open(null, this.availableCategories));
        final var refreshButton = new RefreshButton(_ -> this.setupLazyDataProviderAllPosts());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(PostResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn((ValueProvider<PostResponseDto, ?>) PostResponseDto::getId).setHeader("ID").setWidth("80px")
                .setFlexGrow(0);

        // Make the title column clickable
        this.grid.addComponentColumn(post -> {
            final var titleSpan = new Span(post.getTitle());
            titleSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            titleSpan.getStyle().set("cursor", "pointer");
            titleSpan.getStyle().set("width", "100%");
            titleSpan.getStyle().set("display", "block");
            titleSpan.addClickListener(_ -> this.postDialog.open(post, this.availableCategories));
            return titleSpan;
        }).setHeader("Title").setFlexGrow(2);

        this.grid.addColumn(post -> post.getUsername() != null ? post.getUsername() : "").setHeader("Author")
                .setWidth("120px")
                .setFlexGrow(0);
        this.grid.addColumn(post -> post.getCategoryName() != null ? post.getCategoryName() : "").setHeader("Category")
                .setWidth("120px").setFlexGrow(0);

        this.grid.addComponentColumn(post -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(post.getPublished() != null ? post.getPublished() : false);
            checkbox.setReadOnly(true);
            return checkbox;
        }).setHeader("Published").setWidth("100px").setFlexGrow(0);

        this.grid.addComponentColumn(post -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(post.getCommentable() != null ? post.getCommentable() : false);
            checkbox.setReadOnly(true);
            return checkbox;
        }).setHeader("Commentable").setWidth("100px").setFlexGrow(0);

        this.grid.addColumn((ValueProvider<PostResponseDto, ?>) PostResponseDto::getCreated).setHeader("Created")
                .setWidth("150px").setFlexGrow(0);
        this.grid.addColumn((ValueProvider<PostResponseDto, ?>) PostResponseDto::getLastEdit).setHeader("Last Edit")
                .setWidth("150px").setFlexGrow(0);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("200px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PostResponseDto post) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(_ -> this.postDialog.open(post, this.availableCategories));
        final var deleteButton = new DeleteButton(_ -> this.deletePost(post));
        final var commentButton = new AddCommentButton(_ -> this.commentPanel.open(post));

        layout.add(editButton, deleteButton, commentButton);
        return layout;
    }

    private void deletePost(final PostResponseDto post) {
        try {
            this.postService.deletePost(post.getId());
            NotificationUtil.showSuccess("Post deleted successfully");
            this.setupLazyDataProviderAllPosts();
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Deleting post failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error deleting post", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchPosts() {
        final String query = this.searchField.getValue();
        if (query == null || query.trim().isEmpty()) {
            NotificationUtil.showWarning("Please enter a search query");
            return;
        }
        this.searchButton.setEnabled(false);
        this.searchButton.setText("Searching...");

        this.loadDataAsync(
                () -> this.postService.searchPosts(query.trim()),
                posts -> {
                    this.searchButton.setEnabled(true);
                    this.searchButton.setText("Search");
                    this.grid.setItems(posts);
                },
                throwable -> {
                    this.searchButton.setEnabled(true);
                    this.searchButton.setText("Search");
                    ErrorHandler.handleInUiContext(throwable);
                });
    }

    private void filterByDateRange() {
        final LocalDate startDate = this.startDatePicker.getValue();
        final LocalDate endDate = this.endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            NotificationUtil.showWarning("Please select both start and end dates");
            return;
        }

        if (startDate.isAfter(endDate)) {
            NotificationUtil.showWarning("Start date must be before end date");
            return;
        }

        this.loadDataAsync(
                () -> this.postService.getPostsByDateRange(startDate.toString(), endDate.toString()),
                posts -> this.grid.setItems(posts));
    }

    private void filterByUser() {
        final Integer userId = this.userIdField.getValue();
        if (userId == null) {
            NotificationUtil.showWarning("Please enter a user ID");
            return;
        }

        this.loadDataAsync(
                () -> this.postService.getPostsByUser(userId.longValue()),
                posts -> this.grid.setItems(posts));
    }
}
