package co.b4pay.api.common.web;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.utils.StringUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseController {

    protected Long getMerchantId(HttpServletRequest request) {
        String merchantId = request.getParameter("merchantId");
        return isBlank(merchantId) ? null : Long.parseLong(merchantId);
    }

    protected boolean isBlank(String str) {
        return str == null || str.isEmpty();
    }

    protected JSONObject getParams(HttpServletRequest request) throws BizException {
        Map<String, String> requiredParams = new HashMap<>(); // 必需参数
        Map<String, String> optionalParams = new HashMap<>(); // 可选参数
        List<String> optionalParamNames = new ArrayList<>(); // 可选参数名
        for (String paramName : getRequiredParams()) { // 必需参数
            String paramValue = request.getParameter(paramName);
            if (StringUtil.isBlank(paramValue)) {
                throw new BizException(String.format("缺少参数[%s]", paramName));
            }
            requiredParams.put(paramName, paramValue);
        }
        for (String paramName : getOptionalParams()) { // 可选参数
            String paramValue = request.getParameter(paramName);
            optionalParamNames.add(paramName);
            if (StringUtil.isNotBlank(paramValue)) {
                optionalParams.put(paramName, paramValue);
            }
        }

        if (MapUtils.isEmpty(requiredParams) && MapUtils.isEmpty(optionalParams)) {
            throw new BizException(String.format("%s可选参数不能同时为空", optionalParamNames));
        }

        JSONObject params = new JSONObject(); // 业务参数
        if (MapUtils.isNotEmpty(requiredParams)) {
            params.putAll(requiredParams);
        }
        if (MapUtils.isNotEmpty(optionalParams)) {
            params.putAll(optionalParams);
        }
        return params;
    }

    protected String[] getRequiredParams() {
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    protected String[] getOptionalParams() {
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }
}
