package co.b4pay.api.dao;

import co.b4pay.api.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantDao extends JpaRepository<Merchant, Long> {



}
