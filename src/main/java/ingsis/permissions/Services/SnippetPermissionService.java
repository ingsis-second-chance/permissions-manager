package ingsis.permissions.Services;

import ingsis.permissions.DTO.*;
import ingsis.permissions.DTO.Error;
import ingsis.permissions.entities.GrantType;
import ingsis.permissions.entities.SnippetPermission;
import ingsis.permissions.repositories.SnippetPermissionRepository;
import ingsis.permissions.utils.UserService;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SnippetPermissionService {
  private static final org.slf4j.Logger logger =
      LoggerFactory.getLogger(SnippetPermissionService.class);

  @Autowired SnippetPermissionRepository snippetPermissionRepository;

  @Autowired UserService userService;

  private final Logger log = LoggerFactory.getLogger(SnippetPermissionService.class);

  public Response<Boolean> hasAccess(String snippetId, String userId) {
    log.info("hasAccess was called");
    Optional<SnippetPermission> snippetPermission =
        snippetPermissionRepository.findBySnippetIdAndUserId(snippetId, userId);
    if (snippetPermission.isEmpty()) {
      return Response.withError(new Error(404, "Snippet permission not found"));
    }
    return Response.withData(true);
  }

  public Response<String> saveRelation(String snippetId, String userId, GrantType grantType) {
    Optional<SnippetPermission> snippetPermissionOpt =
        snippetPermissionRepository.findBySnippetIdAndUserId(snippetId, userId);
    if (snippetPermissionOpt.isEmpty()) {
      try {
        SnippetPermission snippetPermission = new SnippetPermission();
        snippetPermission.setSnippetId(snippetId);
        snippetPermission.setUserId(userId);
        snippetPermission.setGrantType(grantType);
        snippetPermissionRepository.save(snippetPermission);
        return Response.withData("Relationship saved");
      } catch (Exception e) {
        return Response.withError(new Error(500, e.getMessage()));
      }
    } else {
      return Response.withError(new Error(409, "Relationship already exists"));
    }
  }

  public Response<Boolean> canEdit(String snippetId, String userId) {
    Optional<SnippetPermission> snippetPermission =
        snippetPermissionRepository.findBySnippetIdAndUserId(snippetId, userId);
    if (snippetPermission.isEmpty()) {
      return Response.withError(new Error(404, "Snippet permission not found"));
    }
    boolean canEdit = snippetPermission.get().getGrantType().equals(GrantType.WRITE);
    return Response.withData(canEdit);
  }

  public Response<String> getSnippetAuthor(String snippetId) {
    Optional<SnippetPermission> snippetPermission =
        snippetPermissionRepository.findBySnippetIdAndGrantType(snippetId, GrantType.WRITE);
    if (snippetPermission.isEmpty()) {
      return Response.withError(new Error(404, "Snippet permission not found"));
    }
    String userId = snippetPermission.get().getUserId();
    try {
      String author = userService.getUsernameFromUserId(userId);
      return Response.withData(author);
    } catch (Exception e) {
      return Response.withError(new Error(500, e.getMessage()));
    }
  }

  public Response<List<SnippetPermissionGrantResponse>> getSnippetGrants(
      String userId, String filterType) {
    FilterType filter = FilterType.valueOf(filterType);
    List<SnippetPermission> snippetPermissions =
        snippetPermissionRepository.findAllByUserId(userId);
    if (filter != FilterType.ALL) {
      snippetPermissions =
          snippetPermissions.stream()
              .filter(
                  snippetPermission ->
                      snippetPermission.getGrantType().equals(GrantType.valueOf(filter.name())))
              .toList();
    }
    return Response.withData(
        snippetPermissions.stream()
            .map(
                snippetPermission ->
                    new SnippetPermissionGrantResponse(
                        snippetPermission.getSnippetId(),
                        userService.getUsernameFromUserId(
                            snippetPermissionRepository
                                .findBySnippetIdAndGrantType(
                                    snippetPermission.getSnippetId(), GrantType.WRITE)
                                .get()
                                .getUserId())))
            .toList());
  }

  public Response<List<String>> getAllSnippetsByUser(String userId) {
    List<SnippetPermission> snippetPermissions =
        snippetPermissionRepository.findAllByUserIdAndGrantType(userId, GrantType.WRITE);
    return Response.withData(
        snippetPermissions.stream().map(SnippetPermission::getSnippetId).toList());
  }

  public Response<String> deleteRelation(String snippetId, String userId) {
    Optional<SnippetPermission> snippetPermissionOpt =
        snippetPermissionRepository.findBySnippetIdAndUserId(snippetId, userId);
    if (snippetPermissionOpt.isEmpty()) {
      return Response.withError(new Error(404, "Snippet permission not found"));
    }
    try {
      snippetPermissionRepository.delete(snippetPermissionOpt.get());
      return Response.withData("Relationship deleted");
    } catch (Exception e) {
      return Response.withError(new Error(500, e.getMessage()));
    }
  }

  public Response<String> deleteAllRelations(String snippetId) {
    List<SnippetPermission> snippetPermissions =
        snippetPermissionRepository.findAllBySnippetId(snippetId);
    if (snippetPermissions.isEmpty()) {
      return Response.withError(new Error(404, "Snippet permissions not found"));
    }
    try {
      snippetPermissionRepository.deleteAll(snippetPermissions);
      return Response.withData("All relationships deleted");
    } catch (Exception e) {
      return Response.withError(new Error(500, e.getMessage()));
    }
  }

  public Response<String> saveShareRelation(ShareSnippetDTO shareSnippetDTO, String userId) {
    Response<Boolean> canEdit = canEdit(shareSnippetDTO.getSnippetId(), userId);
    if (canEdit.isError()) {
      return Response.withError(canEdit.getError());
    }
    if (!canEdit.getData()) {
      return Response.withError(new Error(403, "Share Access Denied"));
    }
    String userIdToShare =
        userService.getAllUsers().stream()
            .filter(user -> user.getUsername().equals(shareSnippetDTO.getUsername()))
            .findFirst()
            .map(UserDTO::getUser_id)
            .orElse(null);
    try {
      Response<String> response =
          saveRelation(shareSnippetDTO.getSnippetId(), userIdToShare, GrantType.READ);
      if (response.isError()) {
        return response;
      }
    } catch (Exception e) {
      return Response.withError(new Error(500, e.getMessage()));
    }
    return Response.withData("Snippet shared");
  }

  public Response<List<UserInfo>> getUsersPaginated(Integer size, Integer index, String prefix) {
    try {
      List<UserDTO> allUsers = userService.getAllUsers();
      if (allUsers == null) {
        return Response.withError(new Error(404, "Users not found"));
      }
      List<UserInfo> filteredUsers =
          allUsers.stream()
              .filter(user -> user.getUsername().startsWith(prefix))
              .skip((long) index * size)
              .limit(size)
              .toList()
              .stream()
              .map(user -> new UserInfo(user.getUsername(), user.getUser_id()))
              .toList();
      return Response.withData(filteredUsers);
    } catch (Exception e) {
      return Response.withError(new Error(500, e.getMessage()));
    }
  }
}
