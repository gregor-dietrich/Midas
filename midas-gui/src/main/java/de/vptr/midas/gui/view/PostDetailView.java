package de.vptr.midas.gui.view;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.dto.PostCommentResponseDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.service.PostService;
import de.vptr.midas.gui.util.ErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.inject.Inject;

/** Component. */
@Route(value = "posts", layout = MainLayout.class)
public class PostDetailView extends AbstractAsyncDataView
        implements HasUrlParameter<String>, BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(PostDetailView.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm");

    @Inject
    PostService postService;

    @Inject
    PostCommentService postCommentService;

    @Inject
    AuthService authService;

    private String identifier;

    /** Creates a new instance. */
    public PostDetailView() {
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

        if (this.identifier == null || this.identifier.trim().isEmpty()) {
            NotificationUtil.showError("Invalid post identifier");
            event.getUI().navigate(PostView.class);
            return;
        }

        this.removeAll();
        this.loadPostAsync();
    }

    private void loadPostAsync() {
        LOG.infof("Loading post with identifier: %s", this.identifier);

        this.loadDataAsync(() -> {
            // Try parsing as ID first
            try {
                final var id = Long.parseLong(this.identifier);
                final var post = this.postService.getPostById(id);
                if (post.isPresent()) {
                    return post.get();
                }
            } catch (final NumberFormatException e) {
                LOG.debugf("Identifier '%s' is not a numeric ID, trying search", this.identifier);
            }

            // Fallback: search by title
            final var authHeader = this.authService.getAuthorizationHeader();
            final var results = this.postService.searchPosts(this.identifier, authHeader);
            if (results != null && !results.isEmpty()) {
                return results.get(0);
            }

            return null;
        }, post -> {
            if (post == null) {
                this.showNotFound();
            } else {
                this.buildPostContent(post);
                this.loadCommentsAsync(post.getId()); // ensure this method uses loadDataAsync too
            }
        }, throwable -> {
            LOG.errorf(throwable, "Error loading post: %s", throwable.getMessage());
            ErrorHandler.handleInUiContext(throwable);
        }, 30);
    }

    private void buildPostContent(final PostResponseDto post) {
        // Back button
        final var backButton = new Button("← Back to Posts",
                _ -> this.getUI().ifPresent(ui -> ui.navigate(PostView.class)));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        // Title
        final var title = new H2(post.getTitle() != null ? post.getTitle() : "Untitled");
        title.getStyle().set("margin-bottom", "0");

        // Metadata
        final var metaLayout = new HorizontalLayout();
        metaLayout.setSpacing(true);
        metaLayout.getStyle().set("font-size", "var(--lumo-font-size-s)");
        metaLayout.getStyle().set("color", "var(--lumo-secondary-text-color)");

        if (post.getUsername() != null) {
            metaLayout.add(new Span("By " + post.getUsername()));
        }

        if (post.getCreated() != null) {
            metaLayout.add(new Span("Published " + post.getCreated().format(DATE_FORMATTER)));
        }

        if (post.getLastEdit() != null && !post.getLastEdit().equals(post.getCreated())) {
            metaLayout.add(new Span("(edited " + post.getLastEdit().format(DATE_FORMATTER) + ")"));
        }

        if (post.getCategoryName() != null) {
            final var categoryBadge = new Span(post.getCategoryName());
            categoryBadge.getStyle().set("background", "var(--lumo-primary-color-10pct)");
            categoryBadge.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
            categoryBadge.getStyle().set("padding", "2px 8px");
            metaLayout.add(categoryBadge);
        }

        // Full content
        final var contentDiv = new Div();
        contentDiv.getStyle().set("white-space", "pre-wrap");
        contentDiv.getStyle().set("line-height", "1.6");
        contentDiv.getStyle().set("padding", "var(--lumo-space-m)");
        contentDiv.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        contentDiv.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        contentDiv.getStyle().set("background", "var(--lumo-base-color)");
        contentDiv.getStyle().set("margin-top", "var(--lumo-space-m)");
        contentDiv.setText(post.getContent() != null ? post.getContent() : "");

        this.add(backButton, title, metaLayout, contentDiv);
    }

    private void loadCommentsAsync(final Long postId) {
        LOG.infof("Loading comments for post: %s", postId);

        this.loadDataAsync(
                () -> this.postCommentService.getCommentsByPost(postId),
                this::buildCommentsSection,
                this::handleCommentLoadError,
                15);
    }

    void handleCommentLoadError(final Throwable throwable) {
        LOG.errorf(throwable, "Error loading comments: %s", throwable.getMessage());
        ErrorHandler.handleInUiContext(throwable);
    }

    private void buildCommentsSection(final List<PostCommentResponseDto> comments) {
        final var commentsSection = new VerticalLayout();
        commentsSection.setPadding(false);
        commentsSection.setSpacing(true);
        commentsSection.getStyle().set("margin-top", "var(--lumo-space-l)");

        final var commentsHeader = new H3("Comments (" + comments.size() + ")");
        commentsSection.add(commentsHeader);

        if (comments.isEmpty()) {
            final var noComments = new Span("No comments yet.");
            noComments.getStyle().set("color", "var(--lumo-secondary-text-color)");
            commentsSection.add(noComments);
        } else {
            for (final var comment : comments) {
                commentsSection.add(this.createCommentCard(comment));
            }
        }

        this.add(commentsSection);
    }

    private VerticalLayout createCommentCard(final PostCommentResponseDto comment) {
        final var card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        card.getStyle().set("background", "var(--lumo-contrast-5pct)");

        // Comment header: username and date
        final var headerLayout = new HorizontalLayout();
        headerLayout.setSpacing(true);
        headerLayout.getStyle().set("font-size", "var(--lumo-font-size-s)");

        final var username = new Span(comment.getUsername() != null ? comment.getUsername() : "Anonymous");
        username.getStyle().set("font-weight", "bold");
        headerLayout.add(username);

        if (comment.getCreated() != null) {
            final var date = new Span(comment.getCreated().format(DATE_FORMATTER));
            date.getStyle().set("color", "var(--lumo-secondary-text-color)");
            headerLayout.add(date);
        }

        // Comment content
        final var content = new Span(comment.getContent() != null ? comment.getContent() : "");
        content.getStyle().set("margin-top", "var(--lumo-space-xs)");
        content.getStyle().set("white-space", "pre-wrap");

        card.add(headerLayout, content);
        return card;
    }

    private void showNotFound() {
        final var backButton = new Button("← Back to Posts",
                _ -> this.getUI().ifPresent(ui -> ui.navigate(PostView.class)));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        final var message = new H2("Post Not Found");
        final var description = new Span("The requested post could not be found.");
        description.getStyle().set("color", "var(--lumo-secondary-text-color)");

        this.add(backButton, message, description);
    }
}
