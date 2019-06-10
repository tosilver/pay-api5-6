package co.b4pay.api.dao;

import co.b4pay.api.model.KJAgreeapply;
import co.b4pay.api.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface KJAgreeapplyDao extends JpaRepository<KJAgreeapply, String> {

    @Query(value = "SELECT * FROM dst_kj_agreeapply t WHERE t.meruser_id = :meruserId LIMIT 0,1",
            nativeQuery = true)
    KJAgreeapply findByMeruserId(@Param("meruserId") String meruserId);



}
