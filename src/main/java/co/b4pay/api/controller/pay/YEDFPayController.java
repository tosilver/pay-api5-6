package co.b4pay.api.controller.pay;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.web.BaseController;
import co.b4pay.api.model.Router;
import co.b4pay.api.model.base.AjaxResponse;
import co.b4pay.api.service.KJPayService;
import co.b4pay.api.service.RouterService;
import co.b4pay.api.service.YEDFPayService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 余额代付
 *
 * @author zgp
 * @version
 */
@RestController
public class YEDFPayController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(YEDFPayController.class);

    private static final String ROUTER_KEY = "yedfPay";

    private static final String[] REQUIRED_PARAMS =
            new String[]{"tradeNo","totalAmount","money", "agre_type","pay_type","is_compay","order_datetime","customer_name","customer_cert_type","customer_cert_id",
                    "customer_phone","bank_no","bank_short_name", "bank_name","bank_card_no"};
    private static final String[] OPTIONAL_PARAMS = new String[]{"channelId","remark"};

    @Autowired
    private RouterService routerService;

    @Autowired
    private YEDFPayService yedfPayService;

    @RequestMapping(value = "/pay/yedfPay.do", method = RequestMethod.POST)
    public Object aliSPay(HttpServletRequest request) {
        try {
            Router router = routerService.findById(ROUTER_KEY);
            return yedfPayService.executeReturn(getMerchantId(request), router, getParams(request), request);
        } catch (BizException e) {
            logger.warn(e.getMessage());
            return AjaxResponse.failure(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResponse.failure();
        }

    }

    @RequestMapping(value = "/pay/yedfPayExecute.do", method = RequestMethod.POST)
    public AjaxResponse aliSPayExecute(HttpServletRequest request) {
        try {
            Router router = routerService.findById(ROUTER_KEY);
            JSONObject jsonObject = yedfPayService.execute(getMerchantId(request), router, getParams(request), request);
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
