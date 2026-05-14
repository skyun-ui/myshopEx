package com.example.myShop.service;

import com.example.myShop.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 카카오 OAuth2 사용자 정보를 {@link Member}와 맞추고, 기존 폼 로그인과 동일하게 Principal 이름을 이메일로 둔다.
 * 이메일 기준으로 {@link MemberService#registerOrUpdateFromKakao}에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String REGISTRATION_KAKAO = "kakao";

    private final MemberService memberService;

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User loaded = delegate.loadUser(userRequest);

        if (!REGISTRATION_KAKAO.equals(userRequest.getClientRegistration().getRegistrationId())) {
            return loaded;
        }

        KakaoUserInfo kakao = KakaoUserInfo.from(loaded.getAttributes());
        if (!StringUtils.hasText(kakao.getEmail())) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "insufficient_scope",
                    "카카오 로그인을 위해 이메일 제공에 동의해 주세요.",
                    null
            ));
        }

        Member member;
        try {
            member = memberService.registerOrUpdateFromKakao(
                    kakao.getEmail(),
                    kakao.getNickname(),
                    kakao.getId()
            );
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "invalid_request",
                    e.getMessage(),
                    null
            ), e);
        } catch (IllegalStateException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "account_conflict",
                    e.getMessage(),
                    null
            ), e);
        }

        Map<String, Object> attributes = new HashMap<>(loaded.getAttributes());
        attributes.put("email", member.getEmail());

        String roleName = member.getRole().name();
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + roleName)),
                attributes,
                "email"
        );
    }

    private static final class KakaoUserInfo {
        private final String id;
        private final String email;
        private final String nickname;

        private KakaoUserInfo(String id, String email, String nickname) {
            this.id = id;
            this.email = email;
            this.nickname = nickname;
        }

        static KakaoUserInfo from(Map<String, Object> attributes) {
            Object idObj = attributes.get("id");
            if (!(idObj instanceof Number)) {
                throw new OAuth2AuthenticationException(new OAuth2Error(
                        "invalid_user_info",
                        "카카오 사용자 ID를 확인할 수 없습니다.",
                        null
                ));
            }
            String id = String.valueOf(((Number) idObj).longValue());

            String email = null;
            String nickname = null;

            Object kakaoAccountObj = attributes.get("kakao_account");
            if (kakaoAccountObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> kakaoAccount = (Map<String, Object>) kakaoAccountObj;
                Object emailObj = kakaoAccount.get("email");
                if (emailObj instanceof String) {
                    email = (String) emailObj;
                }
                Object profileObj = kakaoAccount.get("profile");
                if (profileObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> profile = (Map<String, Object>) profileObj;
                    Object nickObj = profile.get("nickname");
                    if (nickObj instanceof String) {
                        nickname = (String) nickObj;
                    }
                }
            }

            return new KakaoUserInfo(id, email, nickname);
        }

        String getId() {
            return id;
        }

        String getEmail() {
            return email;
        }

        String getNickname() {
            return nickname;
        }
    }
}
