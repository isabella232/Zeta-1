package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.IllegalStatusException;
import com.ebay.dss.zds.exception.InvalidInputException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller is for testing purpose.
 */
@RestController
@RequestMapping("foo")
public class FooController {
  @RequestMapping(path = "/exception", method = RequestMethod.GET)
  public ResponseEntity<String> exception(@RequestParam(name = "type", defaultValue = "0") String type) {
    switch (type) {
      case "0":
        throw new IllegalStatusException("Exception 0 is thrown.");
      case "1":
        throw new IllegalStateException("Exception 1 is thrown.");
      case "2":
        try {
          String s = "" + 100 / 0;
        }catch(Throwable t){
          throw new InvalidInputException(ErrorCode.INVALID_INPUT, "Invalid input - 2 is not a good input, this is to test nested cause in exception.", t);
        }
      default:
        return ResponseEntity.badRequest().body("Unknown type");
    }
  }
}
