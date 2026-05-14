package com.example.myShop.repository;

import com.example.myShop.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member,Long> {
    Member findByEmail(String email);

    Optional<Member> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);
}
