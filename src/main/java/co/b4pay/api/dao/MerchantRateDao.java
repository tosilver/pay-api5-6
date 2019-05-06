package co.b4pay.api.dao;

import co.b4pay.api.model.MerchantRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantRateDao extends JpaRepository<MerchantRate, Long> {

    @Query("select t from MerchantRate t where t.merchantId = :merchantId and t.routerId = :routerId ")
    MerchantRate findByMerchantIdAndRouterId(@Param("merchantId") Long merchantId,@Param("routerId") String routerId);
}
