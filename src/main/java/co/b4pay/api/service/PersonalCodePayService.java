package co.b4pay.api.service;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.model.Router;
import co.b4pay.api.model.Trade;
import co.b4pay.api.model.base.AjaxResponse;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class PersonalCodePayService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(PersonalCodePayService.class);

    //public AjaxResponse execute(JSONObject params, Router router) throws BizException {
    //
    //    String merchantId = params.containsKey("merchantId")?
    //            params.getString("merchantId"):null;
    //
    //    Double amount = params.containsKey("amount")?
    //            params.getDouble("amount"):null;
    //
    //    Integer payWay = params.containsKey("payWay")?
    //            params.getInteger("payWay"):null;
    //
    //    String tradeNo = params.containsKey("tradeNo")?
    //            params.getString("tradeNo"):null;
    //
    //    Trade trade = tradeDao.findByMerchantIdAndAmount(new BigDecimal(amount == null?0D:amount),String.valueOf(merchantId),tradeNo);
    //
    //    if(trade != null){
    //        throw new BizException(String.format("[%s]商户有金额为[%s]的未支付订单,订单号为：%s", merchantId,amount,tradeNo));
    //    }
    //
    //
    //}

}
