package de.vptr.midas.gui.view;

import java.time.LocalDate;
import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
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

import de.vptr.midas.gui.component.button.DeleteButton;
import de.vptr.midas.gui.component.button.EditButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.AdminPostCommentDialog;
import de.vptr.midas.gui.component.layout.DateFilterLayout;
import de.vptr.midas.gui.component.layout.IntegerFilterLayout;
import de.vptr.midas.gui.component.layout.SearchLayout;
import de.vptr.midas.gui.dto.PostCommentResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** AdminPostCommentView view. */
@Route(value = "admin/comments", layout = AdminMainLayout.class)
public class AdminPostCommentView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AdminPostCommentView.class);

    @Inject
    transient PostCommentService commentService;

    @Inject
    transient AuthService authService;

    @Nullable
    private Grid<PostCommentResponseDto> grid;
    @Nullable
    private TextField searchField;
    @Nullable
    private Button searchButton;
    @Nullable
    private DatePicker startDatePicker;
    @Nullable
    private DatePicker endDatePicker;
    @Nullable
    private IntegerField userIdField;

    @Nullable
    private AdminPostCommentDialog commentDialog;

    // CPD-OFF
    /** Initializes AdminPostCommentView. */
    public AdminPostCommentView() {
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
        this.setupLazyDataProvider();
    }
    // CPD-ON

    private void setupLazyDataProvider() {
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.commentService.getAllComments(query.getPage(), query.getPageSize()).stream());
    }

    // CPD-OFF
    private void buildUi() {
        this.removeAll();

        final var header = new H2("Post Comments");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.commentDialog = new AdminPostCommentDialog(this.commentService, this::setupLazyDataProvider,
                () -> this.getUI().ifPresent(ui -> ui.navigate(LoginView.class)));

        this.add(header, searchLayout, buttonLayout, Objects.requireNonNull(this.grid));
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(e -> {
            if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                this.setupLazyDataProvider();
            }
        }, e -> this.searchComments(), "Search by author or content...", "Search Comments");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        // Date range filter
        final var dateFilterLayout = new DateFilterLayout(e -> this.filterByDateRange());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        // User ID filter
        final var userFilterLayout =
                new IntegerFilterLayout(e -> this.filterByUser(), "Enter User ID...", "Filter by User");
        this.userIdField = userFilterLayout.getIntegerField();

        searchLayout.add(dateFilterLayout, userFilterLayout);
        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var refreshButton = new RefreshButton(e -> this.setupLazyDataProvider());

        layout.add(refreshButton);
        return layout;
    }
    // CPD-ON

    private void createGrid() {
        this.grid = new Grid<>(PostCommentResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn((ValueProvider<PostCommentResponseDto, ?>) PostCommentResponseDto::getId).setHeader("ID")
                .setWidth("80px").setFlexGrow(0);

        // Post title column
        this.grid.addComponentColumn(comment -> {
            final var title = comment.getPostTitle() != null ? comment.getPostTitle() : "(No title)";
            final var titleSpan = new Span(title);
            titleSpan.getStyle().set("color", "var(--lumo-contrast-70pct)");
            titleSpan.getStyle().set("font-weight", "500");
            return titleSpan;
        }).setHeader("Post").setWidth("200px").setFlexGrow(1);

        // Author column
        this.grid.addColumn(comment -> comment.getUsername() != null ? comment.getUsername() : "(Unknown)")
                .setHeader("Author").setWidth("120px").setFlexGrow(0);

        // Content column with limited display
        this.grid.addComponentColumn(comment -> {
            final var content = comment.getContent() != null ? comment.getContent() : "";
            final var truncated = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            final var span = new Span(truncated);
            span.setTitle(content); // Show full content on hover
            return span;
        }).setHeader("Content").setFlexGrow(2);

        this.grid.addColumn((ValueProvider<PostCommentResponseDto, ?>) PostCommentResponseDto::getCreated)
                .setHeader("Created").setWidth("150px").setFlexGrow(0);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PostCommentResponseDto comment) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton =
                new EditButton(e -> Objects.requireNonNull(this.commentDialog).open(comment.toPostCommentDto()));
        final var deleteButton = new DeleteButton(e -> this.deleteComment(comment));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void deleteComment(final PostCommentResponseDto comment) {
        try {
            this.commentService.deleteComment(Objects.requireNonNull(comment.getId()));
            NotificationUtil.showSuccess("Comment deleted successfully");
            this.setupLazyDataProvider();
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Deleting comment failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error deleting comment", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchComments() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        if (query == null || query.trim().isEmpty()) {
            this.setupLazyDataProvider();
            return;
        }

        final var sb = Objects.requireNonNull(this.searchButton);
        sb.setEnabled(false);
        LOG.infof("Starting async comment search with query: %s", query);

        this.loadDataAsync(() -> this.commentService.searchComments(query.trim()), comments -> {
            sb.setEnabled(true);
            LOG.infof("Successfully found %s comments", comments.size());
            Objects.requireNonNull(this.grid).setItems(comments);
        }, throwable -> {
            sb.setEnabled(true);
            MidasErrorHandler.handleInUiContext(throwable);
        });
    }

    // CPD-OFF
    private void filterByDateRange() {
        final LocalDate startDate = Objects.requireNonNull(this.startDatePicker).getValue();
        final LocalDate endDate = Objects.requireNonNull(this.endDatePicker).getValue();

        if (startDate == null || endDate == null) {
            NotificationUtil.showWarning("Please select both start and end dates");
            return;
        }

        if (startDate.isAfter(endDate)) {
            NotificationUtil.showWarning("Start date must be before end date");
            return;
        }

        this.loadDataAsync(() -> this.commentService.getCommentsByDateRange(startDate.toString(), endDate.toString()),
                comments -> Objects.requireNonNull(this.grid).setItems(comments));
    }
    // CPD-ON

    private void filterByUser() {
        final Integer userId = Objects.requireNonNull(this.userIdField).getValue();
        if (userId == null) {
            NotificationUtil.showWarning("Please enter a user ID");
            return;
        }

        this.loadDataAsync(() -> this.commentService.getCommentsByUser(userId.longValue()),
                comments -> Objects.requireNonNull(this.grid).setItems(comments));
    }
}
