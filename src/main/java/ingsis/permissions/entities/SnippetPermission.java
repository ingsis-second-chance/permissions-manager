package ingsis.permissions.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "snippet_permissions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SnippetPermission {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String snippetId;

  private String userId;

  @Enumerated(EnumType.STRING)
  private GrantType grantType;
}
