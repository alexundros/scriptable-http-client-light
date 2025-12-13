package ru.alxpro.scriptable_http_client_light.script.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebService(serviceName = "Calculator", targetNamespace = "calculator")
public class CalcSoapService {

  private static final Logger log = LoggerFactory.getLogger(CalcSoapService.class);

  @WebMethod(operationName = "Add")
  public int add(@WebParam(name = "intA") int intA, @WebParam(name = "intB") int intB) {
    int result = intA + intB;
    log.info("SoapTestServer (JAX-WS) calc: {} + {} = {}", intA, intB, result);
    return result;
  }
}
