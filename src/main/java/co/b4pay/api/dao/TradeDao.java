package co.b4pay.api.dao;

import co.b4pay.api.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TradeDao extends JpaRepository<Trade, String> {

    //@Query("FROM Trade t WHERE t.merchantOrderNo = :merchantOrderNo")
    @Query(value = "SELECT * FROM dst_trade t WHERE t.merchant_order_no = :merchantOrderNo LIMIT 0,1",
            nativeQuery = true)
    Trade findByMerchantOrderNo(@Param("merchantOrderNo") String merchantOrderNo);

    @Query("FROM Trade t WHERE t.merchantId = :merchantId AND t.merchantOrderNo = :merchantOrderNo")
    Trade findByMerchantId(Long merchantId, String merchantOrderNo);

    @Query(value = "SELECT * FROM dst_trade t WHERE t.merchant_order_no = :tradeNo AND t.total_amount = :amount AND t.trade_state = 0 AND t.merchant_id = :merchantId ORDER BY t.update_time DESC LIMIT 0,1",
            nativeQuery = true)
    Trade findByMerchantIdAndAmount(@Param("amount") BigDecimal amount, @Param("merchantId") String merchantId,
                                    @Param("tradeNo") String tradeNo);

    @Query(value = "SELECT * FROM dst_trade t WHERE t.total_amount = :amount AND t.trade_state = 0 AND t.merchant_id = :merchantId AND t.channel_id = :channelId ORDER BY t.update_time DESC LIMIT 0,1",
            nativeQuery = true)
    Trade findByAmountAndChannel(@Param("amount") BigDecimal amount, @Param("merchantId") String merchantId,
                                 @Param("channelId") String channelId);

    @Query(value = "select a.* from dst_trade a left join dst_channel b on a.channel_id = b.id where a.trade_state = 0 and b.router_id in ('fixedCodePay','unFixedCodePay')",
            nativeQuery = true)
    List<Trade> findAllUnPaidTrade();

    @Query(value = "SELECT * FROM dst_trade t WHERE t.merchant_order_no = :tradeNo AND t.total_amount = :amount AND t.trade_state = 0 AND t.merchant_id = :merchantId AND t.channel_id = :channelId ORDER BY t.update_time DESC LIMIT 0,1",
            nativeQuery = true)
    Trade findByNoAmountAndChannel(@Param("tradeNo") String tradeNo, @Param("amount") BigDecimal amount,
                                   @Param("merchantId") String merchantId, @Param("channelId") String channelId);
    @Query(value = "SELECT * FROM dst_trade t WHERE t.pay_order_no = :payOrderNo LIMIT 0,1",
            nativeQuery = true)
    Trade findByPayOrderNo(@Param("payOrderNo") String payOrderNo);
}
