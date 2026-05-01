package de.vptr.midas.gui.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.layout.AsyncSearchLayout;
import de.vptr.midas.gui.component.layout.DateFilterLayout;
import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PostCategoryService;
import de.vptr.midas.gui.service.PostService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.inject.Inject;

/** View for browsing published posts. */
@Route(value = "posts", layout = MainLayout.class)
public class PostView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(PostView.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final int EXCERPT_LENGTH = 200;
    private static final int CATEGORY_LOAD_TIMEOUT_SECONDS = 15;

    @Inject
    PostService postService;

    @Inject
    PostCategoryService postCategoryService;

    @Inject
    AuthService authService;

    private Grid<PostResponseDto> grid;
    private AsyncSearchLayout searchLayout;
    private ComboBox<PostCategoryResponseDto> categoryFilter;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    private List<PostCategoryResponseDto> availableCategories;

    /** Initializes PostView. */
    public PostView() {
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
        this.loadPostsAsync();
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("Posts");
        final var filterLayout = this.createFilterLayout();
        this.createGrid();

        this.add(header, filterLayout, this.grid);
    }

    private HorizontalLayout createFilterLayout() {
        this.searchLayout = new AsyncSearchLayout(
                this::searchPosts,
                this::loadPostsAsync,
                "Search by title or content...",
                "Search Posts");

        // Category filter
        this.categoryFilter = new ComboBox<>("Category");
        this.categoryFilter.setPlaceholder("All categories");
        this.categoryFilter.setItemLabelGenerator(PostCategoryResponseDto::getName);
        this.categoryFilter.setClearButtonVisible(true);
        this.categoryFilter.setWidth("200px");
        this.categoryFilter.addValueChangeListener(e -> this.loadPostsAsync());

        // Date range filter
        final var dateFilterLayout = new DateFilterLayout(e -> this.loadPostsAsync());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        this.searchLayout.add(this.categoryFilter, dateFilterLayout);
        return this.searchLayout;
    }

    private void createGrid() {
        this.grid = new Grid<>(PostResponseDto.class, false);
        this.grid.setSizeFull();
        this.grid.setPageSize(20);

        // Single component column rendering each row as a card
        this.grid.addComponentColumn(this::createPostCard)
                .setHeader("")
                .setFlexGrow(1);
    }

    private VerticalLayout createPostCard(final PostResponseDto post) {
        final var card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        card.getStyle().set("cursor", "pointer");
        card.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        // Title
        final var title = new Span(post.getTitle() != null ? post.getTitle() : "Untitled");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)");
        title.getStyle().set("font-weight", "bold");
        title.getStyle().set("color", "var(--lumo-primary-text-color)");

        // Metadata row: author | date | category | comments
        final var metaLayout = new HorizontalLayout();
        metaLayout.setSpacing(true);
        metaLayout.getStyle().set("font-size", "var(--lumo-font-size-s)");
        metaLayout.getStyle().set("color", "var(--lumo-secondary-text-color)");
        metaLayout.getStyle().set("margin-top", "var(--lumo-space-xs)");

        if (post.getUsername() != null) {
            metaLayout.add(new Span("By " + post.getUsername()));
        }

        if (post.getCreated() != null) {
            metaLayout.add(new Span(post.getCreated().format(DATE_FORMATTER)));
        }

        if (post.getCategoryName() != null) {
            final var categoryBadge = new Span(post.getCategoryName());
            categoryBadge.getStyle().set("background", "var(--lumo-primary-color-10pct)");
            categoryBadge.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
            categoryBadge.getStyle().set("padding", "2px 8px");
            metaLayout.add(categoryBadge);
        }

        if (post.getCommentsCount() != null && post.getCommentsCount() > 0) {
            metaLayout.add(new Span(post.getCommentsCount() + " comments"));
        }

        // Excerpt
        final var excerpt = new Span(this.createExcerpt(post.getContent()));
        excerpt.getStyle().set("color", "var(--lumo-body-text-color)");
        excerpt.getStyle().set("margin-top", "var(--lumo-space-s)");

        card.add(title, metaLayout, excerpt);

        // Navigate to detail view on click
        card.addClickListener(e -> this.getUI().ifPresent(ui -> ui.navigate("posts/" + post.getId())));

        return card;
    }

    private String createExcerpt(final String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        if (content.length() <= EXCERPT_LENGTH) {
            return content;
        }
        return content.substring(0, EXCERPT_LENGTH) + "...";
    }

    private void loadPostsAsync() {
        LOG.info("Starting async published post loading");

        final var selectedCategory = this.categoryFilter.getValue();
        final LocalDate startDate = this.startDatePicker.getValue();
        final LocalDate endDate = this.endDatePicker.getValue();

        if (selectedCategory != null) {
            this.loadDataAsync(
                    () -> this.postService.getPostsByCategory(selectedCategory.getId()),
                    posts -> {
                        LOG.infof("Successfully loaded %s posts for category %s", posts.size(), selectedCategory.getName());
                        this.grid.setItems(this.applyDateFilter(posts, startDate, endDate));
                    });
        } else {
            this.loadDataAsync(
                    () -> this.postService.getPublishedPosts(),
                    posts -> {
                        LOG.infof("Successfully loaded %s published posts", posts.size());
                        this.grid.setItems(this.applyDateFilter(posts, startDate, endDate));
                    });
        }
    }

    private List<PostResponseDto> applyDateFilter(final List<PostResponseDto> posts,
            final LocalDate startDate, final LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return posts;
        }
        return posts.stream()
                .filter(post -> {
                    if (post.getCreated() == null) {
                        return false;
                    }
                    if (startDate != null) {
                        final LocalDateTime startDateTime = startDate.atStartOfDay();
                        if (post.getCreated().isBefore(startDateTime)) {
                            return false;
                        }
                    }
                    if (endDate != null) {
                        final LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
                        if (post.getCreated().isAfter(endDateTime)) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }

    private void loadCategoriesAsync() {
        LOG.info("Starting async category loading");

        this.loadDataAsync(
                () -> this.postCategoryService.getRootCategories(),
                categories -> {
                    LOG.infof("Successfully loaded %s categories", categories.size());
                    this.availableCategories = categories;
                    this.categoryFilter.setItems(this.availableCategories);
                },
                throwable -> {
                    LOG.error("Error loading categories", throwable);
                    this.availableCategories = List.of();
                    MidasErrorHandler.handleInUiContext(throwable);
                },
                CATEGORY_LOAD_TIMEOUT_SECONDS);
    }

    private void searchPosts(final String query) {
        this.loadDataAsync(
                () -> this.postService.searchPosts(query),
                posts -> {
                    final var selectedCategory = this.categoryFilter.getValue();
                    var filtered = posts;
                    if (selectedCategory != null) {
                        final Long categoryId = selectedCategory.getId();
                        filtered = posts.stream()
                                .filter(post -> post.getCategoryId() != null && post.getCategoryId().equals(categoryId))
                                .toList();
                    }
                    final LocalDate startDate = this.startDatePicker.getValue();
                    final LocalDate endDate = this.endDatePicker.getValue();
                    this.grid.setItems(this.applyDateFilter(filtered, startDate, endDate));
                });
    }
}
