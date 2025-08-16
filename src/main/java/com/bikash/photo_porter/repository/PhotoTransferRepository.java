package com.bikash.photo_porter.repository;

import com.bikash.photo_porter.model.PhotoTransfer;
import com.bikash.photo_porter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoTransferRepository extends JpaRepository<PhotoTransfer, Long> {
    List<PhotoTransfer> findBySourceUser(User sourceUser);
    List<PhotoTransfer> findByTargetUser(User targetUser);
}
