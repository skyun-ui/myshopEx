package com.example.myShop.service;

import com.example.myShop.entity.Member;
import com.example.myShop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Member saveMember(Member member){
        validateDuplicateMember(member);
        return memberRepository.save(member);
    }
    private void validateDuplicateMember(Member member){
        Member findMember = memberRepository.findByEmail(member.getEmail());
        if(findMember != null){
            throw new IllegalStateException("이미 가입 ㅅㄱ요");
        }
    }

    /**
     * 카카오 로그인: 이메일로 회원 여부를 판별한다.
     * 없으면 닉네임·이메일로 신규 {@link Member}를 만들고 {@link #saveMember(Member)}로 저장한다(비밀번호는 난수 인코딩).
     * 있으면 닉네임 등 최신 정보를 반영하고 저장한다.
     */
    public Member registerOrUpdateFromKakao(String email, String nickname, String kakaoMemberId) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("카카오 이메일 동의가 필요합니다.");
        }
        Member existing = memberRepository.findByEmail(email);
        if (existing != null) {
            if (StringUtils.hasText(existing.getOauthId())
                    && !kakaoMemberId.equals(existing.getOauthId())) {
                throw new IllegalStateException("이 이메일은 다른 카카오 계정과 이미 연동되어 있습니다.");
            }
            if (StringUtils.hasText(nickname)) {
                existing.setName(nickname);
            }
            if (!StringUtils.hasText(existing.getOauthId())) {
                existing.linkKakao(kakaoMemberId);
            }
            return memberRepository.save(existing);
        }

        String name = StringUtils.hasText(nickname) ? nickname : "카카오사용자";
        Member created = Member.createKakaoMember(email, name, kakaoMemberId, passwordEncoder);
        return saveMember(created);
    }
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{
        Member member = memberRepository.findByEmail(email);
        if(member == null){
            throw new UsernameNotFoundException(email);
        }
        return User.builder()
                .username(member.getEmail())
                .password(member.getPassword())
                .roles(member.getRole().toString())
                .build();
    }
}

