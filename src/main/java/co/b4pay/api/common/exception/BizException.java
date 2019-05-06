package co.b4pay.api.common.exception;

import co.b4pay.api.common.enums.ResultCode;
import co.b4pay.api.common.enums.ResultCode;

/**
 * 业务层异常基类
 *
 * @author YK
 * @version $Id: BizException.java, v 0.1 2013-9-4 下午1:21:37 YK Exp $
 */
public class BizException extends BaseException {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -7764503052808225410L;

    /**
     * 错误码
     */
    private ResultCode resultCode;

    public BizException(String message) {
        super(message);
    }

    public BizException(String message, Object... arguments) {
        super(String.format(message, arguments));
    }

    /**
     * 带错误码的构造函数
     *
     * @param resultCode 错误码
     */
    public BizException(ResultCode resultCode) {
        super(resultCode.getText());
        this.resultCode = resultCode;
    }

    /**
     * 带错误码和错误信息的构造函数
     *
     * @param resultCode 错误码
     */
    public BizException(ResultCode resultCode, String errDesc) {
        super(resultCode.getText() + "，" + errDesc);
        this.resultCode = resultCode;
    }

    /**
     * @param e
     */
    public BizException(Exception e) {
        // super(Constants.ERROR_MSG_BIZ, e);
        super(e);
    }

    public BizException(String msg, Exception e) {
        super(msg, e);
    }

    /**
     * Getter method for property <tt>resultCode</tt>.
     *
     * @return property value of resultCode
     */
    public ResultCode getResultCode() {
        return resultCode;
    }
}
