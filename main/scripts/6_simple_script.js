logger.log("=== Starting Simple Script ===");

// 1. Using Utilities
var message = "Hello, World!";
var encoded = utils.toBase64(message);
logger.log("Original: " + message);
logger.log("Base64:   " + encoded);

// 2. File Operations
var filename = "simple_raw_test";
var content = "This file was created by a raw script at " + new Date();

try {
    var savedPath = utils.saveFile(filename, content);
    logger.log("Successfully saved file to: " + savedPath);
    
    // Verify reading
    var readBack = utils.readFile(savedPath);
    logger.log("Read back content length: " + readBack.length);
} catch (e) {
    logger.error("Error: " + e);
}

logger.log("=== Simple Script Finished ===");
