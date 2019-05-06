package co.b4pay.api.dao;

import co.b4pay.api.model.Goods;
import co.b4pay.api.model.MerchantRate;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsDao extends JpaRepository<Goods, Long> {

    @Query("select t from Goods t where t.typeId = :typeId")
    List<Goods> findByTypeId(@Param("typeId") Integer typeId);
}
