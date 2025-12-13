scenario = {
    getScenarioName: function() {
        return "Utils & Security Demo";
    },

    runScenario: function() {
        logger.log("Starting " + this.getScenarioName());

        this.demoContext();
        this.demoFileIO();
        this.demoSecurity();
        this.demoXmlHelpers();
    },

    demoContext: function() {
        logger.log("--- Shared Context ---");
        var userName = context.get("current_user_name");
        if (userName) {
            logger.log("PASS: Retrieved user from context: " + userName);
            var encoded = utils.toBase64(userName);
            logger.log("Base64 encoded: " + encoded);
        } else {
            logger.log("WARN: Context is empty. Run script '1' first to populate it.");
        }
    },

    demoFileIO: function() {
        logger.log("--- File I/O ---");
        var fileName = "demo_io";
        var content = "Timestamp: " + new Date();

        var savedPath = utils.saveFile(fileName, content);
        logger.log("Wrote to: " + savedPath);

        var readBack = utils.readFile(savedPath);
        if (readBack === content) {
            logger.log("PASS: Read/Write verification successful.");
        } else {
            logger.error("FAIL: Content mismatch.");
        }
    },

    demoSecurity: function() {
        logger.log("--- Security (Path Traversal) ---");
        try {
            // Attempt to break out of the data/ sandbox
            utils.readFile("../config.properties");
            logger.error("FAIL: Security breach! Should not read external files.");
        } catch (e) {
            // Expecting SecurityException or RuntimeException
            logger.log("PASS: Access denied as expected. (" + e.message + ")");
        }
    },

    demoXmlHelpers: function() {
        logger.log("--- XML Helpers ---");

        // Part A: Map <-> XML Conversion
        var data = {
            "user": {
                "@id": "777",
                "login": "admin",
                "roles": { "role": ["Admin", "Editor"] }
            }
        };

        var xmlStr = utils.mapToXml(data);
        logger.log("Generated Simple XML: " + xmlStr);

        var mapBack = utils.xmlToMap(xmlStr);
        logger.log("Simple XML Map: " + mapBack);

        // Part B: Complex SOAP Response with Embedded (Escaped) XML
        logger.log("--- Complex XML Parsing ---");

        var mockResp =
            "<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>" +
            "  <soap:Body>" +
            "    <GetCompanyResponse xmlns='http://interfax.ru/ifax'>" +
            "      <GetCompanyResult>True</GetCompanyResult>" +
            "      <xmlData>" +
                     "&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;" +
                     "&lt;Response&gt;" +
                       "&lt;ResultInfo ResultType=\"True\" ExecutionTime=\"2\" /&gt;" +
                       "&lt;Data&gt;&lt;Report&gt;SOME_DATA_HERE&lt;/Report&gt;&lt;/Data&gt;" +
                     "&lt;/Response&gt;" +
            "      </xmlData>" +
            "    </GetCompanyResponse>" +
            "  </soap:Body>" +
            "</soap:Envelope>";

        logger.log("MockResp xml string: " + utils.xmlToString(mockResp));

        // Extract the <xmlData> node using XPath
        var xmlData = utils.xpathNode(mockResp, "//*[local-name()='xmlData']");
        logger.log("XmlData xml string: " + utils.xmlToString(xmlData));
        logger.log("XmlData map: " + utils.xmlToMap(xmlData));

        // Parse the inner escaped XML string into a real DOM Node
        var xmlDataInner = utils.parseInnerXmlNode(xmlData);

        // Save the parsed inner XML to a file
        var xmlFile = utils.saveXmlFile("xml-data-inner", xmlDataInner, true);
        logger.log("XmlDataInner saved to file: " + xmlFile);

        logger.log("XmlDataInner xml string: " + utils.xmlToString(xmlDataInner));

        // Convert the inner XML to a Map for easy access
        var xmlDataInnerMap = utils.xmlToMap(xmlDataInner);
        logger.log("XmlDataInner map: " + xmlDataInnerMap);
    }
}