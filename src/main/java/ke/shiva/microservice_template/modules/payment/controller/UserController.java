package ke.shiva.microservice_template.modules.payment.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ke.shiva.microservice_template.modules.payment.dto.users.request.CreateUsersRequest;
import ke.shiva.microservice_template.modules.payment.dto.users.response.UserDto;
import ke.shiva.microservice_template.modules.payment.service.UserService;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.dto.Views;
import ke.shiva.shivacorestarter.util.PaginationUtil;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @JsonView(Views.Detailed.class)
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable String id) {
        return ResponseBuilder.success("User fetched successfully",
                userService.getById(id));
    }

    @GetMapping
    @JsonView(Views.Detailed.class)
    public ResponseEntity<ApiResponse<PaginatedResponse<UserDto>>> listUsers(HttpServletRequest request) {
        Page<UserDto> page = userService.getAllPaginated(request);
        PaginatedResponse<UserDto> response = PaginationUtil.toPaginatedResponse(page);
        return ResponseBuilder.success("Users fetched successfully", response);
    }

//    @GetMapping
//    @JsonView(Views.Detailed.class)
//    public ResponseEntity<ApiResponse<List<UserDto>>> listUsers() {
//        return ResponseBuilder.success("Users fetched successfully",
//                userService.getAll());
//    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUsersRequest request) {
        userService.create(request);
        return ResponseBuilder.success(HttpStatus.CREATED, "User created successfully");
    }

}
