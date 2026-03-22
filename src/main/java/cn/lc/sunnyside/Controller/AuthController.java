package cn.lc.sunnyside.Controller;

import cn.lc.sunnyside.POJO.DTO.FamilyAuthDTO;
import cn.lc.sunnyside.Service.FamilyAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/family")
public class AuthController {
    private final FamilyAuthService familyAuthService;

    @PostMapping("/login")
    public FamilyAuthDTO.FamilyLoginResponse login(@RequestBody FamilyAuthDTO.FamilyLoginRequest request) {
        return familyAuthService.login(request);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalStateException.class)
    public ErrorResponse handleInternalError(IllegalStateException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    public record ErrorResponse(String message) {
    }
}
