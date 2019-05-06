package co.b4pay.api.controller.pay;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.web.BaseController;
import co.b4pay.api.model.Router;
import co.b4pay.api.model.base.AjaxResponse;
import co.b4pay.api.service.MallPayService;
import co.b4pay.api.service.MallPayTestService;
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
 * mall支付
 *
 * @author YK
 * @version $Id v 0.1 2018年06月04日 21:32 Exp $
 */
@RestController
public class MallSPayController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(MallSPayController.class);

    private static final String ROUTER_KEY = "mallPay";

    private static final String[] REQUIRED_PARAMS = new String[]{"tradeNo","totalAmount","notifyUrl","money","time"};
    private static final String[] OPTIONAL_PARAMS = new String[]{"channelId"};

    @Autowired
    private RouterService routerService;

    @Autowired
    private MallPayService mallPayService;

    @RequestMapping(value = "/pay/mallPay.do", method = RequestMethod.POST)
    public Object aliSPay(HttpServletRequest request) {
        try {
            Router router = routerService.findById(ROUTER_KEY);
            return mallPayService.executeReturn(getMerchantId(request), router, getParams(request), request);
        } catch (BizException e) {

            logger.warn(e.getMessage());
            return AjaxResponse.failure(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResponse.failure();
        }

    }

    @RequestMapping(value = "/pay/mallPayExecute.do", method = RequestMethod.POST)
    public AjaxResponse aliSPayExecute(HttpServletRequest request) {
        try {
            Router router = routerService.findById(ROUTER_KEY);
            JSONObject jsonObject = mallPayService.execute(getMerchantId(request), router, getParams(request), request);
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
