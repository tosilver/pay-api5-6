package co.b4pay.api.controller.query;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.web.BaseController;
import co.b4pay.api.model.Router;
import co.b4pay.api.model.base.AjaxResponse;
import co.b4pay.api.service.RouterService;
import co.b4pay.api.service.YEDFPayService;
import co.b4pay.api.service.YEDFQueryPayService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 余额代付/快捷支付查询
 *
 * @author zgp
 * @version
 */
@RestController
public class YEDFqueryPayController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(YEDFqueryPayController.class);

    private static final String ROUTER_KEY = "yedfquery";

    private static final String[] REQUIRED_PARAMS =
            new String[]{"agre_type","merch_order_no","query_id","order_datetime","tradeNo","totalAmount"};
    private static final String[] OPTIONAL_PARAMS = new String[]{"channelId","platform_order_no"};

    @Autowired
    private RouterService routerService;

    @Autowired
    private YEDFQueryPayService yedfQueryPayService;

    @RequestMapping(value = "/pay/kyqueryPay.do", method = RequestMethod.POST)
    public Object aliSPay(HttpServletRequest request) {
        try {
            Router router = routerService.findById(ROUTER_KEY);
            return yedfQueryPayService.executeReturn(getMerchantId(request), router, getParams(request), request);
        } catch (BizException e) {
            logger.warn(e.getMessage());
            return AjaxResponse.failure(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResponse.failure();
        }

    }

    @RequestMapping(value = "/pay/kyqueryPayExecute.do", method = RequestMethod.POST)
    public AjaxResponse aliSPayExecute(HttpServletRequest request) {
        try {
            Router router = routerService.findById(ROUTER_KEY);
            JSONObject jsonObject = yedfQueryPayService.execute(getMerchantId(request), router, getParams(request), request);
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
