package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.PostCategoryClient;
import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.dto.PostCategoryResponseDto;

/** Covers the parent-id derivation edge cases; HTTP behaviour is pinned elsewhere. */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NullAway")
class PostCategoryServiceTest {

    @Mock
    @RestClient
    PostCategoryClient categoryClient;

    @Mock
    AuthService authService;

    @InjectMocks
    PostCategoryService categoryService;

    @Test
    void createCategory_sendsNoParent_forRootCategory() {
        when(this.categoryClient.createCategory(any(PostCategoryDto.class)))
                .thenReturn(new PostCategoryResponseDto().publicId("c1").name("Topics"));

        assertThat(this.categoryService.createCategory(new PostCategoryDto(null, "Topics", (String) null)).getParent())
                .isNull();

        final ArgumentCaptor<PostCategoryDto> sent = ArgumentCaptor.forClass(PostCategoryDto.class);
        verify(this.categoryClient).createCategory(sent.capture());
        assertThat(sent.getValue().getParentPublicId()).isNull();
    }

    @Test
    void updateCategory_keepsExplicitParentId_overParentObject() {
        final var category = new PostCategoryDto("c1", "Hardware", new PostCategoryDto("p1", "Topics", (String) null));
        category.setParentPublicId("p2");
        when(this.categoryClient.updateCategory(eq("c1"), any(PostCategoryDto.class)))
                .thenReturn(new PostCategoryResponseDto().publicId("c1").name("Hardware").parentPublicId("p2"));

        assertThat(this.categoryService.updateCategory(category).getParentPublicId()).isEqualTo("p2");

        final ArgumentCaptor<PostCategoryDto> sent = ArgumentCaptor.forClass(PostCategoryDto.class);
        verify(this.categoryClient).updateCategory(eq("c1"), sent.capture());
        assertThat(sent.getValue().getParentPublicId()).isEqualTo("p2");
    }
}
