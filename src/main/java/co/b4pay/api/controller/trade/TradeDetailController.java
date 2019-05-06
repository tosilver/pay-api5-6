package co.b4pay.api.controller.trade;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.web.BaseController;
import co.b4pay.api.model.Trade;
import co.b4pay.api.model.base.AjaxResponse;
import co.b4pay.api.service.TradeService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 订单详情
 *
 * @author YK
 * @version $Id v 0.1 2017年08月08日 15:51 Exp $
 */
@RestController
@RequestMapping("/trade/detail.do")
public class TradeDetailController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(TradeDetailController.class);

    private static final String[] REQUIRED_PARAMS = new String[]{"merchantOrderNo"};

    @Autowired
    private TradeService tradeService;

    @RequestMapping(method = RequestMethod.GET)
    public AjaxResponse doGet(HttpServletRequest request) {
        try {
            Trade trade = tradeService.findByMerchantId(getMerchantId(request), getParams(request).getString("merchantOrderNo"));
            JSONObject tradeDetail = new JSONObject();
            tradeDetail.put("merchantOrderNo", trade.getMerchantOrderNo());
            tradeDetail.put("payOrderNo", trade.getPayOrderNo());
            tradeDetail.put("totalAmount", trade.getTotalAmount());
            //1 支付成功 2 手动确认支付
            tradeDetail.put("tradeState", trade.getTradeState());
            return AjaxResponse.success("tradeDetail", tradeDetail);
        } catch (BizException e) {
            logger.warn(e.getMessage());
            return AjaxResponse.failure(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            return AjaxResponse.failure();
        }
    }

    @Override
    public String[] getRequiredParams() {
        return REQUIRED_PARAMS;
    }
}
