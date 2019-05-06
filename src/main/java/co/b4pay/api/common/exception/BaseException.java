package co.b4pay.api.common.exception;

/**
 * <b>function:</b>定义异常类基类
 *
 * @author YK
 */
public class BaseException extends RuntimeException {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3659869519957644146L;

    public BaseException(String msg) {
        super(msg);
    }

    /**
     * @param e
     */
    public BaseException(Exception e) {
        super(e);
    }

    /**
     * @param msg
     * @param e
     */
    public BaseException(String msg, Exception e) {
        super(msg, e);
    }
}
