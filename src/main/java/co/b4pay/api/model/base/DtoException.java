package co.b4pay.api.model.base;

/**
 * <b>function:</b>定义数据传输层异常类
 *
 * @author YK
 */
public class DtoException extends BaseException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /*
     * Data Transfer Object（数据传输对象）
     */
    public DtoException(String msg) {
        super("数据传输对象: " + msg);
    }
}
