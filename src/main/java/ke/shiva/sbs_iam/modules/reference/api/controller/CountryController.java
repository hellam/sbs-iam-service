package ke.shiva.sbs_iam.modules.reference.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.reference.api.mapper.CountryMapper;
import ke.shiva.sbs_iam.modules.reference.api.request.CountryRequest;
import ke.shiva.sbs_iam.modules.reference.api.response.CountryResponse;
import ke.shiva.sbs_iam.modules.reference.app.service.CountryService;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Country Management")
@RequestMapping("/api/countries")
public class CountryController {

    private final CountryService countryService;

    @GetMapping
    @Operation(summary = "Get all countries with pagination")
    public ResponseEntity<ApiResponse<Page<CountryResponse>>> getAllCountries(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.debug("GET /api/countries - Fetching countries with pagination");
        Page<CountryEntity> countries = countryService.findAll(pageable);
        Page<CountryResponse> response = countries.map(CountryMapper::toResponse);
        return ResponseBuilder.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get country by ID")
    public ResponseEntity<ApiResponse<CountryResponse>> getCountryById(@PathVariable Long id) {
        log.debug("GET /api/countries/{} - Fetching country", id);
        CountryEntity country = countryService.findById(id);
        CountryResponse response = CountryMapper.toResponse(country);
        return ResponseBuilder.success(response);
    }

    @GetMapping("/code/{countryCode}")
    @Operation(summary = "Get country by code")
    public ResponseEntity<ApiResponse<CountryResponse>> getCountryByCode(@PathVariable String countryCode) {
        log.debug("GET /api/countries/code/{} - Fetching country", countryCode);
        CountryEntity country = countryService.findByCountryCode(countryCode)
                .orElseThrow(() -> BaseException.notFound("Country not found with code: " + countryCode));
        CountryResponse response = CountryMapper.toResponse(country);
        return ResponseBuilder.success(response);
    }

    @PostMapping
    @Operation(summary = "Create a new country")
    public ResponseEntity<ApiResponse<CountryResponse>> createCountry(@Valid @RequestBody CountryRequest request) {
        log.debug("POST /api/countries - Creating country: {}", request.getCountryCode());

        CountryEntity country = CountryMapper.toEntity(request);
        CountryEntity createdCountry = countryService.create(country);
        CountryResponse response = CountryMapper.toResponse(createdCountry);

        return ResponseBuilder.success("Country created successfully", response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing country")
    public ResponseEntity<ApiResponse<CountryResponse>> updateCountry(
            @PathVariable Long id,
            @Valid @RequestBody CountryRequest request
    ) {
        log.debug("PUT /api/countries/{} - Updating country", id);

        CountryEntity country = CountryMapper.toEntity(request);
        CountryEntity updatedCountry = countryService.update(id, country);
        CountryResponse response = CountryMapper.toResponse(updatedCountry);

        return ResponseBuilder.success("Country updated successfully", response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a country")
    public ResponseEntity<ApiResponse<Void>> deleteCountry(@PathVariable Long id) {
        log.debug("DELETE /api/countries/{} - Deleting country", id);
        countryService.delete(id);
        return ResponseBuilder.success("Country deleted successfully");
    }
}
