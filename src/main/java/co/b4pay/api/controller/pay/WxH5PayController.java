package co.b4pay.api.controller.pay;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.web.BaseController;
import co.b4pay.api.model.*;
import co.b4pay.api.model.base.AjaxResponse;
import co.b4pay.api.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author YK
 * @version $Id v 0.1 2018年06月04日 22:32 Exp $
 */
@RestController
@RequestMapping("/pay/wxH5Pay.do")
public class WxH5PayController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(WxH5PayController.class);
    private static final String ROUTER_KEY = "wxH5Pay";

    private static final String[] REQUIRED_PARAMS = new String[]{"trade_no", "total_amount", "body", "notify_url", "spbill_create_ip", "scene_info"};
    private static final String[] OPTIONAL_PARAMS = new String[]{"attach"};

    @Autowired
    private RouterService routerService;

    @Autowired
    private WeiXinPayService weiXinPayService;

    @RequestMapping(method = RequestMethod.POST)
    public AjaxResponse doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            Router router = routerService.findById(ROUTER_KEY);
            if (router == null || router.getStatus() == -1) {
                throw new RuntimeException(String.format("[%s]路由异常", "wxH5Pay"));
            }
            return weiXinPayService.execute(getMerchantId(request), router, getParams(request), request, response);
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
