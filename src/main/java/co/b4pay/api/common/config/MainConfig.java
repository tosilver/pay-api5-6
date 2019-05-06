package co.b4pay.api.common.config;

import co.b4pay.api.common.kit.PropKit;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局配置
 *
 * @author YK
 */
public final class MainConfig {
    /**
     * 保存全局属性值
     */
    private static final Map<String, String> map = new ConcurrentHashMap<>();

    /**
     * 属性文件加载对象
     */
    private static final PropertiesLoader loader = new PropertiesLoader("config.properties");

    /**
     * 获取配置
     *
     * @see \${fns:getConfig('adminPath')}
     */
    public static String getConfig(String key) {
        String value = map.get(key);
        if (value == null) {
            value = loader.getProperty(key);
            map.put(key, value != null ? value : StringUtils.EMPTY);
        }
        return value;
    }

    public static final String CURRENT_USER = "user";
    public static final String SESSION_FORCE_LOGOUT_KEY = "session.force.logout";
    public static final String MESSAGE = "message";
    public static final String PARAM_DIGEST = "digest";
    public static final String PARAM_USERNAME = "username";
    /**
     * 开发模式
     */
    public static final Boolean isDevMode = PropKit.getBoolean("devMode");

    public static final String jobIntervalTime = PropKit.get("JOB_INTERVAL_TIME");

    public static final String jobPartitionTime = PropKit.get("JOB_PARTITION_TIME");

    public static final String API_NOTIFY_URL = PropKit.get("API_NOTIFY_URL");

    public static final String GR_ORDER_URL = PropKit.get("GR_ORDER_URL");

    public static final String TRANSIN = PropKit.get("TRANSIN");

    public static final String EXPIRY_TIME = PropKit.get("EXPIRY_TIME");

    public static final String CHANCEL_ROTATION_TIME = PropKit.get("CHANCEL_ROTATION_TIME");

    public static final String CHANCEL_MIN_RATE = PropKit.get("CHANCEL_MIN_RATE");

}