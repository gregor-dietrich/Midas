package de.vptr.midas.gui.view;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

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
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PostCategoryService;
import de.vptr.midas.gui.service.PostService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.PostUiUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** View for browsing published posts. */
@Route(value = "posts", layout = MainLayout.class)
public class PostView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(PostView.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final int EXCERPT_LENGTH = 200;
    private static final int CATEGORY_LOAD_TIMEOUT_SECONDS = 15;

    @Inject
    transient PostService postService;

    @Inject
    transient PostCategoryService postCategoryService;

    @Inject
    transient AuthService authService;

    @Nullable
    private Grid<PostResponseDto> grid;
    @Nullable
    private AsyncSearchLayout searchLayout;
    @Nullable
    private ComboBox<PostCategoryResponseDto> categoryFilter;
    @Nullable
    private DatePicker startDatePicker;
    @Nullable
    private DatePicker endDatePicker;
    @Nullable
    private String searchQuery; // Current free-text filter, applied server-side

    @Nullable
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
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("Posts");
        final var filterLayout = this.createFilterLayout();
        this.createGrid();

        this.add(header, filterLayout, Objects.requireNonNull(this.grid));
    }

    private HorizontalLayout createFilterLayout() {
        this.searchLayout = new AsyncSearchLayout(query -> {
            this.searchQuery = (query == null || query.trim().isEmpty()) ? null : query.trim();
            this.refreshPosts();
        }, () -> {
            this.searchQuery = null;
            this.refreshPosts();
        }, "Search by title or content...", "Search Posts");

        // Category filter
        this.categoryFilter = new ComboBox<>("Category");
        this.categoryFilter.setPlaceholder("All categories");
        this.categoryFilter.setItemLabelGenerator(PostCategoryResponseDto::getName);
        this.categoryFilter.setClearButtonVisible(true);
        this.categoryFilter.setWidth("200px");
        this.categoryFilter.addValueChangeListener(ignore -> this.refreshPosts());

        // Date range filter
        final var dateFilterLayout = new DateFilterLayout(ignore -> this.refreshPosts());
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
        this.grid.addComponentColumn(this::createPostCard).setHeader("").setFlexGrow(1);

        // Lazy, server-paginated data provider: category, date range and free-text
        // filtering are all applied server-side, so the full post set is never loaded.
        this.grid
                .setItems(
                        query -> this.postService
                                .getPublishedPosts(this.selectedCategoryId(), this.selectedStartDate(),
                                        this.selectedEndDate(), this.searchQuery, query.getPage(), query.getPageSize())
                                .stream());
    }

    /** Triggers a server round-trip to reload the current page of posts with the active filters. */
    private void refreshPosts() {
        final var g = this.grid;
        if (g != null) {
            g.getDataProvider().refreshAll();
        }
    }

    @Nullable
    private String selectedCategoryId() {
        final var combo = this.categoryFilter;
        if (combo == null || combo.getValue() == null) {
            return null;
        }
        return combo.getValue().getPublicId();
    }

    @Nullable
    private LocalDate selectedStartDate() {
        return this.startDatePicker != null ? this.startDatePicker.getValue() : null;
    }

    @Nullable
    private LocalDate selectedEndDate() {
        return this.endDatePicker != null ? this.endDatePicker.getValue() : null;
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
        final var metaLayout = PostUiUtil.createPostMetadataLayout(post, DATE_FORMATTER, false, true, "");
        metaLayout.getStyle().set("margin-top", "var(--lumo-space-xs)");

        // Excerpt
        final var excerpt = new Span(this.createExcerpt(post.getContent()));
        excerpt.getStyle().set("color", "var(--lumo-body-text-color)");
        excerpt.getStyle().set("margin-top", "var(--lumo-space-s)");

        card.add(title, metaLayout, excerpt);

        // Navigate to detail view on click
        card.addClickListener(ignore -> this.getUI().ifPresent(ui -> ui.navigate("posts/" + post.getPublicId())));

        return card;
    }

    private String createExcerpt(@Nullable final String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        if (content.length() <= EXCERPT_LENGTH) {
            return content;
        }
        return content.substring(0, EXCERPT_LENGTH) + "...";
    }

    private void loadCategoriesAsync() {
        LOG.info("Starting async category loading");

        this.loadDataAsync(() -> this.postCategoryService.getRootCategories(), categories -> {
            LOG.infof("Successfully loaded %s categories", categories.size());
            this.availableCategories = categories;
            Objects.requireNonNull(this.categoryFilter).setItems(this.availableCategories);
        }, throwable -> {
            LOG.error("Error loading categories", throwable);
            this.availableCategories = List.of();
            MidasErrorHandler.handleInUiContext(throwable);
        }, CATEGORY_LOAD_TIMEOUT_SECONDS);
    }
}
