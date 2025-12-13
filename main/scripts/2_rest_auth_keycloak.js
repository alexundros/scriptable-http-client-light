scenario = {
    getScenarioName: function() {
        return "Rest Auth (OAuth2 Client)";
    },

    runScenario: function() {
        logger.log("Starting " + this.getScenarioName());

        try {
            var tokenUrl = config.get("oauth.url");
            var scope = config.get("client.scope");

            // Use getNotEmpty to quickly check for environment variables
            // If missing, the script will fail immediately with a clear error
            var clientId = env.getNotEmpty("CLIENT_ID");
            var clientSecret = env.getNotEmpty("CLIENT_SECRET");

            logger.log("Config loaded. Requesting Token from: " + tokenUrl);

            // Get Token
            var token = auth.getToken(tokenUrl, clientId, clientSecret, scope);

            if (!token) {
                throw "Token is null or empty!";
            }
            logger.log("Token received successfully.");

            // Use Token (Test against httpbin to verify header)
            var targetUrl = "https://httpbin.org/bearer";
            logger.log("Testing secured resource: " + targetUrl);

            var resp = http.getWithToken(targetUrl, token);

            if (resp.status === 200) {
                var json = JSON.parse(resp.body);

                if (json.authenticated === true) {
                    logger.log("SUCCESS: Server confirmed authentication.");
                } else {
                    logger.error("FAIL: Server returned 200 but authenticated=false");
                }

                // JWT Structure Check (Simple heuristic)
                if (json.token && json.token.split('.').length === 3) {
                    logger.log("Token format looks like valid JWT.");
                }
            } else {
                logger.error("Request failed. Status: " + resp.status);
                logger.error("Body: " + resp.body);
            }

        } catch (e) {
            logger.error("Error: " + e);
        }
    }
}