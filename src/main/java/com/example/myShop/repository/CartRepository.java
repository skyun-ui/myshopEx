package com.example.myShop.repository;

import com.example.myShop.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("select c from Cart c where c.member.id = :memberId")
    Cart findByMemberId(@Param("memberId") Long memberId);
}
