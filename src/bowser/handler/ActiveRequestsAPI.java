package bowser.handler;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.normalize;

import bowser.model.Controller;
import bowser.model.Handler;
import bowser.model.Request;

import ox.Json;
import ox.x.XList;

/**
 * Adds a route which can show currently active requests which are processing on this server.
 */
public class ActiveRequestsAPI extends Controller {

  private String bowserToken;

  public ActiveRequestsAPI(String bowserToken) {
    this.bowserToken = normalize(bowserToken);
  }

  @Override
  public void init() {
    route("GET", "/bowser/requests").to(getRequests);
  }

  private final Handler getRequests = (request, response) -> {
    String token = normalize(request.param("bowserToken"));
    checkState(token.equals(this.bowserToken), "Invalid bowserToken: " + token);

    XList<Request> requests = XList.create(getServer().getRequestsInProgress())
        .filter(r -> !r.path.equals("/bowser/requests"));

    response.write(Json.array(requests,
        r -> Json.object()
            .with("path", r.path)));
  };

}
