package com.heilous.global.auth;

import com.heilous.user.entity.User;
import com.heilous.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Value("${oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        String email = extractEmail(provider, oAuth2User);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));

        String token = jwtProvider.createToken(email, user.getRole().name());

        getRedirectStrategy().sendRedirect(request, response, redirectUri + "?token=" + token);
    }

    private String extractEmail(String provider, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        return switch (provider) {
            case "kakao" -> {
                Object kakaoAccount = attributes.get("kakao_account");
                if (kakaoAccount instanceof Map<?, ?> account) {
                    yield (String) account.get("email");
                }
                yield null;
            }
            case "naver" -> {
                Object resp = attributes.get("response");
                if (resp instanceof Map<?, ?> response) {
                    yield (String) response.get("email");
                }
                yield null;
            }
            default -> (String) attributes.get("email"); // google
        };
    }
}
