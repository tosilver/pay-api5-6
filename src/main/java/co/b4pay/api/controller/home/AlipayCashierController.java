package co.b4pay.api.controller.home;

import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.model.Trade;
import co.b4pay.api.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 支付宝收银页面
 *
 * @author YK
 * @version $Id v 0.1 2017年08月08日 15:51 Exp $
 */
@Controller
@RequestMapping("/alipay/cashier.htm")
public class AlipayCashierController {
    private static final Logger logger = LoggerFactory.getLogger(AlipayCashierController.class);

    @Autowired
    private TradeService tradeService;

    @RequestMapping(method = RequestMethod.GET)
    public void doGet(HttpServletResponse response, String tradeId) {
        try {
            Trade trade = tradeService.findById(tradeId);
            String form = trade.getResponse();
            WebUtil.writeHtml(response, form); //直接将完整的表单html输出到页面
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
