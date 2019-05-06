/**
 * Hola.YK Inc.
 * Copyright (c) 2012-2013 All Rights Reserved.
 */
package co.b4pay.api.common.enums;

/**
 * 服务管理结果码
 *
 * @author YK
 * @version $Id: ResultCode.java, v 0.1 2013年9月5日 上午12:25:42 YK Exp $
 */
public interface ResultCode {

    /**
     * 获取枚举编码
     *
     * @return
     */
    String getCode();

    /**
     * 获取枚举描述
     *
     * @return
     */
    String getText();
}