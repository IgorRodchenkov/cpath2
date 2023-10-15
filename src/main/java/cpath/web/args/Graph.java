package cpath.web.args;

import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

import cpath.service.api.GraphType;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Deprecated
public class Graph extends BaseGraph {
  @NotNull(message = "Parameter 'kind' is required.")
  @Schema(
    description = "BioPAX graph traversal type.",
    required = true,
    example = "neighborhood"
  )
  private GraphType kind;

  @Schema(
      description = "Graph search direction (default: UNDIRECTED)",
      example = "undirected"
  )
  private Direction direction;

  @Schema(
    description = "Target BioPAX entity URIs/IDs; optional - only for PATHSFROMTO graph " +
        "(also when missing, then PATHSBETWEEN is there used).",
    example = "[]"
  )
  private String[] target;

  @Schema(
      description = "Limit Type: 'normal', 'shortest-plus-k'; only for PATHSFROMTO query (default: normal)",
      example = "normal"
  )
  private LimitType limitType;

  public Graph() {
    super();
    limitType = LimitType.NORMAL; //for pathsfromto only
    direction = Direction.UNDIRECTED;
  }

  public GraphType getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = GraphType.typeOf(kind); //null when illegal value (also handles empty/null and register/case)
  }

  public Direction getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    Direction dir = Direction.typeOf(direction); //null when illegal value (also handles empty/null and register/case)
    this.direction = (dir != null) ? dir : Direction.UNDIRECTED;
  }

  public String[] getTarget() {
    return target;
  }

  public void setTarget(String[] target) {
    Set<String> uris = new HashSet<>(target.length);
    for (String item : target) {
      if (item.contains(",")) {
        //split by ',' ignoring spaces and empty values (between ,,)
        for (String id : item.split("\\s*,\\s*", -1))
          uris.add(id);
      } else {
        uris.add(item.trim());
      }
    }
    this.target = uris.toArray(new String[uris.size()]);
  }

  public LimitType getLimitType() {
    return limitType;
  }

  public void setLimitType(String limitType) {
     LimitType limt = LimitType.typeOf(limitType);
     this.limitType = (limt != null) ? limt : LimitType.NORMAL;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    if (limitType != null)
      sb.append("; limt:").append(limitType);
    if (target != null && target.length > 0)
      sb.append("; tgt:").append(Arrays.toString(target));
    if (direction != null)
      sb.append("; dir:").append(direction);
    return sb.toString();
  }

  @Override
  public String cmd() {
    return (kind != null) ? kind.toString() : "graph";
  }

}
