package de.vptr.midas.gui.view;

import java.time.LocalDate;
import java.util.Objects;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.button.DeleteButton;
import de.vptr.midas.gui.component.button.EditButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.AdminPostCommentDialog;
import de.vptr.midas.gui.dto.PostCommentResponseDto;
import de.vptr.midas.gui.service.PostCommentService;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** AdminPostCommentView view. */
@Route(value = "admin/comments", layout = AdminMainLayout.class)
public class AdminPostCommentView extends AbstractAdminEntityView<PostCommentResponseDto> {

    @Inject
    transient PostCommentService commentService;

    @Nullable
    private AdminPostCommentDialog commentDialog;

    @Override
    protected void onBeforeEnterAuthenticated() {
        this.setupLazyDataProvider();
    }

    @Override
    protected String getViewTitle() {
        return "Post Comments";
    }

    @Override
    protected Class<PostCommentResponseDto> getEntityClass() {
        return PostCommentResponseDto.class;
    }

    @Override
    protected String getEntityNameSingle() {
        return "Comment";
    }

    @Override
    protected String getSearchPlaceholder() {
        return "Search by author or content...";
    }

    @Override
    protected String getSearchButtonText() {
        return "Search Comments";
    }

    @Override
    protected void initAdditionalComponents() {
        this.commentDialog = new AdminPostCommentDialog(this.commentService, this::setupLazyDataProvider,
                this.getAuthFailureRunnable());
    }

    @Override
    protected void setupLazyDataProvider() {
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.commentService.getAllComments(query.getPage(), query.getPageSize()).stream());
    }

    @Override
    protected void executeSearch() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        this.executeAsyncSearch(query, q -> this.commentService.searchComments(q.trim()));
    }

    @Override
    protected void executeDateRangeFilter(final LocalDate startDate, final LocalDate endDate) {
        this.loadDataAsync(() -> this.commentService.getCommentsByDateRange(startDate.toString(), endDate.toString()),
                comments -> Objects.requireNonNull(this.grid).setItems(comments));
    }

    @Override
    protected void executeUserFilter(final Integer userId) {
        this.loadDataAsync(() -> this.commentService.getCommentsByUser(userId.longValue()),
                comments -> Objects.requireNonNull(this.grid).setItems(comments));
    }

    @Override
    protected HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.add(new RefreshButton(ignore -> this.setupLazyDataProvider()));
        return layout;
    }

    @Override
    protected void configureGrid(final Grid<PostCommentResponseDto> g) {
        g.addColumn((ValueProvider<PostCommentResponseDto, ?>) PostCommentResponseDto::getId).setHeader("ID")
                .setWidth("80px").setFlexGrow(0);

        g.addComponentColumn(comment -> {
            final var title = comment.getPostTitle() != null ? comment.getPostTitle() : "(No title)";
            final var titleSpan = new Span(title);
            titleSpan.getStyle().set("color", "var(--lumo-contrast-70pct)");
            titleSpan.getStyle().set("font-weight", "500");
            return titleSpan;
        }).setHeader("Post").setWidth("200px").setFlexGrow(1);

        g.addColumn(comment -> comment.getUsername() != null ? comment.getUsername() : "(Unknown)").setHeader("Author")
                .setWidth("120px").setFlexGrow(0);

        g.addComponentColumn(comment -> {
            final var content = comment.getContent() != null ? comment.getContent() : "";
            final var truncated = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            final var span = new Span(truncated);
            span.setTitle(content);
            return span;
        }).setHeader("Content").setFlexGrow(2);

        g.addColumn((ValueProvider<PostCommentResponseDto, ?>) PostCommentResponseDto::getCreated).setHeader("Created")
                .setWidth("150px").setFlexGrow(0);

        g.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PostCommentResponseDto comment) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton =
                new EditButton(ignore -> Objects.requireNonNull(this.commentDialog).open(comment.toPostCommentDto()));
        final var deleteButton = new DeleteButton(ignore -> this.handleDeletion(() -> {
            this.commentService.deleteComment(Objects.requireNonNull(comment.getId()));
        }));

        layout.add(editButton, deleteButton);
        return layout;
    }
}
