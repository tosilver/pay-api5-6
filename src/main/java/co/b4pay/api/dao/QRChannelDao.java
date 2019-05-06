package co.b4pay.api.dao;

import co.b4pay.api.model.MallAddress;
import co.b4pay.api.model.QRChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 渠道信息DAO
 *
 * @author YK
 * @version $Id: ChannelDao.java, v 0.1 2018年4月21日 下午22:09:12 YK Exp $
 */
@Repository
public interface QRChannelDao extends JpaRepository<QRChannel, Long> {

    @Query("FROM QRChannel t WHERE t.status = :status")
    List<QRChannel> findByStatus(@Param("status") int status);

    @Query("FROM QRChannel t WHERE t.id= :id and t.status = :status")
    QRChannel findByIdAndStatus(@Param("id") Long id, @Param("status") int status);


    @Modifying
    @Transactional
    @Query("update QRChannel t set t.turnover= :turnover")
    void updateTurnover(@Param("turnover") BigDecimal turnover);


    @Query("FROM QRChannel t WHERE t.merchantId= :merchantId")
    QRChannel findByMerchantIdaAndId(@Param("merchantId") Long merchantId);



}