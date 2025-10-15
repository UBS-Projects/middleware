package com.middleware.model;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MappingRequest {
    private String middlewareApiName;
    private String periodParam;
    private String ouParam;
    private String dhis2Code;
    private String transactionUUID;
}