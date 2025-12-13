scenario = {
    getScenarioName: function() {
        return "External SOAP (W3Schools Temp Convert)";
    },

    runScenario: function() {
        logger.log("Starting " + this.getScenarioName());

        try {
            var tempC = utils.prompt("Enter Temperature in Celsius");

            var url = "https://www.w3schools.com/xml/tempconvert.asmx";
            var action = "https://www.w3schools.com/xml/CelsiusToFahrenheit";
            var operation = "CelsiusToFahrenheit";

            var ns = { "xmlns": "https://www.w3schools.com/xml/" };
            var params = { "Celsius": tempC };

            logger.log("Sending SOAP request to external server...");

            // Using invokeAsMap for easier response handling
            var result = soap.invokeAsMap(url, action, operation, params, ns);
            utils.saveJsonFile("temp_convert_result", result, true);

            // Parse response (structure depends on WSDL service)
            // Expected: { CelsiusToFahrenheitResponse: { CelsiusToFahrenheitResult: "..." } }
            if (result && result.CelsiusToFahrenheitResponse) {
                var fahrenheit = result.CelsiusToFahrenheitResponse.CelsiusToFahrenheitResult;
                logger.log("RESULT: " + tempC + "°C = " + fahrenheit + "°F");
            } else {
                logger.error("Unexpected response structure: " + result);
            }
        } catch (e) {
            logger.error("Error: " + e);
        }
    }
}