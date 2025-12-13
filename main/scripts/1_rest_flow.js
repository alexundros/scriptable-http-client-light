scenario = {
    getScenarioName: function() {
        return "Rest Flow";
    },

    runScenario: function() {
        logger.log("Starting " + this.getScenarioName());

        var port = utils.getAvailableLocalPort();
        var baseUrl = "http://localhost:" + port;

        logger.log(">> Starting Mock Server on port " + port);
        restTestServer.start(port);

        try {
            // Test: Get All Users
            this.testGetAllUsers(baseUrl);
            // Test: Get Single User & Save Context
            var userId = this.testGetSingleUser(baseUrl);
            // Test: Get Posts for User
            if (userId) {
                this.testGetUserPosts(baseUrl, userId);
            }
        } catch (e) {
            logger.error("CRITICAL ERROR: " + e);
        } finally {
            // Teardown
            logger.log(">> Stopping Mock Server");
            restTestServer.stop();
        }
    },

    testGetAllUsers: function(baseUrl) {
        logger.log("--- Step 1: Get All Users ---");
        var resp = http.get(baseUrl + "/users");

        if (this.assertStatus(resp, 200)) {
            // JSONPath usage example
            var allIds = utils.jsonPath(resp.body, "$[*].id");
            var nameOfId1 = utils.jsonPath(resp.body, "$[?(@.id == 1)].name");

            logger.log("Found IDs: " + allIds);
            logger.log("User with ID 1: " + nameOfId1);
        }
    },

    testGetSingleUser: function(baseUrl) {
        logger.log("--- Step 2: Get Single User (Limit 1) ---");
        var resp = http.get(baseUrl + "/users?limit=1");

        if (this.assertStatus(resp, 200)) {
            var users = JSON.parse(resp.body);
            if (users.length > 0) {
                var user = users[0];
                logger.log("Found user: " + user.name + " (ID: " + user.id + ")");

                // Save to file and Shared Context
                utils.saveJsonFile("users_dump", users, true);
                context.put("current_user_id", user.id);
                context.put("current_user_name", user.name);
                logger.log("Saved user data to Context and File.");

                return user.id;
            } else {
                logger.error("User list is empty!");
            }
        }
        return null;
    },

    testGetUserPosts: function(baseUrl, userId) {
        logger.log("--- Step 3: Get Posts for User " + userId + " ---");
        var resp = http.get(baseUrl + "/posts?userId=" + userId);

        if (this.assertStatus(resp, 200)) {
            var posts = JSON.parse(resp.body);
            logger.log("User has " + posts.length + " posts.");
            utils.saveJsonFile("posts_dump", posts, true);
        }
    },

    // Helpers
    assertStatus: function(resp, expected) {
        if (resp.status === expected) {
            return true;
        }
        logger.error("FAIL: Expected status " + expected);
        return false;
    }
}