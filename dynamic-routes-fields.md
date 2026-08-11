# Dynamic Routes — Required & Optional Fields
> Generated from: `last_middleware` DB (localhost:5432)  
> Date: 2026-08-11

---

## 1. `dynamic_routes` — الجدول الرئيسي

| Field | Type | Required | Default | Notes |
|---|---|---|---|---|
| `id` | bigint | AUTO | sequence | Primary Key — لا تحطه يدوياً |
| `route_id` | varchar(255) | ✅ YES | — | اسم فريد للـ route مثل `dhis2-system-info` |
| `path` | varchar(255) | ✅ YES | — | مسار الـ URL مثل `/v1/system-info` |
| `http_method` | varchar(255) | ⚠️ Functional | NULL | `GET` / `POST` / `PUT` / `DELETE` — مطلوب عملياً |
| `version` | integer | ✅ YES | — | رقم الإصدار: 1, 2, 3... |
| `active` | boolean | ✅ YES | — | هل الـ route شغّالة؟ `true` / `false` |
| `default_version` | boolean | ✅ YES | `false` | هل هاد الإصدار الافتراضي للـ route؟ |
| `description` | varchar(255) | ✅ YES | — | وصف الـ route |
| `yaml_content` | text | ⚠️ Functional | NULL | محتوى الـ YAML — مطلوب عملياً ليشتغل الـ route |
| `comment` | varchar(255) | ❌ NO | NULL | ملاحظات اختيارية |
| `created_by` | varchar(255) | ✅ YES | — | email المستخدم اللي أنشأه |
| `updated_by` | varchar(255) | ✅ YES | — | email آخر من عدّل |
| `created_at` | timestamp | ✅ YES | — | تاريخ الإنشاء — auto من الكود |
| `updated_at` | timestamp | ✅ YES | — | تاريخ آخر تعديل — auto من الكود |

### قواعد الـ Versioning
```
نفس route_id ممكن يكون عنده أكثر من version
لكن version واحدة فقط تكون default_version = true
والبقية default_version = false

مثال:
  dhis2-upload-csv | v1 | active=false | default=false
  dhis2-upload-csv | v2 | active=false | default=false
  dhis2-upload-csv | v3 | active=false | default=false
  dhis2-upload-csv | v4 | active=true  | default=true  ← الشغّالة
```

### مثال INSERT صحيح
```sql
INSERT INTO dynamic_routes (
    route_id, path, http_method, version,
    active, default_version,
    description, yaml_content,
    created_by, updated_by, created_at, updated_at
) VALUES (
    'my-new-route',
    '/v1/my-endpoint',
    'GET',
    1,
    true,
    true,
    'My new route description',
    '- route:
    id: my-new-route
    from:
      uri: rest
      parameters:
        method: get
        path: /v1/my-endpoint
      steps:
        - log:
            message: Hello',
    'admin@mail.com',
    'admin@mail.com',
    NOW(),
    NOW()
);
```

---

## 2. `source_systems` — مصادر البيانات

| Field | Type | Required | Default | Notes |
|---|---|---|---|---|
| `id` | bigint | AUTO | sequence | Primary Key |
| `name` | varchar(100) | ✅ YES | — | اسم النظام مثل `Hakeem`, `DHIS2` |
| `active` | boolean | ✅ YES | — | هل النظام فعّال؟ |
| `description` | text | ❌ NO | NULL | وصف اختياري |
| `created_by` | varchar(255) | ✅ YES | — | من أنشأه |
| `updated_by` | varchar(255) | ✅ YES | — | من عدّله |
| `created_at` | timestamp | ✅ YES | — | auto |
| `updated_at` | timestamp | ✅ YES | — | auto |

**الأنظمة الحالية:** Hakeem (id=1), HealthMap (id=2), DHIS2 (id=3)

---

## 3. `integrated_apis` — الـ APIs الخارجية

| Field | Type | Required | Default | Notes |
|---|---|---|---|---|
| `id` | bigint | AUTO | sequence | Primary Key |
| `code` | varchar(100) | ✅ YES | — | كود فريد مثل `DHIS2.DEV` |
| `name` | varchar(255) | ✅ YES | — | اسم الـ API |
| `api_url` | text | ✅ YES | — | الـ URL الكامل |
| `type` | varchar(20) | ✅ YES | — | نوع الـ API |
| `integrated_system` | varchar(100) | ✅ YES | — | FK → كود النظام المرتبط |
| `is_active` | boolean | ✅ YES | — | فعّال؟ |
| `use_ou_from_request` | boolean | ✅ YES | — | استخدام الـ OU من الطلب |
| `use_pe_from_request` | boolean | ✅ YES | — | استخدام الـ PE من الطلب |
| `bound_api_code` | varchar(100) | ❌ NO | NULL | ربط بـ API آخر |
| `description` | text | ❌ NO | NULL | وصف |
| `created_by` | varchar(255) | ❌ NO | NULL | من أنشأه |
| `updated_by` | varchar(255) | ❌ NO | NULL | من عدّله |
| `created_at` | timestamp | ✅ YES | — | auto |
| `updated_at` | timestamp | ❌ NO | NULL | auto |

---

## 4. `integrated_system` — الأنظمة المتكاملة (credentials)

| Field | Type | Required | Default | Notes |
|---|---|---|---|---|
| `id` | bigint | AUTO | sequence | Primary Key |
| `code` | varchar(255) | ✅ YES | — | كود النظام مثل `DHIS2` |
| `host` | varchar(255) | ❌ NO | NULL | الـ host |
| `port` | varchar(255) | ❌ NO | NULL | البورت |
| `protocol` | varchar(255) | ❌ NO | NULL | `http` / `https` |
| `authentication_type` | varchar(255) | ❌ NO | NULL | `BASIC` / `BEARER` / `TOKEN` |
| `username` | varchar(255) | ❌ NO | NULL | اسم المستخدم — **سري** |
| `password` | varchar(255) | ❌ NO | NULL | الباسوورد — **سري** |
| `token` | varchar(1000) | ❌ NO | NULL | الـ token — **سري** |
| `additional_key1` | varchar(255) | ❌ NO | NULL | مفتاح إضافي |
| `additional_value1` | varchar(255) | ❌ NO | NULL | قيمة إضافية |
| `additional_key2` | varchar(255) | ❌ NO | NULL | مفتاح إضافي |
| `additional_value2` | varchar(255) | ❌ NO | NULL | قيمة إضافية |
| `description` | varchar(255) | ❌ NO | NULL | وصف |
| `created_by` | varchar(255) | ❌ NO | NULL | من أنشأه |
| `updated_by` | varchar(255) | ❌ NO | NULL | من عدّله |
| `created_at` | timestamp | ❌ NO | NULL | auto |
| `updated_at` | timestamp | ❌ NO | NULL | auto |

> ⚠️ `username`, `password`, `token` حقول سرية — لا تُظهرها في الـ logs أو الـ API responses

---

## 5. `integration_mapping` — تعريف الـ Mappings

| Field | Type | Required | Default | Notes |
|---|---|---|---|---|
| `id` | bigint | AUTO | sequence | Primary Key |
| `dynamic_route_id` | varchar(100) | ✅ YES | — | FK → `route_id` من `dynamic_routes` |
| `external_key` | varchar(100) | ✅ YES | — | المفتاح الخارجي |
| `mapping_type` | varchar(50) | ✅ YES | — | نوع الـ mapping |
| `data` | text | ✅ YES | — | محتوى الـ mapping |
| `is_active` | boolean | ✅ YES | — | فعّال؟ |
| `integrated_api_id` | bigint | ❌ NO | NULL | FK → `integrated_apis.id` |
| `notes` | text | ❌ NO | NULL | ملاحظات |
| `created_by` | varchar(255) | ❌ NO | NULL | من أنشأه |
| `updated_by` | varchar(255) | ❌ NO | NULL | من عدّله |
| `created_at` | timestamp | ✅ YES | — | auto |
| `updated_at` | timestamp | ❌ NO | NULL | auto |

---

## ملخص — ✅ Required فقط (أهم الجداول)

```
dynamic_routes:
  REQUIRED:  route_id, path, version, active, default_version,
             description, created_by, updated_by, created_at, updated_at
  FUNCTIONAL: http_method, yaml_content  (nullable لكن مطلوبة عملياً)
  OPTIONAL:  comment

source_systems:
  REQUIRED:  name, active, created_by, updated_by, created_at, updated_at
  OPTIONAL:  description

integrated_apis:
  REQUIRED:  code, name, api_url, type, integrated_system,
             is_active, use_ou_from_request, use_pe_from_request, created_at
  OPTIONAL:  bound_api_code, description, created_by, updated_by, updated_at

integrated_system:
  REQUIRED:  code
  OPTIONAL:  everything else (host, port, username, password, token...)

integration_mapping:
  REQUIRED:  dynamic_route_id, external_key, mapping_type, data,
             is_active, created_at
  OPTIONAL:  integrated_api_id, notes, created_by, updated_by, updated_at
```
