package co.b4pay.api.controller.pay;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.common.web.BaseController;
import co.b4pay.api.model.Router;
import co.b4pay.api.model.base.AjaxResponse;
import co.b4pay.api.service.AliH5PayService;
import co.b4pay.api.service.RouterService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author YK
 * @version $Id v 0.1 2018年06月04日 22:32 Exp $
 */
@RestController
public class AliH5PayController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(AliH5PayController.class);

    private static final String ROUTER_KEY = "aliH5Pay";

    private static final String[] REQUIRED_PARAMS = new String[]{"tradeNo", "subject", "totalAmount", "notifyUrl"};
    private static final String[] OPTIONAL_PARAMS = new String[]{"channelId"};

    @Autowired
    private RouterService routerService;

    /*@Resource是按名字/type来自动注入的，当找不到相应的名称的bean时，会重新按照type来查找过。*/
    @Autowired(required = false)/*当aliH5PayService是null时也不会报错，required设置允许值为null*/
    private AliH5PayService aliH5PayService;

    @RequestMapping(value = "/pay/aliH5Pay.do", method = RequestMethod.POST)
    public Object aliH5Pay(HttpServletRequest request) {
        try {
            Router router = routerService.findById(ROUTER_KEY);
            return aliH5PayService.executeReturn(getMerchantId(request), router, getParams(request), request);
        } catch (BizException e) {
            logger.warn(e.getMessage());
            return AjaxResponse.failure(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResponse.failure();
        }

    }

    @RequestMapping(value = "/pay/aliH5PayExecute.do", method = RequestMethod.POST)
    public AjaxResponse aliH5PayExecute(HttpServletRequest request) {
        try {
            Router router = routerService.findById(ROUTER_KEY);
            JSONObject jsonObject = aliH5PayService.execute(getMerchantId(request), router, getParams(request), request);
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
