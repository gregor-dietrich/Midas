package de.vptr.midas.gui.view;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import de.vptr.midas.gui.dto.PostCommentResponseDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.service.Broadcaster;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.service.PostService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import de.vptr.midas.gui.util.PostUiUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** PostDetailView view. */
@Route(value = "posts", layout = MainLayout.class)
public class PostDetailView extends AbstractAuthenticatedParameterView {

    private static final Logger LOG = Logger.getLogger(PostDetailView.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm");

    @Inject
    transient PostService postService;

    @Inject
    transient PostCommentService postCommentService;

    @Inject
    transient Broadcaster broadcaster;

    @Nullable
    private Registration broadcasterRegistration;

    @Nullable
    private Long currentPostId;

    private final VerticalLayout commentsContainer = new VerticalLayout();

    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.getUI().ifPresent(ui -> {
            this.broadcasterRegistration = this.broadcaster.register(message -> {
                final var _ = ui.access(() -> {
                    if (message.startsWith("comment-created:") && this.currentPostId != null) {
                        final String postIdStr = message.substring("comment-created:".length());
                        if (postIdStr.equals(this.currentPostId.toString())) {
                            LOG.info("Received real-time update for comments, refreshing...");
                            this.loadCommentsAsync(this.currentPostId);
                        }
                    }
                });
            });
        });
    }

    @Override
    protected void onDetach(final DetachEvent detachEvent) {
        if (this.broadcasterRegistration != null) {
            this.broadcasterRegistration.remove();
            this.broadcasterRegistration = null;
        }
        super.onDetach(detachEvent);
    }

    @Override
    protected void onBeforeEnterAuthenticated(final BeforeEnterEvent event) {
        if (this.identifier == null || this.identifier.trim().isEmpty()) {
            NotificationUtil.showError("Invalid post identifier");
            event.forwardTo(PostView.class);
            return;
        }

        this.removeAll();
        this.loadPostAsync();
    }

    private void loadPostAsync() {
        final var idStr = Objects.requireNonNull(this.identifier);
        LOG.infof("Loading post with identifier: %s", idStr);

        this.loadDataAsync(() -> {
            // Try parsing as ID first
            try {
                final var id = Long.parseLong(idStr);
                final var post = this.postService.getPostById(id);
                if (post.isPresent()) {
                    return post.get();
                }
            } catch (final NumberFormatException e) {
                LOG.debugf("Identifier '%s' is not a numeric ID, trying search", idStr);
            }

            // Fallback: search by title
            final var results = this.postService.searchPosts(idStr);
            if (results != null && !results.isEmpty()) {
                return results.get(0);
            }

            return null;
        }, post -> {
            if (post == null) {
                this.showNotFound();
            } else {
                this.currentPostId = post.getId();
                this.buildPostContent(post);
                final var postId = post.getId();
                if (postId != null) {
                    this.loadCommentsAsync(postId); // ensure uses loadDataAsync
                }
            }
        }, throwable -> {
            LOG.errorf(throwable, "Error loading post: %s", throwable.getMessage());
            MidasErrorHandler.handleInUiContext(throwable);
        }, 30);
    }

    private void buildPostContent(final PostResponseDto post) {
        // Back button
        final var backButton =
                new Button("← Back to Posts", ignore -> this.getUI().ifPresent(ui -> ui.navigate(PostView.class)));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        // Title
        final var title = new H2(post.getTitle() != null ? post.getTitle() : "Untitled");
        title.getStyle().set("margin-bottom", "0");

        // Metadata
        final var metaLayout = PostUiUtil.createPostMetadataLayout(post, DATE_FORMATTER, true, false, "Published ");

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

        this.commentsContainer.setPadding(false);
        this.commentsContainer.setSpacing(true);
        this.commentsContainer.getStyle().set("margin-top", "var(--lumo-space-l)");

        this.add(backButton, title, metaLayout, contentDiv, this.commentsContainer);
    }

    private void loadCommentsAsync(final Long postId) {
        LOG.infof("Loading comments for post: %s", postId);

        this.loadDataAsync(() -> this.postCommentService.getCommentsByPost(postId), this::buildCommentsSection,
                this::handleCommentLoadError, 15);
    }

    void handleCommentLoadError(final Throwable throwable) {
        LOG.errorf(throwable, "Error loading comments: %s", throwable.getMessage());
        MidasErrorHandler.handleInUiContext(throwable);
    }

    private void buildCommentsSection(final List<PostCommentResponseDto> comments) {
        this.commentsContainer.removeAll();

        final var commentsHeader = new H3("Comments (" + comments.size() + ")");
        this.commentsContainer.add(commentsHeader);

        if (comments.isEmpty()) {
            final var noComments = new Span("No comments yet.");
            noComments.getStyle().set("color", "var(--lumo-secondary-text-color)");
            this.commentsContainer.add(noComments);
        } else {
            for (final var comment : comments) {
                this.commentsContainer.add(this.createCommentCard(comment));
            }
        }
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
        final var backButton =
                new Button("← Back to Posts", ignore -> this.getUI().ifPresent(ui -> ui.navigate(PostView.class)));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        final var message = new H2("Post Not Found");
        final var description = new Span("The requested post could not be found.");
        description.getStyle().set("color", "var(--lumo-secondary-text-color)");

        this.add(backButton, message, description);
    }
}
