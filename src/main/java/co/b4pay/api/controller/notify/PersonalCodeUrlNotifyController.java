package co.b4pay.api.controller.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static co.b4pay.api.service.CodePayService.FIXED_CODE_URL_MAP;
import static co.b4pay.api.service.CodePayService.UNFIXED_CODE_URL_MAP;

/**
 * 收款码图片地址管理相关Url
 */
@RestController
@RequestMapping({"/notify/updatePersonalCodeUrl.do"})
public class PersonalCodeUrlNotifyController {
    private static final Logger logger = LoggerFactory.getLogger(PersonalCodeUrlNotifyController.class);

    @RequestMapping(method = {RequestMethod.GET})
    public void doPost(HttpServletRequest request) {
        logger.warn("用户上传二维码图片，重新更新URL");
        FIXED_CODE_URL_MAP.clear();
        UNFIXED_CODE_URL_MAP.clear();
    }
}
