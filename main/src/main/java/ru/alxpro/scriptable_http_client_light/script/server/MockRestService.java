package ru.alxpro.scriptable_http_client_light.script.server;

import com.google.gson.Gson;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class MockRestService {

  private static final Logger log = LoggerFactory.getLogger(MockRestService.class);
  private final Gson gson = new Gson();
  private final List<User> users = new ArrayList<>();
  private final List<Post> posts = new ArrayList<>();

  private static class User {

    int id;
    String name;
    String username;

    User(int i, String n, String u) {
      id = i;
      name = n;
      username = u;
    }
  }

  private static class Post {

    int userId;
    int id;
    String title;

    Post(int u, int i, String t) {
      userId = u;
      id = i;
      title = t;
    }
  }

  public MockRestService() {
    users.add(new User(1, "Leanne Graham", "Bret"));
    users.add(new User(2, "Ervin Howell", "Antonette"));
    posts.add(new Post(1, 101, "First Post by Leanne"));
    posts.add(new Post(1, 102, "Second Post by Leanne"));
    posts.add(new Post(2, 201, "Post by Ervin"));
  }

  @GET
  @Path("/users")
  @Produces(MediaType.APPLICATION_JSON)
  public String getUsers(@QueryParam("limit") Integer limit) {
    List<User> result = new ArrayList<>(users);
    if (limit != null && limit > 0 && limit < result.size()) {
      result = result.subList(0, limit);
    }
    log.info("MockRestService: getUsers limit={}", limit);
    return gson.toJson(result);
  }

  @GET
  @Path("/posts")
  @Produces(MediaType.APPLICATION_JSON)
  public String getPosts(@QueryParam("userId") Integer userId) {
    List<Post> result = new ArrayList<>(posts);
    if (userId != null) {
      result = result.stream()
          .filter(p -> p.userId == userId)
          .collect(Collectors.toList());
    }
    log.info("MockRestService: getPosts userId={}", userId);
    return gson.toJson(result);
  }
}
