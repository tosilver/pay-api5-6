package co.b4pay.api.controller.TLKJ;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.web.BaseController;
import co.b4pay.api.model.Router;
import co.b4pay.api.model.base.AjaxResponse;
import co.b4pay.api.service.RouterService;
import co.b4pay.api.service.TLKJ.PayAgreeConfirmService;
import co.b4pay.api.service.TLKJ.PayApplyService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 通联支付申请接口
 *
 * @author YK
 * @version $Id v 0.1 2018年06月04日 21:32 Exp $
 */
@RestController
public class PayAgreeConfirmController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(PayAgreeConfirmController.class);

    private static final String ROUTER_KEY = "TLKJPay";


    private static final String[] REQUIRED_PARAMS = new String[]{"orderid","agreeid"};
    private static final String[] OPTIONAL_PARAMS = new String[]{"smscode","thpinfo"};

    @Autowired
    private RouterService routerService;

    @Autowired
    private PayAgreeConfirmService payAgreeConfirmService;

    @RequestMapping(value = "/pay/payagreeconfirm.do", method = RequestMethod.POST)
    public Object agreeapply(HttpServletRequest request) {
        try {
            Router router =routerService.findById(ROUTER_KEY);
            return payAgreeConfirmService.executeReturn(getMerchantId(request), router, getParams(request), request);
        } catch (BizException e) {
            logger.warn(e.getMessage());
            return AjaxResponse.failure(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResponse.failure();
        }

    }

    @RequestMapping(value = "/pay/payagreeconfirmExecute.do", method = RequestMethod.POST)
    public AjaxResponse agreeapplyExecute(HttpServletRequest request) {
        try {
            Router router =routerService.findById(ROUTER_KEY);
            JSONObject jsonObject = payAgreeConfirmService.execute(getMerchantId(request), router, getParams(request), request);
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
