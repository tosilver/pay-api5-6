package co.b4pay.api.service;

import co.b4pay.api.dao.TradeDao;
import co.b4pay.api.model.Trade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class TradeService extends BaseService<TradeDao, Trade, String> {

    public Trade findByMerchantOrderNo(String merchantOrderNo) {
        return dao.findByMerchantOrderNo(merchantOrderNo);
    }

    public Trade findByPayOrderNo(String PayOrderNo) {
        return dao.findByPayOrderNo(PayOrderNo);
    }
    public Trade findByMerchantId(Long merchantId, String merchantOrderNo) {
        return dao.findByMerchantId(merchantId, merchantOrderNo);
    }

    public Trade findByAmountAndChannel(BigDecimal amount, String merchantId, String chennelId) {
        return dao.findByAmountAndChannel(amount, merchantId, chennelId);
    }
}
