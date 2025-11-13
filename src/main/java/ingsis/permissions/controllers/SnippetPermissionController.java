package ingsis.permissions.controllers;

import ingsis.permissions.DTO.Response;
import ingsis.permissions.DTO.ShareSnippetDTO;
import ingsis.permissions.DTO.SnippetPermissionGrantResponse;
import ingsis.permissions.DTO.UserInfo;
import ingsis.permissions.Services.SnippetPermissionService;
import ingsis.permissions.entities.GrantType;
import ingsis.permissions.utils.TokenUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/snippets")
public class SnippetPermissionController {
  private static final Logger logger = LoggerFactory.getLogger(SnippetPermissionController.class);

  @Autowired SnippetPermissionService snippetPermissionService;

  @GetMapping("has-access")
  public ResponseEntity<Object> hasAccess(
      @RequestParam String snippetId, @RequestHeader Map<String, String> headers) {
    String token = headers.get("authorization").substring(7);
    Map<String, String> userInfo = TokenUtils.decodeToken(token);
    String userId = userInfo.get("userId");
    Response<Boolean> access = snippetPermissionService.hasAccess(snippetId, userId);
    if (access.isError()) {
      return new ResponseEntity<>(
          access.getError().message(), HttpStatus.valueOf(access.getError().code()));
    }
    if (!access.getData()) {
      return new ResponseEntity<>("Access Denied", HttpStatus.FORBIDDEN);
    }
    return ResponseEntity.ok().build();
  }

  @GetMapping("can-edit")
  public ResponseEntity<Object> canEdit(
      @RequestParam String snippetId, @RequestHeader Map<String, String> headers) {
    String token = headers.get("authorization").substring(7);
    Map<String, String> userInfo = TokenUtils.decodeToken(token);
    String userId = userInfo.get("userId");
    Response<Boolean> access = snippetPermissionService.canEdit(snippetId, userId);
    if (access.isError()) {
      return new ResponseEntity<>(
          access.getError().message(), HttpStatus.valueOf(access.getError().code()));
    }
    if (!access.getData()) {
      return new ResponseEntity<>("Access Denied", HttpStatus.FORBIDDEN);
    }
    return ResponseEntity.ok().build();
  }

  @PostMapping("/save/relationship")
  public ResponseEntity<Object> saveRelation(
      @RequestBody String snippetId, @RequestHeader Map<String, String> headers) {
    String token = headers.get("authorization").substring(7);
    Map<String, String> userInfo = TokenUtils.decodeToken(token);
    String userId = userInfo.get("userId");
    Response<String> hasPassed =
        snippetPermissionService.saveRelation(snippetId, userId, GrantType.WRITE);
    if (hasPassed.isError()) {
      return new ResponseEntity<>(
          hasPassed.getError().message(), HttpStatus.valueOf(hasPassed.getError().code()));
    }
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/delete/relationship")
  public ResponseEntity<Object> deleteRelation(
      @RequestBody String snippetId, @RequestHeader Map<String, String> headers) {
    String token = headers.get("authorization").substring(7);
    Map<String, String> userInfo = TokenUtils.decodeToken(token);
    String userId = userInfo.get("userId");
    Response<String> hasPassed = snippetPermissionService.deleteRelation(snippetId, userId);
    if (hasPassed.isError()) {
      return new ResponseEntity<>(
          hasPassed.getError().message(), HttpStatus.valueOf(hasPassed.getError().code()));
    }
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/delete/all-relationships")
  public ResponseEntity<Object> deleteAllRelations(
      @RequestBody String snippetId, @RequestHeader Map<String, String> headers) {
    String token = headers.get("authorization").substring(7);
    Map<String, String> userInfo = TokenUtils.decodeToken(token);
    String userId = userInfo.get("userId");
    Response<String> hasPassed = snippetPermissionService.deleteAllRelations(snippetId);
    if (hasPassed.isError()) {
      return new ResponseEntity<>(
          hasPassed.getError().message(), HttpStatus.valueOf(hasPassed.getError().code()));
    }
    return ResponseEntity.ok().build();
  }

  @PostMapping("/save/share/relationship")
  public ResponseEntity<Object> saveShareRelation(
      @RequestBody ShareSnippetDTO shareSnippetDTO, @RequestHeader Map<String, String> headers) {
    String token = headers.get("authorization").substring(7);
    Map<String, String> userInfo = TokenUtils.decodeToken(token);
    String userId = userInfo.get("userId");
    Response<String> hasPassed =
        snippetPermissionService.saveShareRelation(shareSnippetDTO, userId);
    if (hasPassed.isError()) {
      return new ResponseEntity<>(
          hasPassed.getError().message(), HttpStatus.valueOf(hasPassed.getError().code()));
    }
    return ResponseEntity.ok().build();
  }

  @GetMapping("/get/relationships")
  public ResponseEntity<Object> getRelations(
      @RequestParam String filterType, @RequestHeader Map<String, String> headers) {
    String token = headers.get("authorization").substring(7);
    Map<String, String> userInfo = TokenUtils.decodeToken(token);
    String userId = userInfo.get("userId");
    Response<List<SnippetPermissionGrantResponse>> snippetGrants =
        snippetPermissionService.getSnippetGrants(userId, filterType);
    if (snippetGrants.isError()) {
      logger.error("Error fetching snippet grants: {}", snippetGrants.getError().message());
      return new ResponseEntity<>(
          snippetGrants.getError().message(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    logger.info("Snippet grants found: {}", snippetGrants.getData());
    return ResponseEntity.ok(snippetGrants.getData());
  }

  @GetMapping("/get/all/edit")
  public ResponseEntity<Object> getAllSnippetsByUser(@RequestHeader Map<String, String> headers) {
    String token = headers.get("authorization").substring(7);
    Map<String, String> userInfo = TokenUtils.decodeToken(token);
    String userId = userInfo.get("userId");
    Response<List<String>> response = snippetPermissionService.getAllSnippetsByUser(userId);
    if (response.isError()) {
      return new ResponseEntity<>(
          response.getError().message(), HttpStatus.valueOf(response.getError().code()));
    }
    return ResponseEntity.ok(response.getData());
  }

  @GetMapping("/get/author")
  public ResponseEntity<Object> getSnippetAuthor(@RequestParam String snippetId) {
    Response<String> response = snippetPermissionService.getSnippetAuthor(snippetId);
    if (response.isError()) {
      return new ResponseEntity<>(
          response.getError().message(), HttpStatus.valueOf(response.getError().code()));
    }
    return ResponseEntity.ok(response.getData());
  }

  @GetMapping("/paginated")
  public ResponseEntity<Object> getUsersPaginated(
      @RequestParam String page, @RequestParam String pageSize, @RequestParam String prefix) {
    Integer size = Integer.parseInt(pageSize);
    Integer index = Integer.parseInt(page);
    Response<List<UserInfo>> response =
        snippetPermissionService.getUsersPaginated(size, index, prefix);
    if (response.getError() != null) {
      return ResponseEntity.status(500).body(response.getError());
    }
    HashMap<String, Object> data = new HashMap<>();
    data.put("page", page);
    data.put("page_size", pageSize);
    data.put("count", response.getData().size());
    data.put("users", response.getData());
    return ResponseEntity.ok(data);
  }
}
