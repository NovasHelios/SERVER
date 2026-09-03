package com.heilous.auth.service;

import com.heilous.user.entity.User;
import com.heilous.user.enums.UserRole;
import com.heilous.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        String email = extractEmail(provider, oAuth2User);
        String name  = extractName(provider, oAuth2User);

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    provider + " 계정에서 이메일을 가져올 수 없습니다. 이메일 제공 동의가 필요합니다."
            );
        }

        userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(email)
                        .password("")
                        .name(name != null ? name : email)
                        .role(UserRole.USER)
                        .isVerified(true)
                        .isActive(true)
                        .provider(provider)
                        .build())
        );

        return oAuth2User;
    }

    private String extractEmail(String provider, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        return switch (provider) {
            case "kakao" -> {
                // { kakao_account: { email: "..." } }
                Object kakaoAccount = attributes.get("kakao_account");
                if (kakaoAccount instanceof Map<?, ?> account) {
                    yield (String) account.get("email");
                }
                yield null;
            }
            case "naver" -> {
                // { response: { email: "...", name: "..." } }
                Object response = attributes.get("response");
                if (response instanceof Map<?, ?> resp) {
                    yield (String) resp.get("email");
                }
                yield null;
            }
            default -> (String) attributes.get("email"); // google
        };
    }

    private String extractName(String provider, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        return switch (provider) {
            case "kakao" -> {
                // { properties: { nickname: "..." } }
                Object properties = attributes.get("properties");
                if (properties instanceof Map<?, ?> props) {
                    yield (String) props.get("nickname");
                }
                yield null;
            }
            case "naver" -> {
                Object response = attributes.get("response");
                if (response instanceof Map<?, ?> resp) {
                    yield (String) resp.get("name");
                }
                yield null;
            }
            default -> (String) attributes.get("name"); // google
        };
    }
}
