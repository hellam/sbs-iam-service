package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeSupportContentUpsertRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeSupportContentResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.system.SupportContentEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.backoffice.SupportContentCategory;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SupportContentRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackofficeSupportContentService {

    private static final String DEFAULT_ACTOR = "backoffice";
    private static final List<String> CONTACT_CHANNELS = List.of("PHONE", "EMAIL", "WHATSAPP", "BRANCH");

    private final SupportContentRepository supportContentRepository;
    private final EncryptionUtil encryptionUtil;

    @Transactional(readOnly = true)
    public List<BackofficeSupportContentResponse> listByCategory(String category) {
        SupportContentCategory resolvedCategory = resolveCategory(category);
        return supportContentRepository.findByCategoryOrderBySortOrderAscUpdatedAtDesc(resolvedCategory).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BackofficeSupportContentResponse create(BackofficeSupportContentUpsertRequest request) {
        SupportContentEntity entity = new SupportContentEntity();
        applyUpsert(entity, request, DEFAULT_ACTOR);
        SupportContentEntity saved = supportContentRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public BackofficeSupportContentResponse update(String encryptedId, BackofficeSupportContentUpsertRequest request) {
        Long id = decodeId(encryptedId);
        SupportContentEntity entity = supportContentRepository.findById(id)
                .orElseThrow(() -> BaseException.notFound("Support content not found."));
        applyUpsert(entity, request, DEFAULT_ACTOR);
        SupportContentEntity saved = supportContentRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public BackofficeSupportContentResponse updateStatus(String encryptedId, boolean active) {
        Long id = decodeId(encryptedId);
        SupportContentEntity entity = supportContentRepository.findById(id)
                .orElseThrow(() -> BaseException.notFound("Support content not found."));
        entity.setIsActive(active);
        entity.setUpdatedBy(DEFAULT_ACTOR);
        SupportContentEntity saved = supportContentRepository.save(entity);
        return toResponse(saved);
    }

    private void applyUpsert(
            SupportContentEntity entity,
            BackofficeSupportContentUpsertRequest request,
            String actor
    ) {
        SupportContentCategory category = resolveCategory(request.getCategory());
        String title = normalizeRequired(request.getTitle(), "Title is required.");
        String content = normalizeRequired(request.getContent(), "Content is required.");
        String subtitle = normalizeOptional(request.getSubtitle());
        String contactChannel = normalizeOptional(request.getContactChannel());
        String contactValue = normalizeOptional(request.getContactValue());
        Integer sortOrder = request.getSortOrder() == null ? 0 : request.getSortOrder();
        if (sortOrder < 0) {
            throw BaseException.badRequest("Sort order cannot be negative.");
        }

        if (category == SupportContentCategory.CONTACT) {
            if (!StringUtils.hasText(contactChannel)) {
                throw BaseException.badRequest("Contact channel is required for contact entries.");
            }
            if (!StringUtils.hasText(contactValue)) {
                throw BaseException.badRequest("Contact value is required for contact entries.");
            }
            String upperChannel = contactChannel.toUpperCase(Locale.ROOT);
            if (!CONTACT_CHANNELS.contains(upperChannel)) {
                throw BaseException.badRequest("Unsupported contact channel. Use PHONE, EMAIL, WHATSAPP, or BRANCH.");
            }
            contactChannel = upperChannel;
        } else {
            contactChannel = null;
            contactValue = null;
        }

        entity.setCategory(category);
        entity.setTitle(title);
        entity.setSubtitle(subtitle);
        entity.setContent(content);
        entity.setContactChannel(contactChannel);
        entity.setContactValue(contactValue);
        entity.setSortOrder(sortOrder);
        entity.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        if (!StringUtils.hasText(entity.getCreatedBy())) {
            entity.setCreatedBy(actor);
        }
        entity.setUpdatedBy(actor);
    }

    private SupportContentCategory resolveCategory(String rawCategory) {
        if (!StringUtils.hasText(rawCategory)) {
            throw BaseException.badRequest("Category is required.");
        }
        String normalized = rawCategory.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(SupportContentCategory.values())
                .filter(value -> value.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> BaseException.badRequest("Unsupported category. Use FAQ, TROUBLESHOOTING, or CONTACT."));
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (!StringUtils.hasText(normalized)) {
            throw BaseException.badRequest(message);
        }
        return normalized;
    }

    private String encodeId(Long id) {
        return encryptionUtil.encrypt(String.valueOf(id));
    }

    private Long decodeId(String encryptedId) {
        if (!StringUtils.hasText(encryptedId)) {
            throw BaseException.badRequest("Support content ID is required.");
        }
        try {
            return Long.valueOf(encryptionUtil.decrypt(encryptedId.trim()));
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to decrypt support-content ID token. tokenPrefix={}",
                    encryptedId.substring(0, Math.min(16, encryptedId.length()))
            );
            throw BaseException.badRequest("Invalid support content ID.");
        }
    }

    private BackofficeSupportContentResponse toResponse(SupportContentEntity entity) {
        return BackofficeSupportContentResponse.builder()
                .id(encodeId(entity.getId()))
                .category(entity.getCategory() == null ? null : entity.getCategory().name())
                .title(entity.getTitle())
                .subtitle(entity.getSubtitle())
                .content(entity.getContent())
                .contactChannel(entity.getContactChannel())
                .contactValue(entity.getContactValue())
                .sortOrder(entity.getSortOrder())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
