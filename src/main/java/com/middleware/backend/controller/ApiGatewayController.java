package com.middleware.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.middleware.backend.dto.ApiRequest;
import com.middleware.backend.dto.ApiResponse;
import com.middleware.backend.exception.ApiNotFoundException;
import com.middleware.backend.service.ApiGatewayService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/gateway")
public class ApiGatewayController {

    private final ApiGatewayService apiGatewayService;

    @PostMapping("/**")
    public ResponseEntity<Object> handleRequest(@RequestBody ApiRequest request,
                                                @RequestHeader Map<String, String> headers,
                                                HttpServletRequest servletRequest) {
        String path = servletRequest.getRequestURI().replace("/gateway", "");
        try{
        ApiResponse response = apiGatewayService.handleRequest(path, RequestMethod.POST, request.getBody(), headers);

        return ResponseEntity
        .status(response.getStatus())
        .headers(response.getHeaders())
        .body(response.getBody());
        }catch(Exception e){
            log.error("Validation Error: {}", e.getMessage());

            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("error", e.getMessage());
            errorBody.put("message", e.getMessage());

            Map<String, String> responseHeaders = new HashMap<>();
            responseHeaders.put("API Validatin", "Failed");

            ApiResponse response = ApiResponse.withHeaders(errorBody, HttpStatus.INTERNAL_SERVER_ERROR, responseHeaders);
       
            return ResponseEntity
        .status(response.getStatus())
        .headers(response.getHeaders())
        .body(response);
        }
        /*
        // Build HttpHeaders from ApiResponse
        HttpHeaders responseHeaders = new HttpHeaders();
        if (response.getHeaders() != null) {
            response.getHeaders().forEach(responseHeaders::add);
        }

        return ResponseEntity
                .status(response.getStatus())
                .headers(responseHeaders)
                .body(response.getBody());
*/
    

    }

    // TODO: Add support for GET, PUT, DELETE etc. if needed
}
