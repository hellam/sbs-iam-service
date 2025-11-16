package ke.shiva.sbs_iam.modules.payment.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import ke.shiva.sbs_iam.modules.payment.dto.users.request.CreateUsersRequest;
import ke.shiva.sbs_iam.modules.payment.dto.users.response.UserDto;
import ke.shiva.sbs_iam.modules.payment.entity.User;
import ke.shiva.sbs_iam.modules.payment.mapper.UserMapper;
import ke.shiva.sbs_iam.modules.payment.repository.UserRepository;
import ke.shiva.shivacorestarter.util.EncryptionUtil;
import ke.shiva.shivacorestarter.util.PaginationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepository repo;
    private final UserMapper mapper;
    private final EncryptionUtil encryptionUtil;

    public UserService( UserMapper mapper, EncryptionUtil encryptionUtil) {
        this.encryptionUtil = encryptionUtil;
        this.mapper = mapper;
    }

    @Transactional
    public UserDto create(CreateUsersRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());

        User saved = repo.save(user);
        return mapper.toDto(saved);
    }

    public List<UserDto> getAll() {
        return repo.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    public UserDto getById(String encryptedId) {
        Long id = Long.parseLong(encryptionUtil.decrypt(encryptedId));
        return repo.findById(id).map(mapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + id + " not found"));
    }

    public UserDto getByEmail(String email) {
        User byEmail = repo.findByEmail(email);
        return mapper.toDto(byEmail);
    }

    public UserDto getByUsername(String username) {
        return mapper.toDto(repo.findByUsername(username));
    }

    public Page<UserDto> getAllPaginated(HttpServletRequest request) {
        // Define searchable columns
        List<String> searchableColumns = List.of("fullName", "username", "email");

        // Define sortable columns
        List<String> sortableColumns = List.of("id", "fullName", "username", "email", "createdAt");

        // Define filterable columns
        List<String> filterableColumns = List.of("fullName");

        // Default page size
        int defaultPerPage = 10;
        Page<User> page = PaginationUtil.filterAndPaginate(repo, request,
                searchableColumns,
                sortableColumns,
                filterableColumns,
                defaultPerPage);
        return page.map(mapper::toDto);
    }
}
