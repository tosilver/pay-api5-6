package co.b4pay.api.controller.pay;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.web.BaseController;
import co.b4pay.api.model.Router;
import co.b4pay.api.model.base.AjaxResponse;
import co.b4pay.api.service.QRPayService;
import co.b4pay.api.service.RouterService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * qr支付
 *
 * @author YK
 * @version $Id v 0.1 2018年06月04日 21:32 Exp $
 */
@RestController
public class QRReplacementOrderController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(QRReplacementOrderController.class);

    private static final String ROUTER_KEY_ALI = "qrALiPay";
    private static final String ROUTER_KEY_WX = "qrWXPay";
    private static final String ROUTER_KEY_JH = "qrJHPay";


    private static final String[] REQUIRED_PARAMS = new String[]{"tradeNo", "totalAmount", "notifyUrl", "time", "type"};
    private static final String[] OPTIONAL_PARAMS = new String[]{"channelId"};

    @Autowired
    private RouterService routerService;

    @Autowired
    private QRPayService qrPayService;

    @RequestMapping(value = "/pay/qrReplacement.do", method = RequestMethod.POST)
    public AjaxResponse aliSPay(HttpServletRequest request) {
        try {
            logger.info("进行补单操作");
            String type = request.getParameter("type");
            Router router = null;
            switch (type) {
                case "0":
                    router = routerService.findById(ROUTER_KEY_ALI);
                    logger.info("----------->请求的是支付宝");
                    break;
                case "1":
                    router = routerService.findById(ROUTER_KEY_WX);
                    logger.info("----------->请求的是微信");
                    break;
                case "2":
                    router = routerService.findById(ROUTER_KEY_JH);
                    logger.info("----------->请求的是聚合码");
                    break;
                default:
                    break;
            }

            JSONObject jsonObject = qrPayService.replacement(getMerchantId(request), router, getParams(request), request);
            return AjaxResponse.success(jsonObject);
        } catch (BizException e) {
            logger.warn(e.getMessage());
            return AjaxResponse.failure(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResponse.failure();
        }

    }


    @Override
    protected String[] getRequiredParams() {
        return REQUIRED_PARAMS;
    }

    @Override
    protected String[] getOptionalParams() {
        return OPTIONAL_PARAMS;
    }
}
