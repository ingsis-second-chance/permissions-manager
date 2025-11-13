package ingsis.permissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import ingsis.permissions.DTO.Error;

import ingsis.permissions.DTO.Response;
import ingsis.permissions.DTO.ShareSnippetDTO;
import ingsis.permissions.Services.SnippetPermissionService;
import ingsis.permissions.controllers.SnippetPermissionController;
import ingsis.permissions.entities.GrantType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;


@ActiveProfiles("test")
@MockitoSettings(strictness = Strictness.LENIENT)
@Import(TestSecurityConfig.class)
@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class SnippetPermissionControllerTest {
    @Autowired
    private SnippetPermissionController snippetPermissionController;

    @MockBean
    private SnippetPermissionService snippetPermissionService;

    private String mockToken;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"mockUserId\",\"username\":\"mockUsername\",\"role\":\"user\",\"iat\":1609459200}";
        String signature = "mockSignature";

        mockToken = base64Encode(header) + "." + base64Encode(payload) + "." + signature;
        mockToken = "Bearer " + mockToken;

        when(jwt.getTokenValue()).thenReturn(mockToken);
        when(jwt.getClaim("sub")).thenReturn("mockUserId");
        when(jwt.getClaim("username")).thenReturn("mockUsername");
        when(jwt.getClaim("role")).thenReturn("user");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);

        SecurityContextHolder.setContext(securityContext);
    }

    private String base64Encode(String value) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes());
    }

    @Test
    void testHasAccess() {
        when(snippetPermissionService.hasAccess(anyString(), anyString()))
                .thenReturn(Response.withData(true));

        ResponseEntity<Object> response =
                snippetPermissionController.hasAccess("snippetId", Map.of("authorization", mockToken));

        assertEquals(200, response.getStatusCode().value());

        when(snippetPermissionService.hasAccess(anyString(), anyString()))
                .thenReturn(Response.withData(false));

        ResponseEntity<Object> response2 =
                snippetPermissionController.hasAccess("snippetId", Map.of("authorization", mockToken));

        assertEquals(403, response2.getStatusCode().value());
    }

    @Test
    void testCanEdit() {
        when(snippetPermissionService.canEdit(anyString(), anyString()))
                .thenReturn(Response.withData(true));

        ResponseEntity<Object> response =
                snippetPermissionController.canEdit("snippetId", Map.of("authorization", mockToken));

        assertEquals(200, response.getStatusCode().value());

        when(snippetPermissionService.canEdit(anyString(), anyString()))
                .thenReturn(Response.withData(false));

        ResponseEntity<Object> response2 =
                snippetPermissionController.canEdit("snippetId", Map.of("authorization", mockToken));

        assertEquals(403, response2.getStatusCode().value());
    }

    @Test
    void testSaveRelation() {
        when(snippetPermissionService.saveRelation(anyString(), anyString(), eq(GrantType.WRITE)))
                .thenReturn(Response.withData(""));

        ResponseEntity<Object> response =
                snippetPermissionController.saveRelation("snippetId", Map.of("authorization", mockToken));

        assertEquals(200, response.getStatusCode().value());

        when(snippetPermissionService.saveRelation(anyString(), anyString(), eq(GrantType.WRITE)))
                .thenReturn(Response.withError(new Error(409, "Relationship already exists")));

        ResponseEntity<Object> response2 =
                snippetPermissionController.saveRelation("snippetId", Map.of("authorization", mockToken));

        assertEquals(409, response2.getStatusCode().value());

        when(snippetPermissionService.saveRelation(anyString(), anyString(), eq(GrantType.WRITE)))
                .thenReturn(Response.withError(new Error(500, "Error message")));

        ResponseEntity<Object> response3 =
                snippetPermissionController.saveRelation("snippetId", Map.of("authorization", mockToken));

        assertEquals(500, response3.getStatusCode().value());
    }

    @Test
    void testDeleteRelation() {
        when(snippetPermissionService.deleteRelation(anyString(), anyString()))
                .thenReturn(Response.withData(""));

        ResponseEntity<Object> response =
                snippetPermissionController.deleteRelation("snippetId", Map.of("authorization", mockToken));

        assertEquals(200, response.getStatusCode().value());

        when(snippetPermissionService.deleteRelation(anyString(), anyString()))
                .thenReturn(Response.withError(new Error(500, "Error message")));

        ResponseEntity<Object> response2 =
                snippetPermissionController.deleteRelation("snippetId", Map.of("authorization", mockToken));

        assertEquals(500, response2.getStatusCode().value());
    }

    @Test
    void testdeleteAllRelations() {
        when(snippetPermissionService.deleteAllRelations(anyString()))
                .thenReturn(Response.withData(""));

        ResponseEntity<Object> response =
                snippetPermissionController.deleteAllRelations(
                        "snippetId", Map.of("authorization", mockToken));

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testSaveShareRelation() {
        when(snippetPermissionService.saveShareRelation(any(ShareSnippetDTO.class), anyString()))
                .thenReturn(Response.withData(""));

        ResponseEntity<Object> response =
                snippetPermissionController.saveShareRelation(
                        new ShareSnippetDTO("snippetId", "username"), Map.of("authorization", mockToken));

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testGetRelations() {
        when(snippetPermissionService.getSnippetGrants(anyString(), anyString()))
                .thenReturn(Response.withData(null));

        ResponseEntity<Object> response =
                snippetPermissionController.getRelations("ALL", Map.of("authorization", mockToken));

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testGetAllSnippetsByUser() {
        when(snippetPermissionService.getAllSnippetsByUser(anyString()))
                .thenReturn(Response.withData(null));

        ResponseEntity<Object> response =
                snippetPermissionController.getAllSnippetsByUser(Map.of("authorization", mockToken));

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testGetSnippetAuthor() {
        when(snippetPermissionService.getSnippetAuthor(anyString())).thenReturn(Response.withData(""));

        ResponseEntity<Object> response = snippetPermissionController.getSnippetAuthor("snippetId");

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testGetUsersPaginated() {
        when(snippetPermissionService.getUsersPaginated(anyInt(), anyInt(), anyString()))
                .thenReturn(Response.withData(List.of()));

        ResponseEntity<Object> response = snippetPermissionController.getUsersPaginated("1", "10", "");

        assertEquals(200, response.getStatusCode().value());
    }
}
