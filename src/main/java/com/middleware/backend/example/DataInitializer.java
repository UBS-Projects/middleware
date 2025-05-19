package com.middleware.backend.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ExternalApiRepository externalApiRepository;
    private final ConstantRepository constantRepository;

    public DataInitializer(ExternalApiRepository externalApiRepository, ConstantRepository constantRepository) {
        this.externalApiRepository = externalApiRepository;
        this.constantRepository = constantRepository;
    }
//    private final TransformationMappingRepository transformationMappingRepository;
//
//    public DataInitializer(
//            ExternalApiRepository externalApiRepository,
//            TransformationMappingRepository transformationMappingRepository
//    ) {
//        this.externalApiRepository = externalApiRepository;
//        this.transformationMappingRepository = transformationMappingRepository;
//    }

    @Override
    public void run(String... args) {
        // Check if already initialized
        if (externalApiRepository.findByName("ExternalService1").isPresent()) {
            return;
        }

        // 1. Create External API
        ExternalApi reqbinApi = new ExternalApi();
        reqbinApi.setName("ExternalService1");
        reqbinApi.setUrl("https://reqbin.com/echo/post/json");
        reqbinApi.setRequestTemplate("""
                {
                  "Id": #{#root.productId},
                  "Customer": #{#root.customerName},
                  "Quantity": #{#root.qty},
                  "Price": #{#root.unitPrice * constants['priceMultiplier']}
                }
                               
                """);
        constantRepository.save(new Constant("priceMultiplier", "1.05"));

        externalApiRepository.save(reqbinApi);

//        // 2. Create Transformation Mappings
//        List<TransformationMapping> mappings = List.of(
//                createMapping("Id", "#root.productId", reqbinApi),
//                createMapping("Customer", "#root.customerName", reqbinApi),
//                createMapping("Quantity", "#root.qty", reqbinApi),
//                createMapping("Price", "#root.unitPrice * 1.05", reqbinApi)
//        );

//        transformationMappingRepository.saveAll(mappings);

        System.out.println("✅ External API and mappings bootstrapped.");
    }

//    private TransformationMapping createMapping(String targetField, String expression, ExternalApi api) {
//        TransformationMapping mapping = new TransformationMapping();
//        mapping.setTargetField(targetField);
//        mapping.setSpelExpression(expression);
//        mapping.setApi(api);
//        return mapping;
//    }
}
