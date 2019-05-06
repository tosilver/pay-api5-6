package co.b4pay.api.controller.pay;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.web.BaseController;
import co.b4pay.api.model.Router;
import co.b4pay.api.model.base.AjaxResponse;
import co.b4pay.api.service.GRPayService;
import co.b4pay.api.service.RouterService;
import co.b4pay.api.service.SHPayService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class GRPayController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(GRPayController.class);

    private static final String ROUTER_KEY = "grPay";

    private static final String[] REQUIRED_PARAMS = new String[]{"tradeNo", "subject", "totalAmount", "notifyUrl"};
    private static final String[] OPTIONAL_PARAMS = new String[]{"channelId"};

    @Autowired
    private RouterService routerService;

    @Autowired
    private GRPayService grPayService;

    @RequestMapping(value = "/pay/grPay.do", method = RequestMethod.POST)
    public Object aliH5Pay(HttpServletRequest request) {
        try {
            //查询路由
            Router router = routerService.findById(ROUTER_KEY);
            String totalAmount = request.getParameter("totalAmount");
            return grPayService.executeReturn(getMerchantId(request), router, getParams(request), request);
        } catch (BizException e) {
            logger.warn(e.getMessage());
            return AjaxResponse.failure(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResponse.failure();
        }

    }

    @RequestMapping(value = "/pay/grPayExecute.do", method = RequestMethod.POST)
    public AjaxResponse aliH5PayExecute(HttpServletRequest request) {
        try {
            Router router = routerService.findById(ROUTER_KEY);
            JSONObject jsonObject = grPayService.execute(getMerchantId(request), router, getParams(request), request);
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
