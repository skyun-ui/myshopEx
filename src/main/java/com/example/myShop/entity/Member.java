package com.example.myShop.entity;

import com.example.myShop.constant.Role;
import com.example.myShop.dto.MemberFormDto;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.persistence.*;

@Entity
@Table(name = "member_id")
@Getter @Setter
@ToString
public class Member extends BaseEntity{
    @Id
    @Column(name = "member_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    private String address;

    @Enumerated(EnumType.STRING)
    private Role role;

    /** 소셜 로그인 제공자 (예: KAKAO). 일반 가입 회원은 null */
    private String oauthProvider;

    /** 소셜 계정 식별자(카카오 회원번호 등). 일반 가입 회원은 null */
    @Column(unique = true)
    private String oauthId;

    public static Member createMember(MemberFormDto memberFormDto, PasswordEncoder passwordEncoder){
        Member member = new Member();
        member.setName(memberFormDto.getName());
        member.setEmail(memberFormDto.getEmail());
        member.setAddress(memberFormDto.getAddress());
        String password = passwordEncoder.encode(memberFormDto.getPassword());
        member.setPassword(password);
        member.setRole(Role.ADMIN);
        return member;
    }

    /**
     * 카카오 최초 연동 회원 생성. 기존 폼 가입과 동일하게 {@link #email}을 로그인 식별자(Principal 이름)로 사용한다.
     */
    public static Member createKakaoMember(String email, String name, String kakaoMemberId, PasswordEncoder passwordEncoder) {
        Member member = new Member();
        member.setEmail(email);
        member.setName(name);
        member.setAddress("(소셜 로그인)");
        member.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        member.setRole(Role.USER);
        member.setOauthProvider("KAKAO");
        member.setOauthId(kakaoMemberId);
        return member;
    }

    /** 이미 동일 이메일로 가입한 일반 회원 계정에 카카오 로그인을 연동한다. */
    public void linkKakao(String kakaoMemberId) {
        this.oauthProvider = "KAKAO";
        this.oauthId = kakaoMemberId;
    }
}
