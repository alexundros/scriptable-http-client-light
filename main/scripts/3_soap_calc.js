scenario = {
    getScenarioName: function() {
        return "Soap Calculator (JAX-WS Test)";
    },

    runScenario: function() {
        logger.log("Starting " + this.getScenarioName());

        var port = utils.getAvailableLocalPort();
        var url = "http://localhost:" + port + "/calculator";

        // Start internal SOAP server
        soapTestServer.start(url);

        try {
            var ns = { "ns": "calculator" };
            var params = { intA: 150, intB: 250 };

            // Test 1: invoke (returns DOM Node)
            logger.log("--- Test 1: invoke (DOM Node) ---");
            // Note: SOAPAction is quoted as required by standard
            var resultNode = soap.invoke(url, "\"Add\"", "ns:Add", params, ns);

            // Save raw XML response
            var savedFile = utils.saveXmlFile("soap_result", resultNode, true);
            logger.log("Response saved to: " + savedFile);

            // Parse response via XPath
            // Searching for AddResponse ignoring namespace prefix
            var sum = utils.xpathString(resultNode, "//*[local-name()='AddResponse']");

            if (sum == "400") { // 150 + 250
                logger.log("PASS: 150 + 250 = " + sum);
            } else {
                logger.error("FAIL: Expected 400, got " + sum);
            }

            // Test 2: invokeAsMap (returns Java Map)
            logger.log("--- Test 2: invokeAsMap (Java Map) ---");
            var resultMap = soap.invokeAsMap(url, "\"Add\"", "ns:Add", params, ns);
            logger.log("Map Result: " + resultMap);

            // Check value in map (structure depends on XmlUtils.xmlToMap)
            if (resultMap) logger.log("PASS: Map conversion successful");

        } catch (e) {
            logger.error("Error: " + e);
        } finally {
            soapTestServer.stop();
        }
    }
}