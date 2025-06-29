package com.middleware.backend.kaotocamel.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.backend.kaotocamel.dto.RouteTestRequest;
import com.middleware.backend.kaotocamel.dto.RouteTestResult;
import com.middleware.backend.kaotocamel.dto.RouteValidationResult;
import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;
import com.middleware.backend.kaotocamel.service.DynamicRouteService;
import com.middleware.backend.kaotocamel.service.RouteValidationService;
import com.middleware.backend.kaotocamel.spec.DynamicRouteSpecification;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class DynamicRouteController {

    private final DynamicRouteService routeService;
    private final RouteValidationService routeValidationService;

    //يستقبل YAML content للـ route
    //يستخرج metadata (ID, description, path, method) من الـ YAML
    //ينشئ version جديد للـ route
    //يلغي تفعيل النسخ السابقة ويفعل النسخة الجديدة
    //يحفظ في قاعدة البيانات ويسجل في audit log
    @PostMapping("/deploy")
    public String upload(@RequestBody String yaml, @RequestParam(required = false) String comment) {
        return routeService.uploadRoute(yaml, comment);
    }
//    يلغي تفعيل route معين (soft delete)
//    يوقف الـ route في Camel context
//    يحدث الحالة في قاعدة البيانات
    @DeleteMapping("/{routeId}")
    public String deactivate(@PathVariable String routeId) {
        return routeService.deactivateRoute(routeId);
    }

//    يعود إلى نسخة محددة من الـ route
//    يلغي النسخة الحالية ويفعل النسخة المطلوبة
//    يعيد تحميل الـ route في Camel
    @PostMapping("/{routeId}/revert/{version}")
    public String revert(@PathVariable String routeId, @PathVariable int version) {
        return routeService.revertToVersion(routeId, version);
    }

//    يوقف route معين في Camel context
//    يحدث حالة الـ active إلى false
    @PostMapping("/{routeId}/stop")
    public String stop(@PathVariable String routeId) {
        return routeService.stopRoute(routeId);
    }

//    يشغل آخر نسخة من الـ route
//    يحمل الـ YAML ويشغله في Camel
//    يحدث الحالة إلى active
    @PostMapping("/{routeId}/start")
    public String start(@PathVariable String routeId) {
        return routeService.startRoute(routeId);
    }

//    يتحقق من صحة YAML syntax
//    يتأكد من وجود route ID و description
//    يحاول تحميل الـ route مؤقتاً للتأكد من صحته
    @PostMapping(value = "/validate", consumes = "application/json", produces = "application/json")
    public ResponseEntity<RouteValidationResult> validateRoute(@RequestBody RouteTestRequest request) {
        RouteValidationResult result = routeValidationService.validateRoute(request.getYamlContent());
        return result.isValid() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

//    ينشئ نسخة معدلة من الـ YAML للاختبار
//    يحمل الـ route مؤقتاً
//    يرسل test message ويرجع النتيجة
//    ينظف الـ route المؤقت بعد الانتهاء
    @PostMapping(value = "/test", consumes = "application/json", produces = "application/json")
    public ResponseEntity<RouteTestResult> testRoute(@RequestBody RouteTestRequest request) {
        RouteTestResult result = routeValidationService.testRoute(request.getYamlContent(), request.getTestMessage());
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    //يسترجع قائمة الـ routes مع إمكانية الفلترة
    //يدعم البحث بـ: routeId, description, version, path, httpMethod, active status
    //يدعم فلترة بالتاريخ والبحث في YAML content
    //يرجع النتائج مع pagination
    @GetMapping
    public Page<DynamicRouteEntity> getFilteredRoutes(@RequestParam(required = false) String routeId,
            @RequestParam(required = false) String description, @RequestParam(required = false) Integer version,
            @RequestParam(required = false) String path, @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) Boolean active, @RequestParam(required = false) Boolean defaultVersion,
            @RequestParam(required = false) String comment, @RequestParam(required = false) String yamlContains,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Specification<DynamicRouteEntity> spec = Specification
                .where(DynamicRouteSpecification.hasField("routeId", routeId))
                .and(DynamicRouteSpecification.hasField("description", description))
                .and(DynamicRouteSpecification.hasField("version", version))
                .and(DynamicRouteSpecification.hasField("path", path))
                .and(DynamicRouteSpecification.hasField("httpMethod", httpMethod))
                .and(DynamicRouteSpecification.hasField("active", active))
                .and(DynamicRouteSpecification.hasField("defaultVersion", defaultVersion))
                .and(DynamicRouteSpecification.containsComment(comment))
                .and(DynamicRouteSpecification.containsInYaml(yamlContains))
                .and(DynamicRouteSpecification.createdAfter(createdAfter))
                .and(DynamicRouteSpecification.createdBefore(createdBefore));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return routeService.getAllRoutes(spec, pageable);
    }
}
