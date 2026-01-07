# Fix Summary: RequestMappingHandlerMapping Bean Conflict

## Problem
The IAM service was failing to start with the following error:

```
Error creating bean with name 'rateLimitFilter': 
Unsatisfied dependency expressed through constructor parameter 2: 
No qualifying bean of type 'org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping' available: 
expected single matching bean but found 2: 
  - requestMappingHandlerMapping
  - controllerEndpointHandlerMapping
```

## Root Cause
When Spring Boot Actuator is added to the classpath, it creates an additional `RequestMappingHandlerMapping` bean called `controllerEndpointHandlerMapping` for handling actuator endpoints. This caused a conflict because:

1. **Main MVC Handler**: `requestMappingHandlerMapping` - handles regular controller endpoints
2. **Actuator Handler**: `controllerEndpointHandlerMapping` - handles actuator endpoints like `/actuator/health`

The `RateLimitFilter` and `RequestSignatureFilter` in shiva-core-starter were trying to autowire `RequestMappingHandlerMapping` without specifying which bean to use, causing Spring to fail due to ambiguity.

## Solution Applied

Added `@Qualifier("requestMappingHandlerMapping")` annotation to specify we want the main MVC handler mapping:

### Files Modified in shiva-core-starter:

1. **RateLimitFilter.java**
   - Added import: `org.springframework.beans.factory.annotation.Qualifier`
   - Added qualifier to field:
     ```java
     @Qualifier("requestMappingHandlerMapping")
     private final RequestMappingHandlerMapping handlerMapping;
     ```

2. **RequestSignatureFilter.java**
   - Added import: `org.springframework.beans.factory.annotation.Qualifier`
   - Added qualifier to field:
     ```java
     @Qualifier("requestMappingHandlerMapping")
     private final RequestMappingHandlerMapping handlerMapping;
     ```

3. **pom.xml**
   - Bumped version from `1.0.22` to `1.0.23`

### Updated IAM Service:
- Updated dependency version in `iam-service/pom.xml` from `1.0.22` to `1.0.23`

## Build Steps

1. Built shiva-core-starter v1.0.23:
   ```bash
   cd shiva-core-starter
   ./build.sh "clean install"
   ```
   ✅ BUILD SUCCESS

2. Updated iam-service to use v1.0.23
   ✅ COMPLETED

## Testing
The iam-service should now start without the bean conflict error. The rate limiting and request signature filters will correctly use the main MVC handler mapping instead of the actuator handler mapping.

## Why This Fix Works
By adding `@Qualifier("requestMappingHandlerMapping")`, we explicitly tell Spring to inject the main MVC handler mapping bean, which is the one that handles our application's controller endpoints. The actuator handler mapping is still available for actuator endpoints but won't interfere with our filters.

## Related Spring Framework Change
This issue became more prominent in Spring Boot 3.x / Spring Framework 6.x due to stricter requirements around parameter name retention. The error message specifically mentions:
> Ensure that your compiler is configured to use the '-parameters' flag.

However, the proper fix is to use `@Qualifier` to disambiguate between multiple beans of the same type, which is a Spring best practice.

